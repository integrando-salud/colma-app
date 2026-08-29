package com.colma.colmaApp.features.multimedia.ui

import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.util.concurrent.TimeUnit

@Composable
fun AudioContent(
    @RawRes audioResId: Int?,
    localFilePath: String? = null,
    navigationButtons: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val uriString = if (localFilePath != null) localFilePath else "android.resource://${context.packageName}/$audioResId"
    val exoPlayer = remember(uriString) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            try { exoPlayer.stop() } catch (_: Exception) {}
            exoPlayer.release()
        }
    }

    var isPlaying by rememberSaveable(uriString) { mutableStateOf(false) }
    var playbackPositionMs by remember(uriString) { mutableStateOf(0L) }
    var durationMs by remember(uriString) { mutableStateOf(0L) }

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
        audioDurationMs = if (durationMs > 0L) durationMs else 120_000L,
        playbackPositionMs = playbackPositionMs,
        isPlaying = isPlaying,
        onPlayPauseToggle = { isPlaying = !isPlaying },
        onSeek = { newPos ->
            val pos = newPos.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlexibleFooterColumn(
                minHeight = screenHeight,
                content = {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("Audio Explicativo", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(R.drawable.outline_headphones_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFE84589)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                                    onValueChange = { frac -> onSeek((frac * duration).toLong()) },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatTime(playbackPositionMs), fontSize = 12.sp, color = Color.Gray)
                                Text(formatTime(audioDurationMs), fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Pulsa el botón de play para escuchar el audio explicativo.", fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                    }
                },
                footer = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        navigationButtons()
                    }
                }
            )
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
