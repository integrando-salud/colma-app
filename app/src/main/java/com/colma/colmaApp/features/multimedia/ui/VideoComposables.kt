package com.colma.colmaApp.features.multimedia.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.colma.colmaApp.R
import com.colma.colmaApp.utils.videoPlayer.FullscreenVideoActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun VideoContent(
    @RawRes contentAssetId: Int?,
    localFilePath: String? = null,
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
                                Text("Video Explicativo", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(R.drawable.outline_videocam_24),
                                    contentDescription = "Ver video",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFFE84589)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            VideoPlayerWithFullscreen(
                                resId = contentAssetId,
                                localFilePath = localFilePath,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
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

@Composable
fun VideoPlayerWithFullscreen(
    @RawRes resId: Int?,
    localFilePath: String? = null,
    modifier: Modifier = Modifier,
    playerWidth: Dp = 320.dp,
    playerHeight: Dp = 200.dp
) {
    val context = LocalContext.current
    val uriString = when {
        localFilePath != null -> localFilePath
        resId != null && resId != 0 -> "android.resource://${context.packageName}/$resId"
        else -> ""
    }
    var lastPosition by rememberSaveable(uriString) { mutableStateOf(0L) }
    val exoPlayer = remember(uriString) {
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
    DisposableEffect(key1 = uriString) {
        onDispose { exoPlayer.release() }
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
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                contentDescription = "Reproducir en pantalla completa",
                tint = Color.White,
                modifier = Modifier.padding(8.dp).fillMaxSize()
            )
        }
    }
}
