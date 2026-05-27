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

class MultimediaSelectorUI : ComponentActivity() {

    private val viewModel: MultimediaSelectorViewModel by viewModels {
        MultimediaSelectorViewModelFactory(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        // Reload options to update completion status (ticks)
        if (viewModel.uiState.value.sessionId != -1L) {
            viewModel.loadMultimediaOptions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }

        val sessionId = intent?.getLongExtra("sessionId", -1L) ?: -1L
        val sessionTitle = intent?.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent?.getLongExtra("subSessionId", -1L) ?: -1L
        val subSessionTitle = intent?.getStringExtra("subSessionTitle") ?: ""
        val treatmentTypesString = intent?.getStringExtra("treatmentTypes") ?: "HTA"
        val treatmentTypes = treatmentTypesString.split(',').mapNotNull {
            try {
                TreatmentType.valueOf(it.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }.ifEmpty { listOf(TreatmentType.HTA) }

        viewModel.initialize(sessionId, sessionTitle, subSessionId, subSessionTitle, treatmentTypes)

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is MultimediaSelectorViewModel.NavigationEvent.NavigateToMultimedia -> {
                                navigateToMultimedia(event.option, event.index, uiState)
                            }
                            is MultimediaSelectorViewModel.NavigationEvent.NavigateBack -> {
                                finish()
                            }
                        }
                    }
                }

                MultimediaSelectorScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }

    private fun navigateToMultimedia(option: MultimediaOption, index: Int, uiState: MultimediaSelectorUiState) {
        val intent = Intent(this, com.colma.colmaApp.features.multimedia.ui.MultimediaActivity::class.java)
        intent.apply {
            putExtra("sessionId", uiState.sessionId)
            putExtra("sessionTitle", uiState.sessionTitle)
            putExtra("subSessionId", uiState.subSessionId)
            putExtra("subSessionTitle", uiState.subSessionTitle)
            putExtra("treatmentTypes", uiState.treatmentTypes.joinToString(",") { it.name })
            putExtra("currentMultimediaIndex", index)
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaSelectorScreen(
    uiState: MultimediaSelectorUiState,
    onEvent: (MultimediaSelectorEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)

    fun friendlySubLabel(subId: Long, parentId: Long): String {
        return try {
            val maybeIndex = subId - parentId * 10
            if (maybeIndex in 1..999) "Semana $maybeIndex" else "Semana $subId"
        } catch (e: Exception) {
            "Semana $subId"
        }
    }

    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding(), color = colorResource(R.color.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorResource(R.color.contraste),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .heightIn(min = 36.dp)
                            .clickable { onEvent(MultimediaSelectorEvent.BackClicked) }
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cerrar",
                                color = colorResource(R.color.white),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${uiState.error}", color = Color.Red)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
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
                    if (uiState.totalSteps > 0) {
                        StepProgressBar(
                            currentStep = uiState.currentStepIndex,
                            totalSteps = uiState.totalSteps,
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
    currentStep: Int,
    totalSteps: Int,
    signatureColor: Color,
    modifier: Modifier = Modifier
) {
    if (totalSteps <= 0) return

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
        ) {            for (i in 1..totalSteps) {
                // Connector line before each step (except the first)
                if (i > 1) {
                    val lineColor = if (i <= currentStep) lightSignatureColor else incompleteColor
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

                // Step item: dot + optional label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(dotSize),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            i < currentStep -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sesión $i completada",
                                    tint = lightSignatureColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            i == currentStep -> {
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
                    Text(
                        text = if (i == currentStep) "Sesión $i" else "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (i == currentStep) signatureColor else Color.Transparent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Connector line after each step (except the last)
                if (i < totalSteps) {
                    val lineColor = if (i < currentStep) lightSignatureColor else incompleteColor
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
