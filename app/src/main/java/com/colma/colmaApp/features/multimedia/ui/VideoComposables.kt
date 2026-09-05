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
    isAutoZoomed: Boolean = false,
    navigationButtons: @Composable () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isVideoAvailable = remember(localFilePath, contentAssetId) {
        (localFilePath != null && java.io.File(localFilePath).exists()) || (contentAssetId != null && contentAssetId != 0)
    }

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
                        if (isVideoAvailable) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                VideoPlayerWithFullscreen(
                                    resId = contentAssetId,
                                    localFilePath = localFilePath,
                                    isAutoZoomed = isAutoZoomed,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_unknown_document_24),
                                    contentDescription = "Video no disponible",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Video no disponible",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF333333),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Hubo un problema al descargar este video o no se encuentra disponible en el dispositivo.",
                                    fontSize = 15.sp,
                                    color = Color(0xFF666666),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Puedes continuar con la sesión utilizando los botones de abajo.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF888888),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
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
    isAutoZoomed: Boolean = false,
    modifier: Modifier = Modifier,
    playerWidth: Dp = 320.dp,
    playerHeight: Dp = 200.dp
) {
    val context = LocalContext.current
    val uriString = remember(localFilePath, resId) {
        if (localFilePath != null && java.io.File(localFilePath).exists()) {
            localFilePath
        } else if (resId != null && resId != 0) {
            "android.resource://${context.packageName}/$resId"
        } else {
            "android.resource://${context.packageName}/${R.raw.exercise}"
        }
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
    val isExceptionVideo = isAutoZoomed || remember(uriString) {
        uriString.contains("exercise", ignoreCase = true) ||
        uriString.contains("sesion_6_2", ignoreCase = true) ||
        uriString.contains("sesion_6", ignoreCase = true) ||
        uriString.contains("s6.2", ignoreCase = true) ||
        uriString.contains("s6_2", ignoreCase = true) ||
        uriString.contains("s6-2", ignoreCase = true) ||
        uriString.contains("semana_6", ignoreCase = true) ||
        uriString.contains("/6/2/", ignoreCase = true) ||
        uriString.contains("/6/2", ignoreCase = true)
    }
    Box(
        modifier = modifier
            .background(Color.Black, shape = RoundedCornerShape(8.dp))
            .clickable {
                val intent = Intent(context, FullscreenVideoActivity::class.java).apply {
                    putExtra(FullscreenVideoActivity.EXTRA_URI, uriString)
                    putExtra(FullscreenVideoActivity.EXTRA_POSITION, lastPosition)
                    putExtra(FullscreenVideoActivity.EXTRA_IS_ZOOMED, isExceptionVideo)
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
                    resizeMode = if (isExceptionVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                view.resizeMode = if (isExceptionVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
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
