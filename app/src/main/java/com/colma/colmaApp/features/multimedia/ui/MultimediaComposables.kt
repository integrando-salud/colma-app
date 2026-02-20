package com.colma.colmaApp.features.multimedia.ui

import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView

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
import androidx.compose.foundation.lazy.itemsIndexed

import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog
import com.colma.colmaApp.utils.varios.CardThatOpensEditDialog
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import android.widget.Toast

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
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





@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InformationDisplayContent(displayContent: String) {
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
                InformationCard(displayContent = paragraphs[page])
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
            InformationCard(displayContent = "No content provided.")
        }
    }
}

@Composable
fun InformationCard(displayContent: String) {
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Información importante",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
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
        }
    )
}

@Composable
fun FinishPage(
    audioDurationMs: Long = 120_000L,
    playbackPositionMs: Long = 0L,
    isPlaying: Boolean = false,
    onPlayPauseToggle: () -> Unit = {},
    onSeek: (Long) -> Unit = {}
) {
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
                .verticalScroll(rememberScrollState()),
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
fun VideoContent(@RawRes contentAssetId: Int) {
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
                .verticalScroll(rememberScrollState()),
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
                    .size(width = 320.dp, height = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "A continuación puedes ver un video explicativo para entender mejor los ejercicios de esta sesión.",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 4.dp),
                // center text within card
            )
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
    var playbackPosition by rememberSaveable(resId) { mutableStateOf(0L) }
    var playWhenReady by rememberSaveable(resId) { mutableStateOf(true) }
    val exoPlayer = remember(resId) {
        ExoPlayer.Builder(context).build()
    }
    DisposableEffect(key1 = resId) {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.seekTo(playbackPosition)
        exoPlayer.playWhenReady = playWhenReady
        onDispose {
            playbackPosition = exoPlayer.currentPosition
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
    }
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .size(width = playerWidth, height = playerHeight)
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.Center)
        )
        IconButton(
            onClick = {
                val uriString = "android.resource://${context.packageName}/$resId"
                val positionMs = exoPlayer.currentPosition
                exoPlayer.playWhenReady = false
                FullscreenVideoActivity.start(context, uriString, positionMs)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(6.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_fullscreen_24),
                contentDescription = "Fullscreen",
                tint = Color.White
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
    onFinish: () -> Unit
) {
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember { mutableStateOf(false) } // Keep this for UI feedback
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val allQuestionsAnswered = userAnswers.size == quizQuestions.size

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // Space for the button
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(quizQuestions) { index, question ->
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
                                        is_correct = if (isCorrect) 1 else 0
                                    )
                                )
                            }
                            val percentageCorrect = if (totalQuestions > 0) (correctAnswers.toDouble() / totalQuestions.toDouble()) * 100 else 0.0

                            val quizResponseRecord = QuizResponseRecord(
                                id_quiz_response = 0, // Will be filled by DB
                                id_usuario = 1, // Default user ID
                                quiz_type = MultimediaContentType.QUIZ.name,
                                week_id = sessionId.toInt(),
                                session_id = subSessionId.toInt(),
                                quiz_title = quizTitle,
                                fecha_registro = null, // Will be set by DB
                                fecha_sincronizacion = null,
                                total_questions = totalQuestions,
                                correct_answers = correctAnswers,
                                percentage_correct = percentageCorrect,
                                multimedia_unique_key = multimediaUniqueKey
                            )
                            onQuizSubmit(quizResponseRecord, questionResponses)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false },
                        colors = ButtonDefaults.textButtonColors(containerColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!quizSubmitted) {
                Button(
                    onClick = {
                        showConfirmationDialog = true
                    },
                    enabled = allQuestionsAnswered,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal))
                ) {
                    Text("Confirmar")
                }
            }
        }
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
                CardThatOpensSelectionDialog(
                    title = "Selecciona tu respuesta",
                    description = selectedAnswerIndex?.let { question.options.getOrNull(it) } ?: "Toca para responder",
                    items = question.options,
                    onItemSelected = { selectedOption ->
                        onAnswerSelected(question.options.indexOf(selectedOption))
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasContent(metasId: String, onFinish: () -> Unit) {
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
    val dbHelper = remember { DBhelper(context) }
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
    var summaryMessage by rememberSaveable { mutableStateOf("") }
    var shouldExecuteFinalAction by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (actividades, conQuienOptions) = getMetasOptions(treatmentType)

            CardThatOpensSelectionDialog(
                title = "Actividad",
                description = "Seleccione la actividad concreta que desee realizar.",
                items = actividades,
                label = { it },
                onItemSelected = { chosen ->
                    actividad.value = chosen
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensSelectionDialog(
                title = "Con quien",
                description = "Quien te acompañará en esta actividad?",
                items = conQuienOptions,
                label = { it },
                onItemSelected = { chosen ->
                    conQuien.value = chosen
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensEditDialog(
                title = "Donde?",
                description = "Describe brevemente en donde lo harás",
                placeholder = "Escribe el lugar...",
                onSave = { newDesc ->
                    donde.value = newDesc
                }
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CardThatOpensDateTimeDialog(
                title = "Fecha Objetivo",
                description = "Selecciona la fecha límite para tu meta.",
                initialMillis = today.timeInMillis,
                onDateTimeSelected = { selectedCal ->
                    selectedCal.set(Calendar.SECOND, 0)
                    fecha.value = dbDateTimeFormat.format(selectedCal.time)
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val missing = buildList {
                        if (actividad.value.isNullOrBlank()) add("Actividad")
                        if (conQuien.value.isNullOrBlank()) add("Con quién")
                        if (donde.value.isNullOrBlank()) add("Dónde")
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
                            Resumen de registro:
                            • Actividad: ${actividad.value}
                            • Con quién: ${conQuien.value}
                            • Dónde: ${donde.value}
                            • Fecha Objetivo: $fechaObjetivoForDisplay
                            • Fecha de Creación: $fechaCreacionForDisplay
                        """.trimIndent()
                    }
                    showSummaryDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.principal),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
            }
            if (showSummaryDialog) {
                CustomAlertDialog(
                    show = showSummaryDialog,
                    onDismissRequest = { showSummaryDialog = false },
                    title = "Resultado",
                    description = summaryMessage,
                    yesLabel = "OK",
                    onYes = {
                        if (shouldExecuteFinalAction) {
                            scope.launch {
                                val newMeta = RegistrosSaludModelos.MetaRecord(
                                    id_meta = 0,
                                    id_usuario = 1,
                                    semana = null,
                                    sesion = null,
                                    tipo_actividad = null,
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
                                    dbHelper.insertMeta(newMeta)
                                }
                                if (result != -1L) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Meta guardada exitosamente", Toast.LENGTH_SHORT).show()
                                        onFinish()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error al guardar la meta", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        showSummaryDialog = false
                    }
                )
            }
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




