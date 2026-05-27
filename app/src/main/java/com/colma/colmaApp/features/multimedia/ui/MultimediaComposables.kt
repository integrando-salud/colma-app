package com.colma.colmaApp.features.multimedia.ui

import android.net.Uri
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.viewinterop.AndroidView
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close

import com.colma.colmaApp.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*
import java.util.concurrent.TimeUnit

import com.colma.colmaApp.utils.videoPlayer.FullscreenVideoActivity
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed

import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog
import com.colma.colmaApp.utils.varios.CardThatOpensEditDialog
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import android.widget.Toast

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.colma.colmaApp.utils.varios.CardThatOpensDateTimeDialog
import java.text.SimpleDateFormat

import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.content.model.QuizQuestion // Existing for QuizContent
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord // New
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord // New
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType // New
import com.google.gson.Gson





import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
fun NavigationButtons(
    currentIndex: Int,
    maxIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    signatureColor: Color
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentIndex > 0) {
            Button(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.principal),
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Anterior", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = signatureColor,
                contentColor = Color.White
            ),
        ) {
            val isLast = currentIndex == maxIndex
            Text(text = if (isLast) "Finalizar" else "Siguiente", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InformationDisplayContent(
    displayContent: String,
    descriptionAudios: List<Int>? = null,
    navigationButtons: @Composable () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val paragraphs = remember(displayContent) {
        displayContent.split("\n\n").filter { it.isNotBlank() }
    }
    val pagerState = rememberPagerState(pageCount = { paragraphs.size })

    Column(modifier = Modifier
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (paragraphs.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                InformationCard(
                    displayContent = paragraphs[page],
                    audioResId = descriptionAudios?.getOrNull(page),
                    navigationButtons = navigationButtons
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalPagerIndicator(
                pagerState = pagerState,
                pageCount = paragraphs.size,
                activeColor = signatureColor,
                inactiveColor = Color.LightGray,
                modifier = Modifier
                    .padding(top = 8.dp)
            )
        } else {
            InformationCard(
                displayContent = "No content provided.",
                audioResId = descriptionAudios?.getOrNull(0),
                navigationButtons = navigationButtons
            )
        }
    }
}

@Composable
fun InformationCard(
    displayContent: String,
    audioResId: Int? = null,
    navigationButtons: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scrollState = rememberScrollState()

    // Audio playback state
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    // Initialize ExoPlayer if audio is available
    DisposableEffect(audioResId) {
        if (audioResId != null) {
            val player = ExoPlayer.Builder(context).build().apply {
                val uri = Uri.parse("android.resource://${context.packageName}/$audioResId")
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
            exoPlayer = player

            onDispose {
                try {
                    player.stop()
                } catch (_: Exception) {}
                player.release()
                exoPlayer = null
            }
        } else {
            onDispose { }
        }
    }

    // Initialize TTS only if no custom audio is available
    DisposableEffect(Unit) {
        if (audioResId == null) {
            isInitializing = true
            val speech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    mainHandler.post {
                        isTtsReady = true
                        isInitializing = false
                    }
                } else {
                    mainHandler.post { isInitializing = false }
                }
            }
            speech.language = Locale("es", "ES")
            speech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { isSpeaking = true }
                }
                override fun onDone(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }
                override fun onError(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }
            })
            tts = speech
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Auto-stop when audio finishes
    LaunchedEffect(exoPlayer, isPlayingAudio) {
        if (exoPlayer != null && isPlayingAudio) {
            while (this.isActive && isPlayingAudio) {
                // Check if audio finished
                if (exoPlayer!!.currentPosition >= exoPlayer!!.duration) {
                    isPlayingAudio = false
                    break
                }
                delay(250L)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scrollbar(scrollState)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Información importante",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // Show play/pause button for both custom audio and TTS
                if (isInitializing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFE84589),
                        strokeWidth = 2.dp
                    )
                } else if (audioResId != null && exoPlayer != null) {
                    // Custom audio available - show play/pause button
                    IconButton(onClick = {
                        exoPlayer?.let { player ->
                            if (isPlayingAudio) {
                                player.stop()
                                player.seekTo(0)
                                isPlayingAudio = false
                            } else {
                                player.seekTo(0)
                                player.playWhenReady = true
                                isPlayingAudio = true
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.tts_icon),
                            contentDescription = if (isPlayingAudio) "Detener audio" else "Reproducir audio",
                            tint = Color(0xFFE84589)
                        )
                    }
                } else if (isTtsReady) {
                    // No custom audio - show TTS button
                    IconButton(onClick = {
                        if (isSpeaking) {
                            tts?.stop()
                            isSpeaking = false
                        } else {
                            tts?.speak(displayContent, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.tts_icon),
                            contentDescription = if (isSpeaking) "Detener lectura" else "Escuchar contenido",
                            tint = Color(0xFFE84589)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Icon(
                painter = painterResource(R.drawable.outline_unknown_document_24),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFE84589)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                displayContent,
                fontSize = 16.sp,
                textAlign = TextAlign.Left
            )

            Spacer(Modifier.height(16.dp))
            navigationButtons()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Gray,
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = 8.dp,
    spacing: Dp = 8.dp,
    indicatorShape: Shape = CircleShape
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(pageCount) { index ->
            val color = if (pagerState.currentPage == index) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .padding(horizontal = spacing / 2)
                    .size(indicatorWidth, indicatorHeight)
                    .background(color = color, shape = indicatorShape)
            )
        }
    }
}

@Composable
fun AudioContent(
    @RawRes audioResId: Int,
    navigationButtons: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember(audioResId) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$audioResId")
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            try {
                exoPlayer.stop()
            } catch (_: Exception) {}
            exoPlayer.release()
        }
    }

    var isPlaying by rememberSaveable(audioResId) { mutableStateOf(false) }
    var playbackPositionMs by remember(audioResId) { mutableStateOf(0L) }
    var durationMs by remember(audioResId) { mutableStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        var attempts = 0
        while (durationMs <= 0 && attempts < 20) {
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            attempts++
            delay(100)
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            while (isActive && isPlaying) {
                playbackPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                delay(250L)
            }
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    FinishPage(
        audioDurationMs = if (durationMs > 0L) durationMs else 120_000L, // fallback duration
        playbackPositionMs = playbackPositionMs,
        isPlaying = isPlaying,
        onPlayPauseToggle = {
            isPlaying = !isPlaying
        },
        onSeek = { newPos ->
            val pos = newPos.coerceIn(0L, (exoPlayer.duration.coerceAtLeast(0L)))
            exoPlayer.seekTo(pos)
            playbackPositionMs = pos
        },
        navigationButtons = navigationButtons
    )
}

@Composable
fun FinishPage(
    audioDurationMs: Long = 120_000L,
    playbackPositionMs: Long = 0L,
    isPlaying: Boolean = false,
    onPlayPauseToggle: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    navigationButtons: @Composable () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scrollbar(scrollState)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Audio Explicativo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.outline_headphones_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFE84589)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPlayPauseToggle) {
                    Icon(
                        painter = if (isPlaying) painterResource(R.drawable.baseline_pause_24) else painterResource(R.drawable.baseline_play_arrow_24),
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFFE84589)
                    )
                }

                val duration = (audioDurationMs.coerceAtLeast(1L)).toFloat()
                val position = (playbackPositionMs.coerceIn(0L, audioDurationMs)).toFloat()
                Slider(
                    value = if (duration > 0f) (position / duration) else 0f,
                    onValueChange = { frac ->
                        val newPos = (frac * duration).toLong()
                        onSeek(newPos)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(playbackPositionMs), fontSize = 12.sp, color = Color.Gray)
                Text(formatTime(audioDurationMs), fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Pulsa el botón de play para escuchar el audio explicativo.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
            navigationButtons()
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VideoContent(@RawRes contentAssetId: Int, navigationButtons: @Composable () -> Unit = {}) {
    val scrollState = rememberScrollState()
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scrollbar(scrollState)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title + icon row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Video Explicativo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.outline_videocam_24),
                    contentDescription = "Ver video",
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFE84589)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Video player - replace missing_photo thumbnail with actual player
            VideoPlayerWithFullscreen(
                resId = contentAssetId, // Use the dynamically provided contentAssetId
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.height(16.dp))
            navigationButtons()
        }
    }
}

    @Composable
    fun VideoPlayerWithFullscreen(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    playerWidth: Dp = 320.dp,
    playerHeight: Dp = 200.dp
) {
    val context = LocalContext.current
    val uriString = "android.resource://${context.packageName}/$resId"

    // Track the last known playback position
    var lastPosition by rememberSaveable(resId) { mutableStateOf(0L) }

    // Local player just to show the frame/preview at current position
    val exoPlayer = remember(resId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            prepare()
            seekTo(lastPosition)
            playWhenReady = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pos = result.data?.getLongExtra(FullscreenVideoActivity.RESULT_POSITION, lastPosition) ?: lastPosition
            lastPosition = pos
            exoPlayer.seekTo(pos)
        }
    }

    DisposableEffect(key1 = resId) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black, shape = RoundedCornerShape(8.dp))
            .clickable {
                val intent = Intent(context, FullscreenVideoActivity::class.java).apply {
                    putExtra(FullscreenVideoActivity.EXTRA_URI, uriString)
                    putExtra(FullscreenVideoActivity.EXTRA_POSITION, lastPosition)
                }
                launcher.launch(intent)
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Disable standard controllers
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Centered play arrow overlay
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                contentDescription = "Reproducir en pantalla completa",
                tint = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    quizQuestions: List<QuizQuestion>,
    quizTitle: String,
    sessionId: Long,
    subSessionId: Long,
    treatmentTypes: List<TreatmentType>,
    multimediaUniqueKey: String,
    onQuizSubmit: (quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>) -> Unit,
    onFinish: () -> Unit,
    navigationButtons: @Composable () -> Unit = {}
) {
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember { mutableStateOf(false) } // Keep this for UI feedback
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val allQuestionsAnswered = userAnswers.size == quizQuestions.size

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        quizQuestions.forEachIndexed { index, question ->
            QuestionCard(
                questionIndex = index,
                question = question,
                selectedAnswerIndex = userAnswers[index],
                isSubmitted = quizSubmitted,
                onAnswerSelected = { answerIndex ->
                    if (!quizSubmitted) {
                        userAnswers[index] = answerIndex
                    }
                }
            )
        }

        if (!quizSubmitted) {
            Button(
                onClick = {
                    showConfirmationDialog = true
                },
                enabled = allQuestionsAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.contraste))
            ) {
                Text("Guardar", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        navigationButtons()
    }

    if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                containerColor = Color.White,
                title = { Text("Confirmar respuestas") },
                text = { Text("¿Estás seguro de que quieres confirmar tus respuestas? No podrás cambiarlas después.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmationDialog = false
                            quizSubmitted = true // Mark as submitted for UI feedback
                            val totalQuestions = quizQuestions.size
                            var correctAnswers = 0
                            val questionResponses = mutableListOf<QuestionResponseRecord>()
                            
                            quizQuestions.forEachIndexed { index, question ->
                                val userAnswerIndex = userAnswers[index]
                                val isCorrect = userAnswerIndex == question.correctAnswerIndex
                                if (isCorrect) {
                                    correctAnswers++
                                }
                                questionResponses.add(
                                    QuestionResponseRecord(
                                        id_quiz_response = 0, // Will be filled by DB
                                        question_index = index,
                                        question_text = question.questionText,
                                        user_answer_index = userAnswerIndex,
                                        user_answer_text = userAnswerIndex?.let { question.options.getOrNull(it) },
                                        correct_answer_index = question.correctAnswerIndex,
                                        correct_answer_text = question.options.getOrNull(question.correctAnswerIndex),
                                        is_correct = if (isCorrect) 1 else 0,
                                        posibles_respuestas = Gson().toJson(question.options)
                                    )
                                )
                            }
                            val percentageCorrect = if (totalQuestions > 0) (correctAnswers.toDouble() / totalQuestions.toDouble()) * 100 else 0.0

                            val quizTreatmentId = treatmentTypes.firstOrNull()?.name?.lowercase() ?: "unknown"
                            val quizResponseRecord = QuizResponseRecord(
                                id_quiz_response = 0, // Will be filled by DB
                                id_usuario = SessionManager.getUserId() ?: 1, // Default user ID
                                quiz_type = quizTreatmentId, // Use treatment type (hta/diabetes)
                                week_id = sessionId.toInt(),
                                session_id = subSessionId.toInt(),
                                quiz_title = quizTitle,
                                fecha_registro = null, // Will be set by DB
                                fecha_sincronizacion = null,
                                total_questions = totalQuestions,
                                correct_answers = correctAnswers,
                                percentage_correct = percentageCorrect,
                                multimedia_unique_key = multimediaUniqueKey
                                // treatment_type removed - quiz_type already indicates hta/diabetes
                            )
                            onQuizSubmit(quizResponseRecord, questionResponses)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false },
                        colors = ButtonDefaults.textButtonColors(containerColor = colorResource(R.color.rojo_boton), contentColor = Color.White)
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
}

@Composable
fun QuestionCard(
    questionIndex: Int,
    question: QuizQuestion,
    selectedAnswerIndex: Int?,
    isSubmitted: Boolean,
    onAnswerSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pregunta ${questionIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isSubmitted) {
                    val isCorrect = selectedAnswerIndex == question.correctAnswerIndex
                    val text = if (isCorrect) "Correcto" else "Incorrecto"
                    val bgColor = if (isCorrect) Color(0xFF008000) else Color.Red
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(bgColor, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = question.questionText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitted) {
                val userAnswerText = selectedAnswerIndex?.let { question.options.getOrNull(it) } ?: "No respondida"
                val correctAnswerText = question.options[question.correctAnswerIndex]
                Text("Tu respuesta: $userAnswerText")
                if (selectedAnswerIndex != question.correctAnswerIndex) {
                    Text("Respuesta correcta: $correctAnswerText", color = Color(0xFF008000))
                }
            } else {
                question.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswerIndex == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnswerSelected(index) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onAnswerSelected(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorResource(R.color.principal)
                            )
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasContent(
    metasId: String, 
    currentWeekId: Int, 
    currentSessionId: Int, 
    onFinish: () -> Unit,
    navigationButtons: @Composable () -> Unit = {}
) {
    val treatmentType = if (metasId != null) {
        try {
            TreatmentType.valueOf(metasId.uppercase(Locale.ROOT))
        } catch (e: IllegalArgumentException) {
            TreatmentType.HTA // Default to HTA if invalid string
        }
    } else {
        TreatmentType.HTA // Default to HTA if no extra is passed
    }

    val context = LocalContext.current
    val dbHelper = remember { DbHelper(context) }
    val scope = rememberCoroutineScope()

    val dbDateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }

    val actividad = rememberSaveable { mutableStateOf<String?>(null) }
    val conQuien = rememberSaveable { mutableStateOf<String?>(null) }
    val donde = rememberSaveable { mutableStateOf<String?>(null) }

    val today = Calendar.getInstance()
    val todayString = dbDateTimeFormat.format(today.time)
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
    val fecha = rememberSaveable { mutableStateOf(dbDateTimeFormat.format(tomorrow.time)) }
    val fechaEdicion = rememberSaveable { mutableStateOf(todayString) }

    var showSummaryDialog by rememberSaveable { mutableStateOf(false) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var summaryMessage by rememberSaveable { mutableStateOf("") }
    var shouldExecuteFinalAction by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // State for blocking dialog when user has a goal for a future week
    var isBlockedByFutureGoal by rememberSaveable { mutableStateOf(false) }
    var blockedWeekNumber by rememberSaveable { mutableIntStateOf(-1) }
    
    // State for blocking dialog when user has completed a goal for this week
    var isBlockedByCompletedGoalThisWeek by rememberSaveable { mutableStateOf(false) }
    
    // Check on load if user should be blocked
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val userId = SessionManager.getUserId() ?: 1
            val highestWeek = dbHelper.getHighestWeekWithGoal(userId, treatmentType.name.lowercase())
            val hasCompletedThisWeek = dbHelper.hasCompletedGoalForWeek(userId, currentWeekId, treatmentType.name.lowercase())
            if (highestWeek != -1 && currentWeekId < highestWeek) {
                withContext(Dispatchers.Main) {
                    isBlockedByFutureGoal = true
                    blockedWeekNumber = highestWeek
                }
            } else if (hasCompletedThisWeek) {
                withContext(Dispatchers.Main) {
                    isBlockedByCompletedGoalThisWeek = true
                }
            }
        }
    }

    // Show blocking dialog if user has a future week goal
    if (isBlockedByFutureGoal) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("No puedes crear metas para esta semana") },
            text = {
                Column {
                    Text("Ya creaste una meta para la **Semana $blockedWeekNumber**.")
                    Spacer(Modifier.height(8.dp))
                    Text("Por favor crea metas posteriores a esta semana.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return // Don't render the rest of the UI
    }

    // Show blocking dialog if user has completed a goal for this week
    if (isBlockedByCompletedGoalThisWeek) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("Meta ya completada") },
            text = {
                Column {
                    Text("Ya completaste una meta para esta semana.")
                    Spacer(Modifier.height(8.dp))
                    Text("Por favor avanza a la siguiente semana para crear una nueva meta.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return // Don't render the rest of the UI
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (actividades, conQuienOptions) = getMetasOptions(treatmentType)

            CardThatOpensSelectionDialog(
                title = "¿Qué actividad deseas hacer?",
                items = actividades,
                label = { it },
                onItemSelected = { chosen ->
                    actividad.value = chosen
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensSelectionDialog(
                title = "¿Con quién la harás? (Opcional)\n",
                items = conQuienOptions,
                label = { it },
                onItemSelected = { chosen ->
                    conQuien.value = chosen
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensEditDialog(
                title = "¿Dónde la harás? (Opcional)",
                placeholder = "Escribe el lugar...",
                onSave = { newDesc ->
                    donde.value = newDesc
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensDateTimeDialog(
                title = "Fecha límite",
                initialMillis = tomorrow.timeInMillis,
                showTime = false,
                onDateTimeSelected = { selectedCal ->
                    selectedCal.set(Calendar.SECOND, 0)
                    fecha.value = dbDateTimeFormat.format(selectedCal.time)
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val missing = buildList {
                        if (actividad.value.isNullOrBlank()) add("Actividad")
                        if (fecha.value.isNullOrBlank()) add("Fecha Objetivo")
                        if (fechaEdicion.value.isNullOrBlank()) add("Fecha de Creación")
                    }
                    shouldExecuteFinalAction = missing.isEmpty()
                    if (missing.isNotEmpty()) {
                        summaryMessage = "Faltan completar los siguientes campos:\n\n• " + missing.joinToString("\n• ")
                    } else {
                        val fechaObjetivoForDisplay = dbDateTimeFormat.parse(fecha.value)?.let { displayDateFormat.format(it) } ?: fecha.value
                        val fechaCreacionForDisplay = dbDateTimeFormat.parse(fechaEdicion.value)?.let { displayDateFormat.format(it) } ?: fechaEdicion.value
                        summaryMessage = """
                            • Actividad: ${actividad.value}
                            • ¿Con quién?: ${conQuien.value ?: "No especificado"}
                            • ¿Dónde?: ${donde.value ?: "No especificado"}
                            • Fecha límite: $fechaObjetivoForDisplay
                        """.trimIndent()
                    }
                    showSummaryDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.contraste),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Guardar", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            navigationButtons()
        }

        if (showSummaryDialog) {
                CustomAlertDialog(
                    show = showSummaryDialog,
                    onDismissRequest = { showSummaryDialog = false },
                    title = if (shouldExecuteFinalAction) "Confirmar meta" else "Faltan datos",
                    description = summaryMessage,
                    yesLabel = if (shouldExecuteFinalAction) "Guardar" else "Entendido",
                    noLabel = if (shouldExecuteFinalAction) "Cancelar" else "",
                    onYes = {
                        if (shouldExecuteFinalAction) {
                            val userId = SessionManager.getUserId() ?: 1
                            scope.launch {
                                val newMeta = RegistrosSaludModelos.MetaRecord(
                                    id_meta = 0,
                                    id_usuario = userId,
                                    semana = currentWeekId,
                                    sesion = currentSessionId,
                                    tipo_actividad = treatmentType.name.lowercase(),
                                    actividad = actividad.value,
                                    lugar = donde.value,
                                    acompañante = conQuien.value,
                                    fecha_regisitro = null,
                                    fecha_edicion = fechaEdicion.value,
                                    fecha_sincronizacion = null,
                                    meta_cumplida = 0,
                                    fecha_objetivo = fecha.value
                                )
                                val result = withContext(Dispatchers.IO) {
                                    // Check if user has a goal for a higher week (treatment-specific)
                                    val highestWeekWithGoal = dbHelper.getHighestWeekWithGoal(
                                        userId,
                                        treatmentType.name.lowercase()
                                    )
                                    
                                    if (highestWeekWithGoal != -1 && currentWeekId < highestWeekWithGoal) {
                                        -3L // Specific code for earlier week blocked
                                    } else if (dbHelper.getPendingFutureMetasCount(userId) > 0) {
                                        -2L // Specific code for active goal exists
                                    } else {
                                        dbHelper.insertMeta(newMeta)
                                    }
                                }
                                if (result > 0L) {
                                    withContext(Dispatchers.Main) {
                                        showSuccessDialog = true
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = if (result == -3L) {
                                            val highestWeekWithGoal = dbHelper.getHighestWeekWithGoal(
                                                userId,
                                                treatmentType.name.lowercase()
                                            )
                                            "Ya tienes una meta creada para la Semana $highestWeekWithGoal. " +
                                                "No puedes crear metas para semanas anteriores. " +
                                                "Completa tu meta actual antes de continuar."
                                        } else if (result == -2L) {
                                            "Ya tienes una meta activa. Debes completarla antes de crear una nueva."
                                        } else {
                                            "Error al guardar la meta"
                                        }
                                        showErrorDialog = true
                                    }
                                }
                            }
                        }
                        showSummaryDialog = false
                    },
                    onNo = { showSummaryDialog = false }
                )
            }

            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showSuccessDialog = false
                        onFinish()
                    },
                    title = { Text("La meta se ha guardado exitosamente") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                onFinish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Guardar")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(
                            onClick = { showErrorDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.rojo_boton), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Guardar")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
    }
}

fun getMetasOptions(treatmentType: TreatmentType): Pair<List<String>, List<String>> {
    val conQuienOptions = listOf("Solo/a", "Familiar", "Pareja", "Amigo/a", "Vecino/a", "Mascota")

    return when (treatmentType) {
        TreatmentType.DIABETES -> Pair(
            listOf(
                "Ordenar la cocina o una habitación.",
                "Ir de compras o ir al mercado.",
                "Hacer un trámite pendiente (ej, hacer una llamada, realizar una consulta, sacar un turno)",
                "Revisar mis documentos personales o médicos.",
                "Limpiar un espacio de la casa que he dejado pendiente.",
                "Reparar o arreglar algo roto o que no funciona.",
                "Lavar o planchar ropa acumulada.",
                "Ir al banco, correo u oficina municipal.",
                "Organizar la semana con una lista de pendientes."
            ),
            conQuienOptions
        )
        TreatmentType.HTA -> Pair(
            listOf(
                "Subir escaleras en lugar de usar el ascensor.",
                "Bajar una parada de colectivo antes para caminar un poco más",
                "Bailar una canción en casa.",
                "Caminar al mercado, centro de salud, casa de un familia",
                "Pasear en bicicleta.",
                "Jugar algún deporte recreativo (fútbol, voley,etc.)."
            ),
            conQuienOptions
        )
    }
}

fun Modifier.scrollbar(
    state: androidx.compose.foundation.ScrollState,
    width: androidx.compose.ui.unit.Dp = 4.dp,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
): Modifier = this.drawWithContent {
    drawContent()
    if (state.maxValue > 0) {
        val viewPortHeight = size.height
        val thumbHeight = (viewPortHeight / (state.maxValue + viewPortHeight)) * viewPortHeight * 0.5f
        val scrollOffset = state.value.toFloat()
        val scrollPercent = scrollOffset / state.maxValue
        val thumbOffset = scrollPercent * (viewPortHeight - thumbHeight)

        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - width.toPx(), thumbOffset),
            size = androidx.compose.ui.geometry.Size(width.toPx(), thumbHeight)
        )
    }
}

fun Modifier.scrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    width: androidx.compose.ui.unit.Dp = 6.dp,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.8f),
): Modifier = this.drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isNotEmpty()) {
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsCount = visibleItemsInfo.size
        
        if (visibleItemsCount < totalItemsCount) {
            val viewPortHeight = size.height
            val thumbHeight = (visibleItemsCount.toFloat() / totalItemsCount) * viewPortHeight
            
            val firstVisibleItem = visibleItemsInfo.first()
            val firstVisibleIndex = firstVisibleItem.index
            val firstVisibleOffset = firstVisibleItem.offset.toFloat()
            val itemSize = firstVisibleItem.size.toFloat()
            
            val scrollPercent = (firstVisibleIndex.toFloat() - (firstVisibleOffset / itemSize)) / totalItemsCount.toFloat()
            val thumbOffset = (scrollPercent * viewPortHeight).coerceIn(0f, viewPortHeight - thumbHeight)

            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(size.width - width.toPx(), thumbOffset),
                size = androidx.compose.ui.geometry.Size(width.toPx(), thumbHeight)
            )
        }
    }
}





