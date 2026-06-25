package com.colma.colmaApp.features.sesiones.MultimediaSelector.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.MultimediaSelector.data.MultimediaRepositoryImpl
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType

import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel.MultimediaSelectorEvent
import com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel.MultimediaSelectorUiState
import com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel.MultimediaSelectorViewModel
import com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel.MultimediaSelectorViewModelFactory
import com.colma.colmaApp.features.multimedia.ui.scrollbar
import com.colma.colmaApp.ui.theme.ColmaAppTheme

// MultimediaSelectorUI activity has been migrated to MainActivity.kt as a NavHost route.
// The Composable functions and helper classes are retained here for routing.

@Composable
fun MultimediaSelectorScreen(
    uiState: MultimediaSelectorUiState,
    onEvent: (MultimediaSelectorEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .scrollbar(scrollState)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    uiState.multimediaOptions.forEachIndexed { index, option ->
                        MultimediaOptionCard(option = option, signatureColor = signatureColor) {
                            onEvent(MultimediaSelectorEvent.MultimediaOptionClicked(option, index))
                        }
                    }
                }
                if (uiState.multimediaOptions.isNotEmpty()) {
                    StepProgressBar(
                        options = uiState.multimediaOptions,
                        signatureColor = signatureColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MultimediaOptionCard(
    option: MultimediaOption,
    signatureColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.iconImageVector != null) {
                Icon(
                    imageVector = option.iconImageVector,
                    contentDescription = option.title,
                    tint = signatureColor,
                    modifier = Modifier.size(40.dp)
                )
            } else if (option.iconResId != null) {
                Icon(
                    painter = painterResource(id = option.iconResId),
                    contentDescription = option.title,
                    tint = signatureColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(option.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(option.description, fontSize = 14.sp, color = Color.Gray)
            }

            if (option.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completado",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp).padding(start = 8.dp)
                )
            }
        }
    }
}
@Composable
fun StepProgressBar(
    options: List<MultimediaOption>,
    signatureColor: Color,
    modifier: Modifier = Modifier
) {
    val totalSteps = options.size
    if (totalSteps <= 0) return

    val activeIndex = options.indexOfFirst { !it.isCompleted }.let {
        if (it == -1) totalSteps - 1 else it
    }

    val lightSignatureColor = signatureColor.copy(alpha = 0.5f)
    val incompleteColor = Color(0xFFD0D5DD)
    val dotSize = 22.dp
    val connectorWidth = 24.dp

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0 until totalSteps) {
                val option = options[i]

                // Connector line before each step (except the first)
                if (i > 0) {
                    val lineColor = if (i <= activeIndex) lightSignatureColor else incompleteColor
                    Canvas(
                        modifier = Modifier
                            .width(connectorWidth)
                            .height(2.dp)
                    ) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Step item: dot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(dotSize),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            option.isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Contenido ${i + 1} completado",
                                    tint = lightSignatureColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            i == activeIndex -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(3.dp, signatureColor, CircleShape)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(signatureColor, CircleShape)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(incompleteColor, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Connector line after each step (except the last)
                if (i < totalSteps - 1) {
                    val lineColor = if (i < activeIndex) lightSignatureColor else incompleteColor
                    Canvas(
                        modifier = Modifier
                            .width(connectorWidth)
                            .height(2.dp)
                    ) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}
