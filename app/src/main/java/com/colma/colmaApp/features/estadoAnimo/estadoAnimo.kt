package com.colma.colmaApp.features.estadoAnimo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import com.colma.colmaApp.R

class EstadoAnimoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            ColmaAppTheme {
                EstadoAnimoScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoAnimoScreen(
    onBack: () -> Unit = {},
    onSubmitResults: (Map<String, Int>) -> Unit = {} // optional callback for actual submit
) {
    val allQuestions = listOf(
        "Poco interés o placer en hacer las cosas",
        "Sentirse deprimido, abatido o sin esperanza",
        "Problemas para quedarse o permanecer dormido",
        "Sentirse cansado o con poca energía"
    )

    val optionLabels = listOf(
        "Nada", "Varios dias", "la mayoria de dias", "Casi todos los dias"
    )

    // -1 = unanswered, 0..3 answers
    val answersState = rememberSaveable { mutableStateOf(List(allQuestions.size) { -1 }) }

    // derived values
    val firstTwoAnswered by remember {
        derivedStateOf {
            val a0 = answersState.value.getOrNull(0) ?: -1
            val a1 = answersState.value.getOrNull(1) ?: -1
            a0 != -1 && a1 != -1
        }
    }

    val firstTwoSum by remember {
        derivedStateOf {
            val a0 = answersState.value.getOrNull(0) ?: 0
            val a1 = answersState.value.getOrNull(1) ?: 0
            a0 + a1
        }
    }

    // show rest only after pressing Enviar (and sum >= 3)
    var showRest by rememberSaveable { mutableStateOf(false) }

    // lock first two answers after pressing Enviar
    var lockedFirstTwo by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun setAnswer(index: Int, value: Int) {
        val tmp = answersState.value.toMutableList()
        tmp[index] = value
        answersState.value = tmp.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Estado de ánimo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.principal),
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Contesta las siguientes preguntas seleccionando la opción que mejor te describe durante las últimas dos semanas:",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // first two questions — pass disabled based on lockedFirstTwo
                QuestionRow(
                    question = allQuestions[0],
                    optionLabels = optionLabels,
                    selected = answersState.value[0],
                    onSelect = { sel -> if (!lockedFirstTwo) setAnswer(0, sel) },
                    disabled = lockedFirstTwo
                )

                QuestionRow(
                    question = allQuestions[1],
                    optionLabels = optionLabels,
                    selected = answersState.value[1],
                    onSelect = { sel -> if (!lockedFirstTwo) setAnswer(1, sel) },
                    disabled = lockedFirstTwo
                )

                if (!showRest && firstTwoAnswered && !lockedFirstTwo) {
                    Text(
                        text = "Presiona Enviar para continuar. Si la suma de las dos primeras respuestas es ≥ 3 aparecerán más preguntas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (showRest) {
                    for (i in 2 until allQuestions.size) {
                        QuestionRow(
                            question = allQuestions[i],
                            optionLabels = optionLabels,
                            selected = answersState.value[i],
                            onSelect = { sel -> setAnswer(i, sel) },
                            disabled = false // extra questions remain editable
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Lock first two when user taps Enviar for the first time
                            if (!lockedFirstTwo) lockedFirstTwo = true

                            if (!showRest) {
                                // validate first two answered
                                if (!firstTwoAnswered) {
                                    // unlock if you want to allow changes after error — keep lockedFirstTwo = false
                                    lockedFirstTwo = false
                                    snackbarHostState.showSnackbar("Por favor responda las dos primeras preguntas")
                                    return@launch
                                }

                                if (firstTwoSum >= 3) {
                                    // reveal rest (do not submit yet)
                                    showRest = true
                                    snackbarHostState.showSnackbar("Contesta las preguntas adicionales")
                                    return@launch
                                } else {
                                    // submit only first two answers
                                    val results = mapOf(
                                        allQuestions[0] to answersState.value[0],
                                        allQuestions[1] to answersState.value[1]
                                    )
                                    onSubmitResults(results)
                                    snackbarHostState.showSnackbar("Respuestas guardadas")
                                    return@launch
                                }
                            } else {
                                // rest already visible: validate all visible questions
                                val visibleCount = allQuestions.size
                                val unanswered = answersState.value.take(visibleCount).count { it == -1 }
                                if (unanswered > 0) {
                                    snackbarHostState.showSnackbar("Por favor responda todas las preguntas")
                                    return@launch
                                }
                                val results = allQuestions.mapIndexed { idx, q -> q to answersState.value[idx] }.toMap()
                                onSubmitResults(results)
                                snackbarHostState.showSnackbar("Respuestas guardadas")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage ?: e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.principal), // 🔹 background color (blue)
                    contentColor = Color.White          // 🔹 text/icon color
                )

            ) {
                Text(text = "Enviar")
            }

            // optional: provide a "Modificar" button that unlocks first two (if you want)
            if (lockedFirstTwo) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        // Let user change first two again (if desired)
                        lockedFirstTwo = false
                        // optionally hide rest and clear answers
                        // showRest = false
                        // val tmp = answersState.value.toMutableList()
                        // for (i in 2 until tmp.size) tmp[i] = -1
                        // answersState.value = tmp.toList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modificar respuestas iniciales")
                }
            }
        }
    }
}

/** Updated QuestionRow to accept disabled flag and dim when disabled */
@Composable
private fun QuestionRow(
    question: String,
    optionLabels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    disabled: Boolean = false
) {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp
    val small = screenWidthDp < 360

    // visual alpha for disabled state
    val contentAlpha = if (disabled) 0.5f else 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).alpha(contentAlpha)) {
            Text(
                text = question,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = if (small) 13.sp else 15.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                optionLabels.forEachIndexed { idx, label ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 64.dp)
                            .padding(horizontal = 6.dp)
                            .clickable(enabled = !disabled) { onSelect(idx) }
                            .semantics { this.selected = (selected == idx) },
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadioButton(
                            selected = selected == idx,
                            onClick = { if (!disabled) onSelect(idx) },
                            enabled = !disabled,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorResource(R.color.light_principal),   // 🔹 your custom selected color
                                unselectedColor = Color.Gray,        // 🔸 color when not selected
                                disabledSelectedColor = Color.LightGray,
                                disabledUnselectedColor = Color(0xFFB0BEC5)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = if (small) 11.sp else 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}