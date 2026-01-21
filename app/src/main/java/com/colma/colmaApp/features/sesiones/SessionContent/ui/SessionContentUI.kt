package com.colma.colmaApp.features.sesiones.SessionContent.ui

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.multimediaScreens.ActionPage
import com.colma.colmaApp.features.multimediaScreens.FinishPage
import com.colma.colmaApp.features.multimediaScreens.WelcomePage
import com.colma.colmaApp.features.sesiones.SessionContent.data.SessionContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentEvent
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentUiState
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentViewModel
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentViewModelFactory
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import android.graphics.Color as AndroidColor

class SessionContentUI : ComponentActivity() {

    private val viewModel: SessionContentViewModel by viewModels {
        SessionContentViewModelFactory(SessionContentRepositoryImpl())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }

        val sessionId = intent?.getLongExtra("sessionId", -1L) ?: -1L
        val sessionTitle = intent?.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent?.getLongExtra("subSessionId", -1L) ?: -1L
        val subSessionTitle = intent?.getStringExtra("subSessionTitle")

        viewModel.onEvent(
            SessionContentEvent.LoadSession(
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                subSessionId = subSessionId.takeIf { it != -1L },
                subSessionTitle = subSessionTitle
            )
        )

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()

                SessionContentScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionContentScreen(
    uiState: SessionContentUiState,
    onEvent: (SessionContentEvent) -> Unit,
    viewModel: SessionContentViewModel
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val activity = context as? Activity

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

    DisposableEffect(key1 = Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    LaunchedEffect(exoPlayer) {
        while (true) {
            val duration = exoPlayer.duration
            if (duration > 0 && duration != C.TIME_UNSET) {
                onEvent(SessionContentEvent.PlayerReady(duration))
                break
            }
            delay(100)
        }
    }

    LaunchedEffect(uiState.isPlaying) {
        exoPlayer.playWhenReady = uiState.isPlaying

        if (uiState.isPlaying) {
            while (true) {
                onEvent(SessionContentEvent.UpdatePlaybackPosition(exoPlayer.currentPosition))
                delay(250)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SessionContentViewModel.NavigationEvent.NavigateBack -> {
                    activity?.finish()
                }
                is SessionContentViewModel.NavigationEvent.SeekAudio -> {
                    exoPlayer.seekTo(event.position)
                }
            }
        }
    }

    fun friendlySubLabel(subId: Long, parentId: Long): String {
        return "Semana $subId"
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = uiState.sessionTitle, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        val secondLine = when {
                            !uiState.subSessionTitle.isNullOrBlank() -> uiState.subSessionTitle
                            uiState.subSessionId != null && uiState.sessionId >= 0L -> friendlySubLabel(uiState.subSessionId, uiState.sessionId)
                            else -> null
                        }
                        if (secondLine != null) {
                            Text(text = secondLine, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = { onEvent(SessionContentEvent.BackClicked) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
                        onClick = { onEvent(SessionContentEvent.TerminarClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = signatureColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = colorResource(R.color.background)
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { onEvent(SessionContentEvent.TabSelected(index)) },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (uiState.selectedTab) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            WelcomePage(displayContent = uiState.informationContent)
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
                                audioDurationMs = if (uiState.durationMs > 0L) uiState.durationMs else 120_000L,
                                playbackPositionMs = uiState.playbackPositionMs,
                                isPlaying = uiState.isPlaying,
                                onPlayPauseToggle = { onEvent(SessionContentEvent.PlayPauseToggle) },
                                onSeek = { onEvent(SessionContentEvent.SeekAudio(it)) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
