package com.colma.colmaApp.features.multimediaScreens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog
import com.colma.colmaApp.R
import com.colma.colmaApp.features.multimediaScreens.navigateToNextOrFinish

data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
)

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent.getLongExtra("sessionId", -1L)
        val sessionTitle = intent.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent.getLongExtra("subSessionId", -1L)
        val subSessionTitle = intent.getStringExtra("subSessionTitle") ?: ""
        val treatmentTypesString = intent.getStringExtra("treatmentTypes") ?: ""
        val currentMultimediaIndex = intent.getIntExtra("currentMultimediaIndex", -1)
        val quizId = intent.getStringExtra("quizId") ?: "hta" // Default to HTA

        setContent {
            ColmaAppTheme {
                QuizScreen(
                    quizId = quizId,
                    onBack = { finish() },
                    onFinish = {
                        navigateToNextOrFinish(
                            activity = this@QuizActivity,
                            sessionId = sessionId,
                            sessionTitle = sessionTitle,
                            subSessionId = subSessionId,
                            subSessionTitle = subSessionTitle,
                            treatmentTypesString = treatmentTypesString,
                            currentMultimediaIndex = currentMultimediaIndex
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(quizId: String, onBack: () -> Unit, onFinish: () -> Unit) {
    val quizQuestions = remember(quizId) {
        when (quizId) {
            "diabetes" -> getDiabetesQuizQuestions()
            else -> getHtaQuizQuestions()
        }
    }
    val title = remember(quizId) {
        when (quizId) {
            "diabetes" -> "Quiz Diabetes"
            else -> "Quiz Hipertensión"
        }
    }

    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val allQuestionsAnswered = userAnswers.size == quizQuestions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = colorResource(R.color.white)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.principal)),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )
        },
        containerColor = colorResource(R.color.background)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), // Space for the button
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(quizQuestions) { index, question ->
                    QuestionCard(
                        questionIndex = index,
                        question = question,
                        selectedAnswerIndex = userAnswers[index],
                        isSubmitted = quizSubmitted,
                        onAnswerSelected = { answerIndex ->
                            if (!quizSubmitted) {
                                userAnswers[index] = answerIndex
                            }
                        }
                    )
                }
            }

            // --- Confirmation Dialog ---
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White) // Green background, white text
                        ) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showConfirmationDialog = false },
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.Red, contentColor = Color.White) // Red background, white text
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // --- Bottom Button ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        if (quizSubmitted) {
                            onFinish()
                        } else {
                            showConfirmationDialog = true
                        }
                    },
                    enabled = if (quizSubmitted) true else allQuestionsAnswered,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal))
                ) {
                    Text(if (quizSubmitted) "Finalizar" else "Confirmar")
                }
            }
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
                    text = "Pregunta ${questionIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isSubmitted) {
                    val isCorrect = selectedAnswerIndex == question.correctAnswerIndex
                    val text = if (isCorrect) "Correcto" else "Incorrecto"
                    val bgColor = if (isCorrect) Color(0xFF008000) else Color.Red
                    Text(
                        text = text,
                        color = Color.White, // White text for contrast
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(bgColor, shape = MaterialTheme.shapes.small) // Rounded background
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = question.questionText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitted) {
                // --- Show Results ---
                val userAnswerText = selectedAnswerIndex?.let { question.options.getOrNull(it) } ?: "No respondida"
                val correctAnswerText = question.options[question.correctAnswerIndex]
                Text("Tu respuesta: $userAnswerText")
                if (selectedAnswerIndex != question.correctAnswerIndex) {
                    Text("Respuesta correcta: $correctAnswerText", color = Color(0xFF008000))
                }
            } else {
                // --- Show Answer Selector ---
                CardThatOpensSelectionDialog(
                    title = "Selecciona tu respuesta",
                    description = selectedAnswerIndex?.let { question.options.getOrNull(it) } ?: "Toca para responder",
                    items = question.options,
                    onItemSelected = { selectedOption ->
                        onAnswerSelected(question.options.indexOf(selectedOption))
                    }
                )
            }
        }
    }
}


fun getHtaQuizQuestions(): List<QuizQuestion> {
    return listOf(
        QuizQuestion(
            questionText = "Tomás se siente muy bien y por eso decide dar un paseo por el parque. Mientras camina, ve un puesto sanitario con personal de salud, quienes le ofrecen tomarle la presion arterial. Le informan que la medicion fue: 150/90 mmHG ¿Qué debe hacer Tomás?",
            options = listOf(
                "Nada, él se siente muy bien y sólo estaba paseando",
                "Programar una cita y consultar con su médico para hablar su presion arterial"
            ),
            correctAnswerIndex = 1
        ),
        QuizQuestion(
            questionText = "¿Qué quiere decir que la presión alta es 'silenciosa'?",
            options = listOf(
                "Que no se habla lo suficiente de ella en los medios de comunicacion.",
                "Que los aparatos que la miden no hacen ruido",
                "Que la presion puede estar alta y no generar síntomas."
            ),
            correctAnswerIndex = 2
        ),
        QuizQuestion(
            questionText = "La hipertension es una condición crónica, y ¿esto que quiere decir?",
            options = listOf(
                "Que nos acompañará toda la vida y no hay nada que podamos hacer al respecto",
                "Que nos acompañará toda la vida pero podemos hacer muchas cosas para controlarla"
            ),
            correctAnswerIndex = 1
        ),
        QuizQuestion(
            questionText = "Tengo presión alta pero me siento muy bien. ¿Qué hago con la medicacion que me recetó el médico?",
            options = listOf(
                "No tomarla más porque me siento bien",
                "Dejo de tomarlas hasta hablar de nuevo con mi médico",
                "Necesito continuar tomandola todos los días"
            ),
            correctAnswerIndex = 2
        )
    )
}

fun getDiabetesQuizQuestions(): List<QuizQuestion> {
    return listOf(
        QuizQuestion(
            questionText = "La insulina es como una llave que permite que la glucosa entre en las células y nos dé energía",
            options = listOf(
                "Verdadero",
                "Falso"
            ),
            correctAnswerIndex = 0
        ),
        QuizQuestion(
            questionText = "Tener mucha sed, orinar mucho y tener mucha hambre pueden ser señales de que...",
            options = listOf(
                "La glucosa está baja y necesitas comer algo",
                "La glucosa está alta y necesita controlarse",
                "La glucosa está equilibrada"
            ),
            correctAnswerIndex = 1
        ),
        QuizQuestion(
            questionText = "La mejor forma de saber cómo está mi glucosa es...",
            options = listOf(
                "Midiéndola con un glucómetro o un análisis de sangre",
                "Solo reconociendo las señales de mi cuerpo",
                "No es necesario saberlo"
            ),
            correctAnswerIndex = 0
        )
    )
}
