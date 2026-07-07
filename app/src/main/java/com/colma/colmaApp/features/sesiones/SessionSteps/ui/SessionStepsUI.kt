package com.colma.colmaApp.features.sesiones.SessionSteps.ui

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.colma.colmaApp.features.sesiones.SessionSteps.data.SessionStepsRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsEvent
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsUiState
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsViewModel
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsViewModelFactory
import com.colma.colmaApp.features.multimedia.ui.scrollbar
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.Brush
import com.colma.colmaApp.features.sesiones.SessionList.ui.shimmerBrush
import com.colma.colmaApp.ui.theme.ColmaAppTheme

// SessionStepsUI activity has been migrated to MainActivity.kt as a NavHost route.
// The Composable functions and helper classes are retained here for routing.

val LocalScreenStartTime = staticCompositionLocalOf { 0L }

@Composable
fun SessionStepsScreen(
    uiState: SessionStepsUiState,
    onEvent: (SessionStepsEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)
    val startTime = remember { System.currentTimeMillis() }

    CompositionLocalProvider(LocalScreenStartTime provides startTime) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.background))
        ) {
            if (uiState.error != null) {
                Text(text = "Error: ${uiState.error}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else {
                Crossfade(targetState = uiState.isLoading, label = "sessionStepsContent") { loading ->
                    if (loading) {
                        val brush = shimmerBrush()
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                repeat(3) {
                                    SessionStepCardSkeleton(brush = brush)
                                }
                            }
                            StepProgressBarSkeleton(brush = brush)
                        }
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
                                    StaggeredItemWrapper(index = index) {
                                        SessionStepCard(option = option, signatureColor = signatureColor) {
                                            onEvent(SessionStepsEvent.SessionStepClicked(option, index))
                                        }
                                    }
                                }
                            }
                            if (uiState.multimediaOptions.isNotEmpty()) {
                                StaggeredItemWrapper(index = uiState.multimediaOptions.size) {
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
            }
        }
    }
}

@Composable
fun SessionStepCardSkeleton(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(brush)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun StepProgressBarSkeleton(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(brush)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun StaggeredItemWrapper(
    index: Int,
    content: @Composable () -> Unit
) {
    val screenStartTime = LocalScreenStartTime.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val elapsed = System.currentTimeMillis() - screenStartTime
        if (elapsed < 500L) {
            val delayTime = (index * 40L).coerceAtMost(240L)
            delay(delayTime)
        } else {
            // Already past initial animation window; render immediately
            isVisible = true
        }
        isVisible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f, // 30px offset
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "translationY"
    )
    Box(
        modifier = Modifier.graphicsLayer(
            alpha = alpha,
            translationY = translationY
        )
    ) {
        content()
    }
}

@Composable
fun SessionStepCard(
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
                if (option.type != MultimediaContentType.DESCRIPTION) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(option.description, fontSize = 14.sp, color = Color.Gray)
                }
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
