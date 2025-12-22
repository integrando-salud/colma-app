package com.colma.colmaApp.features.multimediaScreens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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

data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
)

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quizId = intent.getStringExtra("quizId") ?: "hta" // Default to HTA
        setContent {
            ColmaAppTheme {
                QuizScreen(
                    quizId = quizId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(quizId: String, onBack: () -> Unit) {
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

    var currentQuestionIndex by remember { mutableStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }

    val currentQuestion = quizQuestions.getOrNull(currentQuestionIndex)

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (currentQuestion != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentQuestion.questionText,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    key(currentQuestionIndex) {
                        CardThatOpensSelectionDialog(
                            title = "Selecciona tu respuesta",
                            items = currentQuestion.options,
                            onItemSelected = { selectedOption ->
                                val selectedIndex = currentQuestion.options.indexOf(selectedOption)
                                userAnswers[currentQuestionIndex] = selectedIndex
                                currentQuestionIndex++
                            }
                        )
                    }
                }
            } else {
                // Quiz finished - Summary Screen
                var score = 0
                quizQuestions.forEachIndexed { index, question ->
                    if (userAnswers[index] == question.correctAnswerIndex) {
                        score++
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¡Quiz completado!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tu puntaje: $score de ${quizQuestions.size}", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(quizQuestions) { index, question ->
                            val userAnswerIndex = userAnswers[index]
                            val isCorrect = userAnswerIndex == question.correctAnswerIndex
                            val correctAnswerText = question.options[question.correctAnswerIndex]
                            val userAnswerText = userAnswerIndex?.let { question.options.getOrNull(it) } ?: "No respondida"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Pregunta ${index + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = if (isCorrect) "Correcto" else "Incorrecto",
                                            tint = if (isCorrect) Color(0xFF008000) else Color.Red
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = question.questionText, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tu respuesta: $userAnswerText")
                                    if (!isCorrect) {
                                        Text("Respuesta correcta: $correctAnswerText", color = Color(0xFF008000))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal))
                    ) {
                        Text("Finalizar")
                    }
                }
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