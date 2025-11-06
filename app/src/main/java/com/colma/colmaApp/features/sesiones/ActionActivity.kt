package com.colma.colmaApp.features.sesiones

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.utils.videoPlayer.FullscreenVideoActivity
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

@OptIn(ExperimentalMaterial3Api::class)
class ActionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColmaAppTheme {
                ActionPageScreen(
                    onBack = { finish() },
                    onNext = {
                        startActivity(Intent(this@ActionActivity, FinishActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPageScreen(onBack: () -> Unit = {}, onNext: () -> Unit = {}) {
    val signatureColor = Color(0xFF0D97B6) // use your color
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Acción", fontSize = 18.sp, color = Color.White) },
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
                ActionPage()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = signatureColor, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Siguiente", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ActionPage() {
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
                .padding(24.dp),
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
            VideoPlayerWithFullscreen(resId = R.raw.exercise, modifier = Modifier
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

    // Keep a single player instance for this composable
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = false
            prepare()
        }
    }

    // Release when leaving composable
    DisposableEffect(Unit) {
        onDispose {
            try {
                exoPlayer.release()
            } catch (_: Exception) { }
        }
    }

    // Inline player (small)
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // choose Fit or Fill depending on desired behavior
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .size(width = playerWidth, height = playerHeight)
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.Center)
        )

        // Fullscreen button: launch the FullscreenVideoActivity directly
        IconButton(
            onClick = {
                // build the same URI you used to prepare the inline player
                val uriString = "android.resource://${context.packageName}/$resId"
                // capture current playback position
                val positionMs = exoPlayer.currentPosition
                // pause inline playback so we don't have two players playing
                exoPlayer.playWhenReady = false
                // start fullscreen activity
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

