package com.colma.colmaApp.features.multimedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.content.model.QuizQuestion
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.session.SessionManager
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    quizQuestions: List<QuizQuestion>,
    quizTitle: String,
    sessionId: Long,
    subSessionId: Long,
    treatmentTypes: List<TreatmentType>,
    multimediaUniqueKey: String,
    tratamientoElementoId: Int? = null,
    onQuizSubmit: (quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>) -> Unit,
    onFinish: () -> Unit,
    navigationButtons: @Composable () -> Unit = {}
) {
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val allQuestionsAnswered = userAnswers.size == quizQuestions.size
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
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        quizQuestions.forEachIndexed { index, question ->
                            QuestionCard(
                                questionIndex = index,
                                question = question,
                                selectedAnswerIndex = userAnswers[index],
                                isSubmitted = quizSubmitted,
                                onAnswerSelected = { if (!quizSubmitted) userAnswers[index] = it }
                            )
                        }
                    }
                },
                footer = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!quizSubmitted) {
                            Button(
                                onClick = { showConfirmationDialog = true },
                                enabled = allQuestionsAnswered,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.contraste))
                            ) {
                                Text("Guardar", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        navigationButtons()
                    }
                }
            )
        }


        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                containerColor = Color.White,
                title = { Text("Confirmar respuestas") },
                text = { Text("¿Estás seguro de que quieres confirmar tus respuestas? No podrás cambiarlas después.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmationDialog = false
                            quizSubmitted = true
                            val totalQuestions = quizQuestions.size
                            var correctAnswers = 0
                            val questionResponses = mutableListOf<QuestionResponseRecord>()
                            quizQuestions.forEachIndexed { index, question ->
                                val userAnswerIndex = userAnswers[index]
                                val isCorrect = userAnswerIndex == question.correctAnswerIndex
                                if (isCorrect) correctAnswers++
                                questionResponses.add(
                                    QuestionResponseRecord(
                                        id_quiz_response = 0,
                                        question_index = index,
                                        question_text = question.questionText,
                                        user_answer_index = userAnswerIndex,
                                        user_answer_text = userAnswerIndex?.let { question.options.getOrNull(it) },
                                        correct_answer_index = question.correctAnswerIndex,
                                        correct_answer_text = question.options.getOrNull(question.correctAnswerIndex),
                                        is_correct = if (isCorrect) 1 else 0,
                                        posibles_respuestas = Gson().toJson(question.options)
                                    )
                                )
                            }
                            val percentageCorrect = if (totalQuestions > 0) (correctAnswers.toDouble() / totalQuestions.toDouble()) * 100 else 0.0
                            val quizTreatmentId = treatmentTypes.firstOrNull()?.name?.lowercase() ?: "unknown"
                            val quizResponseRecord = QuizResponseRecord(
                                id_quiz_response = 0,
                                id_usuario = SessionManager.getUserId() ?: 1,
                                quiz_type = quizTreatmentId,
                                week_id = sessionId.toInt(),
                                session_id = subSessionId.toInt(),
                                quiz_title = quizTitle,
                                fecha_registro = null,
                                fecha_sincronizacion = null,
                                total_questions = totalQuestions,
                                correct_answers = correctAnswers,
                                percentage_correct = percentageCorrect,
                                multimedia_unique_key = multimediaUniqueKey,
                                tratamiento_elemento_id = tratamientoElementoId
                            )
                            onQuizSubmit(quizResponseRecord, questionResponses)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorResource(R.color.rojo_boton),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun QuestionCard(
    questionIndex: Int,
    question: QuizQuestion,
    selectedAnswerIndex: Int?,
    isSubmitted: Boolean,
    onAnswerSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (questionIndex == 0) "Pregunta del día" else "Pregunta ${questionIndex + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    modifier = Modifier.weight(1f)
                )
                if (isSubmitted) {
                    val isCorrect = selectedAnswerIndex == question.correctAnswerIndex
                    val text = if (isCorrect) "Correcto" else "Incorrecto"
                    val bgColor = if (isCorrect) Color(0xFF008000) else Color.Red
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(bgColor, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = question.questionText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            if (isSubmitted) {
                val userAnswerText = selectedAnswerIndex?.let { question.options.getOrNull(it) } ?: "No respondida"
                val correctAnswerText = question.options[question.correctAnswerIndex]
                Text("Tu respuesta: $userAnswerText")
                if (selectedAnswerIndex != question.correctAnswerIndex) {
                    Text("Respuesta correcta: $correctAnswerText", color = Color(0xFF008000))
                }
            } else {
                question.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswerIndex == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnswerSelected(index) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onAnswerSelected(index) },
                            colors = RadioButtonDefaults.colors(selectedColor = colorResource(R.color.principal))
                        )
                        Text(text = option, modifier = Modifier.padding(start = 8.dp), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
