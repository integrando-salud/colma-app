package com.colma.colmaApp.features.sesiones

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.colma.colmaApp.features.multimediaScreens.ActionPage
import com.colma.colmaApp.features.multimediaScreens.FinishPage
import com.colma.colmaApp.features.multimediaScreens.WelcomePage
import android.app.Activity

/**
 * SessionContent Activity that reuses WelcomePage, ActionPage and FinishPage.
 */
class SessionContent : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        val sessionId = intent?.getLongExtra("sessionId", -1L) ?: -1L
        val sessionTitle = intent?.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent?.getLongExtra("subSessionId", -1L) ?: -1L
        val subSessionTitle = intent?.getStringExtra("subSessionTitle") // NEW

        setContent {
            ColmaAppTheme {
                SessionContentScreen(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    subSessionId = subSessionId.takeIf { it != -1L },
                    subSessionTitle = subSessionTitle, // NEW
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ContextCastToActivity")
fun SessionContentScreen(
    sessionId: Long,
    sessionTitle: String,
    subSessionId: Long? = null,
    subSessionTitle: String? = null,
    onBack: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    // Tabs
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Información", "Video", "Audio")


    @RawRes val audioRes = R.raw.audio_explicativo


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$audioRes")
            setMediaItem(MediaItem.fromUri(uri))
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

    fun friendlySubLabel(subId: Long, parentId: Long): String {
        return try {
            val maybeIndex = subId - parentId * 10
            if (maybeIndex in 1..999) {
                "Semana $maybeIndex"
            } else {
                "Semana $subId"
            }
        } catch (e: Exception) {
            "Semana $subId"
        }
    }


    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }


    LaunchedEffect(exoPlayer) {
        var attempts = 0
        while ((durationMs <= 0L || durationMs == com.google.android.exoplayer2.C.TIME_UNSET) && attempts < 30) {
            val d = exoPlayer.duration
            if (d > 0 && d != com.google.android.exoplayer2.C.TIME_UNSET) {
                durationMs = d
                break
            }
            attempts++
            kotlinx.coroutines.delay(120)
        }
        if (durationMs <= 0L) durationMs = exoPlayer.duration.coerceAtLeast(0L)
    }


    LaunchedEffect(isPlaying, exoPlayer) {
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            while (isPlaying) {
                playbackPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                kotlinx.coroutines.delay(250L)
            }
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    val onPlayPauseToggle: () -> Unit = {
        isPlaying = !isPlaying
        exoPlayer.playWhenReady = isPlaying
    }

    val onSeek: (Long) -> Unit = { newPos ->
        val bounded = newPos.coerceIn(0L, (exoPlayer.duration.coerceAtLeast(0L)))
        exoPlayer.seekTo(bounded)
        playbackPositionMs = bounded
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = sessionTitle, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        val secondLine = when {
                            !subSessionTitle.isNullOrBlank() -> subSessionTitle
                            subSessionId != null && sessionId >= 0L -> friendlySubLabel(subSessionId, sessionId)
                            else -> null
                        }
                        if (secondLine != null) {
                            Text(text = secondLine, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            try { exoPlayer.playWhenReady = false } catch (_: Exception) {}
                            activity?.finish()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = signatureColor,
                            contentColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        WelcomePage()
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ActionPage()
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        FinishPage(
                            audioDurationMs = if (durationMs > 0L) durationMs else 120_000L,
                            playbackPositionMs = playbackPositionMs,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = onPlayPauseToggle,
                            onSeek = onSeek
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
