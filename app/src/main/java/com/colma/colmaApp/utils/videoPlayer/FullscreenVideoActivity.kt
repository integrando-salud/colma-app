package com.colma.colmaApp.utils.videoPlayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

class FullscreenVideoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URI = "extra_video_uri"
        const val EXTRA_POSITION = "extra_position_ms"
        const val RESULT_POSITION = "result_position_ms"
        private const val STATE_POSITION = "state_position"
        private const val STATE_PLAY_WHEN_READY = "state_play_when_ready"


        /**
         * Start fullscreen player. Use the ActivityResult API if you want to receive the final
         * playback position (the activity sets it as a result).
         */
        fun start(context: Context, uriString: String, positionMs: Long = 0L) {
            val i = Intent(context, FullscreenVideoActivity::class.java).apply {
                putExtra(EXTRA_URI, uriString)
                putExtra(EXTRA_POSITION, positionMs)
            }
            context.startActivity(i)
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var playWhenReady = true
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait orientation for fullscreen (handled in manifest, but kept here for clarity if needed later)
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide status + navigation bars (use WindowInsetsCompat.Type)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Restore state
        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(STATE_POSITION)
            playWhenReady = savedInstanceState.getBoolean(STATE_PLAY_WHEN_READY)
        } else {
            playbackPosition = intent.getLongExtra(EXTRA_POSITION, 0L)
            playWhenReady = true // Force play when starting
        }


        // Read intent
        val uriString = intent.getStringExtra(EXTRA_URI)


        if (uriString == null) {
            finish() // nothing to play
            return
        }

        // Build ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(playbackPosition)
            player.playWhenReady = playWhenReady
        }

        setContent {
            VideoPlayerScreen(
                exoPlayer = exoPlayer!!,
                onExit = {
                    val pos = exoPlayer?.currentPosition ?: 0L
                    exoPlayer?.playWhenReady = false
                    val result = Intent().putExtra(RESULT_POSITION, pos)
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }
            )
        }
    }

    @Composable
    fun VideoPlayerScreen(exoPlayer: ExoPlayer, onExit: () -> Unit) {
        var isPlaying by remember { mutableStateOf(exoPlayer.playWhenReady) }
        var currentPosition by remember { mutableStateOf(exoPlayer.currentPosition) }
        var duration by remember { mutableStateOf(exoPlayer.duration.coerceAtLeast(0L)) }
        var showControls by remember { mutableStateOf(true) }

        // Update progress
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                kotlinx.coroutines.delay(500)
            }
        }

        // Auto-hide controls
        LaunchedEffect(showControls) {
            if (showControls) {
                kotlinx.coroutines.delay(3000)
                showControls = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (showControls) {
                // Background dim
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                // Top Exit Button
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Slider
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = {
                            val newPos = (it * duration).toLong()
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        }) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "-10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            IconButton(onClick = {
                                isPlaying = !isPlaying
                                exoPlayer.playWhenReady = isPlaying
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        IconButton(onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceIn(0, duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        }) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "+10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        exoPlayer?.let { player ->
            outState.putLong(STATE_POSITION, player.currentPosition)
            outState.putBoolean(STATE_PLAY_WHEN_READY, player.playWhenReady)
        }
    }


    override fun onResume() {
        super.onResume()
        // Ensure system bars are hidden again when resuming
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        exoPlayer?.playWhenReady = playWhenReady
    }

    override fun onPause() {
        super.onPause()
        // pause playback to avoid audio playing in background
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        playerView = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        // behave like the exit button: return playback position
        val pos = exoPlayer?.currentPosition ?: 0L
        exoPlayer?.playWhenReady = false
        val result = Intent().putExtra(RESULT_POSITION, pos)
        setResult(Activity.RESULT_OK, result)
        super.onBackPressed()
    }
}
