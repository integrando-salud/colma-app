package com.colma.colmaApp.features.multimedia.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaEvent
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaUiState
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaViewModel
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaViewModelFactory
import com.colma.colmaApp.features.sesiones.MultimediaSelector.data.MultimediaRepositoryImpl
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption // NEW
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType // NEW
import com.colma.colmaApp.ui.theme.ColmaAppTheme

class MultimediaActivity : ComponentActivity() {

    private val viewModel: MultimediaViewModel by viewModels {
        MultimediaViewModelFactory(applicationContext)
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

        val sessionId = intent.getLongExtra("sessionId", -1L)
        val sessionTitle = intent.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent.getLongExtra("subSessionId", -1L)
        val subSessionTitle = intent.getStringExtra("subSessionTitle") ?: ""
        val treatmentTypesString = intent.getStringExtra("treatmentTypes") ?: ""
        val currentMultimediaIndex = intent.getIntExtra("currentMultimediaIndex", 0)

        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                subSessionId = subSessionId,
                subSessionTitle = subSessionTitle,
                treatmentTypesString = treatmentTypesString,
                startIndex = currentMultimediaIndex
            )
        )

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is MultimediaViewModel.NavigationEvent.NavigateBack -> {
                                finish()
                            }
                        }
                    }
                }

                MultimediaScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

// Top-level helper function
fun generateMultimediaUniqueKeyForQuiz(
    option: MultimediaOption,
    treatmentId: String,
    weekId: Long,
    sessionId: Long
): String {
    val sanitizedTitle = option.title.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
    return "QUIZ_${treatmentId}_${weekId}_${sessionId}_${sanitizedTitle}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaScreen(
    uiState: MultimediaUiState,
    onEvent: (MultimediaEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)
    val currentOption = uiState.currentMultimediaOption

    Surface(color = colorResource(id = R.color.background), modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(currentOption?.title ?: uiState.sessionTitle, fontSize = 18.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    TextButton(onClick = { onEvent(MultimediaEvent.Finish) }) {
                        Text(text = "Cerrar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator()
                    }
                    uiState.error != null -> {
                        Text(text = "Error: ${uiState.error}", color = Color.Red)
                    }
                    currentOption != null -> {
                        when (currentOption.type) {
                            MultimediaContentType.DESCRIPTION -> {
                                InformationDisplayContent(
                                    displayContent = currentOption.fullDescription ?: currentOption.description
                                )
                            }
                            MultimediaContentType.VIDEO -> {
                                currentOption.contentAssetId?.let {
                                    VideoContent(contentAssetId = it)
                                } ?: Text("Video resource not found.")
                            }
                            MultimediaContentType.AUDIO -> {
                                currentOption.contentAssetId?.let {
                                    AudioContent(audioResId = it)
                                } ?: Text("Audio resource not found.")
                            }
                            MultimediaContentType.METAS -> {
                                currentOption.metasId?.let {
                                    MetasContent(metasId = it, onFinish = { onEvent(MultimediaEvent.Finish) })
                                } ?: Text("Metas not found.")
                            }
                            MultimediaContentType.QUIZ -> {
                                currentOption.questions?.let { questions ->
                                    val quizTitle = currentOption.title
                                    val quizTreatmentId = uiState.treatmentTypes.firstOrNull()?.name ?: "UNKNOWN"
                                    val quizUniqueKey = generateMultimediaUniqueKeyForQuiz(
                                        currentOption,
                                        quizTreatmentId,
                                        uiState.sessionId,
                                        uiState.subSessionId
                                    )
                                    QuizContent(
                                        quizQuestions = questions,
                                        quizTitle = quizTitle,
                                        sessionId = uiState.sessionId,
                                        subSessionId = uiState.subSessionId,
                                        treatmentTypes = uiState.treatmentTypes,
                                        multimediaUniqueKey = quizUniqueKey,
                                        onQuizSubmit = { quizResponse, questionResponses ->
                                            onEvent(MultimediaEvent.QuizSubmitted(quizResponse, questionResponses))
                                        },
                                        onFinish = { onEvent(MultimediaEvent.GoToNext) }
                                    )
                                } ?: Text("Quiz not found or questions missing.")
                            }
                            else -> {
                                Text("Content for ${currentOption.type.name}: ${currentOption.title}")
                            }
                        }
                    }
                    else -> {
                        Text("No content available.")
                    }
                }
            }

            // Bottom button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onEvent(MultimediaEvent.GoToNext) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = signatureColor, contentColor = Color.White),
                ) {
                    val isLast = uiState.currentMultimediaIndex == uiState.multimediaOptions.lastIndex
                    Text(text = if (isLast) "Finalizar" else "Siguiente", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
