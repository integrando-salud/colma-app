package com.colma.colmaApp.features.multimedia.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import java.io.File
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InformationDisplayContent(
    displayContent: String,
    descriptionAudios: List<Int>? = null,
    localDescriptionAudios: List<String>? = null,
    localDescriptionImages: List<String>? = null,
    navigationButtons: @Composable () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val paragraphs = remember(displayContent) {
        displayContent.split("\n\n").filter { it.isNotBlank() }
    }
    val pageCount = if (paragraphs.isNotEmpty()) {
        paragraphs.size
    } else {
        maxOf(localDescriptionAudios?.size ?: 0, localDescriptionImages?.size ?: 0).coerceAtLeast(1)
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlexibleFooterColumn(
                minHeight = screenHeight,
                content = {
                    if (pageCount > 0) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                        ) { page ->
                            InformationCard(
                                displayContent = paragraphs.getOrNull(page) ?: "",
                                audioResId = descriptionAudios?.getOrNull(page),
                                localAudioPath = localDescriptionAudios?.getOrNull(page),
                                localImagePath = localDescriptionImages?.getOrNull(page)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalPagerIndicator(
                            pagerState = pagerState,
                            pageCount = pageCount,
                            activeColor = signatureColor,
                            inactiveColor = Color.LightGray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        InformationCard(
                            displayContent = "No content provided."
                        )
                    }
                },
                footer = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        navigationButtons()
                    }
                }
            )
        }
    }
}

@Composable
fun InformationCard(
    displayContent: String,
    audioResId: Int? = null,
    localAudioPath: String? = null,
    localImagePath: String? = null
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scrollState = rememberScrollState()

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    val hasAudio = audioResId != null || localAudioPath != null

    DisposableEffect(audioResId, localAudioPath) {
        if (hasAudio) {
            val player = ExoPlayer.Builder(context).build().apply {
                val uri = if (localAudioPath != null) {
                    Uri.parse(localAudioPath)
                } else {
                    Uri.parse("android.resource://${context.packageName}/$audioResId")
                }
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
            exoPlayer = player
            onDispose {
                try { player.stop() } catch (_: Exception) {}
                player.release()
                exoPlayer = null
            }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(Unit) {
        if (!hasAudio) {
            isInitializing = true
            val speech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    mainHandler.post { isTtsReady = true; isInitializing = false }
                } else {
                    mainHandler.post { isInitializing = false }
                }
            }
            speech.language = Locale("es", "ES")
            speech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { mainHandler.post { isSpeaking = true } }
                override fun onDone(utteranceId: String?) { mainHandler.post { isSpeaking = false } }
                override fun onError(utteranceId: String?) { mainHandler.post { isSpeaking = false } }
            })
            tts = speech
        }
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    LaunchedEffect(exoPlayer, isPlayingAudio) {
        if (exoPlayer != null && isPlayingAudio) {
            while (this.isActive && isPlayingAudio) {
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
                if (isInitializing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFE84589), strokeWidth = 2.dp)
                } else if (hasAudio && exoPlayer != null) {
                    IconButton(onClick = {
                        exoPlayer?.let { player ->
                            if (isPlayingAudio) {
                                player.stop(); player.seekTo(0); isPlayingAudio = false
                            } else {
                                player.seekTo(0); player.playWhenReady = true; isPlayingAudio = true
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
                    IconButton(onClick = {
                        if (isSpeaking) { tts?.stop(); isSpeaking = false }
                        else { tts?.speak(displayContent, TextToSpeech.QUEUE_FLUSH, null, "utteranceId") }
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
            if (localImagePath != null) {
                val bitmap = remember(localImagePath) {
                    try {
                        BitmapFactory.decodeFile(localImagePath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.outline_unknown_document_24),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE84589)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.outline_unknown_document_24),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFE84589)
                )
            }
            Spacer(Modifier.height(16.dp))
            if (displayContent.isNotEmpty()) {
                Text(displayContent, fontSize = 16.sp, textAlign = TextAlign.Left)
            }
        }
    }
}
