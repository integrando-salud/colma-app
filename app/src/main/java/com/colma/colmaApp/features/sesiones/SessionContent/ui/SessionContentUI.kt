package com.colma.colmaApp.features.sesiones.SessionContent.ui

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.colma.colmaApp.features.multimedia.ui.VideoContent
import com.colma.colmaApp.features.multimedia.ui.AudioContent
import com.colma.colmaApp.features.multimedia.ui.InformationCard

import com.colma.colmaApp.features.sesiones.SessionContent.data.SessionContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentEvent
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentUiState
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentViewModel
import com.colma.colmaApp.features.sesiones.SessionContent.viewmodel.SessionContentViewModelFactory
import com.colma.colmaApp.features.multimedia.ui.scrollbar
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import android.graphics.Color as AndroidColor

@Composable
fun SessionContentScreen(
    uiState: SessionContentUiState,
    onEvent: (SessionContentEvent) -> Unit,
    viewModel: SessionContentViewModel,
    onBack: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val tabs = listOf("Información", "Video", "Audio")
    @RawRes val audioRes = R.raw.audio_explicativo

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SessionContentViewModel.NavigationEvent.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background))
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .scrollbar(scrollState)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp))
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
                            InformationCard(displayContent = uiState.informationContent)
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            VideoContent(contentAssetId = R.raw.hta_sesion_5_1)
                        }
                    }
                    2 -> {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            AudioContent(audioResId = audioRes)
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onEvent(SessionContentEvent.TerminarClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = signatureColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Terminar", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
            }
        }
    }
}
