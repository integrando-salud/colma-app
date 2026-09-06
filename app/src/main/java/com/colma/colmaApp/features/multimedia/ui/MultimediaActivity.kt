package com.colma.colmaApp.features.multimedia.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.colma.colmaApp.features.sesiones.SessionSteps.data.SessionStepsRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption // NEW
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType // NEW
import com.colma.colmaApp.ui.theme.ColmaAppTheme

// MultimediaActivity activity has been migrated to MainActivity.kt as a NavHost route.
// The Composable functions and helper classes are retained here for routing.

// Top-level helper function
fun generateMultimediaUniqueKeyForQuiz(
    option: MultimediaOption,
    treatmentId: String,
    weekId: Long,
    sessionId: Long
): String {
    return com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils.generateMultimediaUniqueKey(
        option = option,
        treatmentId = treatmentId,
        weekId = weekId,
        sessionId = sessionId
    )
}

@Composable
fun MultimediaScreen(
    uiState: MultimediaUiState,
    onEvent: (MultimediaEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)
    val currentOption = uiState.currentMultimediaOption

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
    ) {
        val navButtons = @Composable {
            NavigationButtons(
                currentIndex = uiState.currentMultimediaIndex,
                maxIndex = uiState.multimediaOptions.lastIndex,
                onPrevious = { onEvent(MultimediaEvent.GoToPrevious) },
                onNext = { onEvent(MultimediaEvent.GoToNext) },
                signatureColor = signatureColor
            )
        }

        // Use key to force recomposition when index changes
        key(uiState.currentMultimediaIndex) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(text = "Error: ${uiState.error}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                currentOption != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (currentOption.type) {
                            MultimediaContentType.DESCRIPTION -> {
                                InformationDisplayContent(
                                    title = currentOption.title,
                                    displayContent = currentOption.fullDescription ?: currentOption.description,
                                    descriptionAudios = currentOption.descriptionAudios,
                                    localDescriptionAudios = currentOption.localDescriptionAudios,
                                    localDescriptionImages = currentOption.localDescriptionImages,
                                    navigationButtons = navButtons
                                )
                            }
                            MultimediaContentType.VIDEO -> {
                                val isWeek6Session2 = uiState.sessionId == 6L && uiState.subSessionId == 2L
                                VideoContent(
                                    contentAssetId = currentOption.contentAssetId ?: com.colma.colmaApp.R.raw.exercise,
                                    localFilePath = currentOption.localFilePath,
                                    isAutoZoomed = isWeek6Session2,
                                    navigationButtons = navButtons
                                )
                            }
                            MultimediaContentType.AUDIO -> {
                                AudioContent(
                                    audioResId = currentOption.contentAssetId ?: com.colma.colmaApp.R.raw.audio_explicativo,
                                    localFilePath = currentOption.localFilePath,
                                    navigationButtons = navButtons
                                )
                            }
                            MultimediaContentType.METAS -> {
                                currentOption.metasId?.let {
                                    MetasContent(
                                        metasId = it,
                                        metasOptions = currentOption.metasOptions,
                                        currentWeekId = uiState.sessionId.toInt(),
                                        currentSessionId = uiState.subSessionId.toInt(),
                                        onFinish = { onEvent(MultimediaEvent.Finish) },
                                        navigationButtons = navButtons
                                    )
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
                                        onFinish = { onEvent(MultimediaEvent.GoToNext) },
                                        navigationButtons = navButtons
                                    )
                                } ?: Text("Quiz not found or questions missing.")
                            }
                            else -> {
                                Text("Content for ${currentOption.type.name}: ${currentOption.title}")
                            }
                        }
                    }
                }
                else -> {
                    Text("No content available.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
