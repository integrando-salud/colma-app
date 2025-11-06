package com.colma.colmaApp.utils.videoPlayer
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SmallPlayerControls(
    videoUri: String,
    currentPositionMs: Long // fetch from your player instance
) {
    val context = LocalContext.current

    IconButton(onClick = {
        FullscreenVideoActivity.start(context, videoUri, currentPositionMs)
    }) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Fullscreen")
    }
}