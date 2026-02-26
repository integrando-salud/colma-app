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

        // PlayerView set up
        playerView = PlayerView(this).apply {
            player = exoPlayer
            useController = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            controllerShowTimeoutMs = 3000
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Root container to host player + overlay button
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        // Add player view
        root.addView(playerView)

        // Exit fullscreen button (top-right)
        val exitBtn = ImageButton(this).apply {
            // Use a system drawable; replace with your own if you want
            setImageResource(R.drawable.outline_fullscreen_exit_24)
            // remove default background so it looks like a floating icon; tweak as needed
            setBackgroundResource(android.R.color.transparent)
            // ensure the icon is visible on dark background
            setColorFilter(android.graphics.Color.WHITE)
            // accessibility
            contentDescription = "Salir pantalla completa"
            // click behavior: send position back and finish
            setOnClickListener {
                val pos = exoPlayer?.currentPosition ?: 0L
                // pause player
                exoPlayer?.playWhenReady = false

                // set result - caller can read this via ActivityResult API
                val result = Intent().putExtra(RESULT_POSITION, pos)
                setResult(Activity.RESULT_OK, result)

                finish()
            }
            // make it not consume too much layout space
            adjustViewBounds = true
        }

        // Layout params for button (top-right with margin)
        val btnSize = resources.displayMetrics.density.times(44).toInt() // ~44dp
        val lp = FrameLayout.LayoutParams(btnSize, btnSize, Gravity.TOP or Gravity.END).apply {
            val marginPx = (resources.displayMetrics.density * 12).toInt()
            topMargin = marginPx
            rightMargin = marginPx
        }
        root.addView(exitBtn, lp)

        setContentView(root)
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
