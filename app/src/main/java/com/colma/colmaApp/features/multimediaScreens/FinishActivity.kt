package com.colma.colmaApp.features.multimediaScreens

import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.MenuMain.MenuMain
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment.Companion.CenterVertically
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
class FinishActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                // pass the raw audio resource id you want to play; default is R.raw.exercise
                FinishPageScreen(
                    audioResId = R.raw.audio_explicativo,
                    onBack = { finish() },
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishPageScreen(
    @RawRes audioResId: Int = R.raw.audio_explicativo,
    onBack: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current

    // Create & manage ExoPlayer inside Compose so it survives recompositions but releases correctly
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$audioResId")
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    // ensure player is released when the composable leaves composition
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            try {
                exoPlayer.stop()
            } catch (_: Exception) {}
            exoPlayer.release()
        }
    }

    // Playback UI state
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    // Update duration once ready
    LaunchedEffect(exoPlayer) {
        // wait until player reports duration (or up to a small timeout)
        var attempts = 0
        while (durationMs <= 0 && attempts < 20) {
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            attempts++
            delay(100)
        }
    }

    // A coroutine to poll position while playing (runs on composition)
    LaunchedEffect(isPlaying, exoPlayer) {
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            while (isActive && isPlaying) {
                playbackPositionMs = exoPlayer.currentPosition
                // keep duration updated in case it's not ready earlier
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                delay(250L)
            }
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    // Pause when lifecycle indicates (activity pause) - Compose doesn't provide lifecycle directly here,
    // but ExoPlayer will be released on DisposableEffect. If you need stricter lifecycle sync,
    // you can control it via Activity callbacks.
    Surface(color = Color.White, modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Finalizar", fontSize = 18.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pass control handlers to the visual FinishPage
                FinishPage(
                    audioDurationMs = if (durationMs > 0L) durationMs else 120_000L, // fallback duration
                    playbackPositionMs = playbackPositionMs,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = {
                        // toggle play state and reflect on player
                        isPlaying = !isPlaying
                        exoPlayer.playWhenReady = isPlaying
                    },
                    onSeek = { newPos ->
                        val pos = newPos.coerceIn(0L, (exoPlayer.duration.coerceAtLeast(0L)))
                        exoPlayer.seekTo(pos)
                        playbackPositionMs = pos
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        // stop player before navigating
                        try { exoPlayer.playWhenReady = false } catch (_: Exception) {}
                        onFinish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = signatureColor, contentColor = Color.White),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Visual FinishPage composable containing the controls and text.
 * This is purely UI — the actual player control happens in the caller (FinishPageScreen).
 */
@Composable
fun FinishPage(
    audioDurationMs: Long = 120_000L,    // fallback duration
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
            // Title + icon
            Row(
                verticalAlignment = CenterVertically,
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

            // Controls row
            Row(
                verticalAlignment = CenterVertically,
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

                // Slider
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

            // Time labels
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

/** Helper to format milliseconds as mm:ss or hh:mm:ss if long */
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
