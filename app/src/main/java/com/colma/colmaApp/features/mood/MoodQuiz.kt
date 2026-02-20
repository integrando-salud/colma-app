package com.colma.colmaApp.features.mood

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.mood.data.MoodRepositoryImpl
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizEvent
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizUiState
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizViewModel
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizViewModelFactory
import com.colma.colmaApp.ui.theme.ColmaAppTheme

class MoodQuizActivity : ComponentActivity() {

    private val viewModel: MoodQuizViewModel by viewModels {
        MoodQuizViewModelFactory(MoodRepositoryImpl(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.sideEffects.collect { effect ->
                        when (effect) {
                            is MoodQuizViewModel.SideEffect.ShowToast -> {
                                snackbarHostState.showSnackbar(effect.message)
                            }
                            MoodQuizViewModel.SideEffect.NavigateBack -> finish()
                            MoodQuizViewModel.SideEffect.NavigateBackWithResult -> {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
                
                MoodQuizScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodQuizScreen(
    uiState: MoodQuizUiState,
    onEvent: (MoodQuizEvent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Estado de ánimo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MoodQuizEvent.BackPressed) }) {
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
        BackHandler(onBack = { onEvent(MoodQuizEvent.BackPressed) })
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Contesta las siguientes preguntas seleccionando la opción que mejor te describe:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuestionRow(
                        question = uiState.questions.getOrElse(0) { "" },
                        optionLabels = uiState.optionLabels,
                        selected = uiState.answers.getOrElse(0) { -1 },
                        onSelect = { sel -> onEvent(MoodQuizEvent.AnswerQuestion(0, sel)) },
                        disabled = uiState.firstTwoQuestionsLocked
                    )

                    QuestionRow(
                        question = uiState.questions.getOrElse(1) { "" },
                        optionLabels = uiState.optionLabels,
                        selected = uiState.answers.getOrElse(1) { -1 },
                        onSelect = { sel -> onEvent(MoodQuizEvent.AnswerQuestion(1, sel)) },
                        disabled = uiState.firstTwoQuestionsLocked
                    )

                    if (uiState.showRestOfQuestions) {
                        for (i in 2 until uiState.questions.size) {
                            QuestionRow(
                                question = uiState.questions[i],
                                optionLabels = uiState.optionLabels,
                                selected = uiState.answers[i],
                                onSelect = { sel -> onEvent(MoodQuizEvent.AnswerQuestion(i, sel)) },
                                disabled = false
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { onEvent(MoodQuizEvent.SaveClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Guardar")
                }

                if (uiState.showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { onEvent(MoodQuizEvent.DismissConfirmDialog) },
                        title = { Text("Confirmar") },
                        text = {
                            Text("¿Desea continuar?")
                        },
                        confirmButton = {
                            Button(
                                onClick = { onEvent(MoodQuizEvent.ConfirmSubmit) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50), // Green
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Continuar")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { onEvent(MoodQuizEvent.DismissConfirmDialog) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336), // Red
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                if (uiState.showMoreQuestionsDialog) {
                    AlertDialog(
                        onDismissRequest = { onEvent(MoodQuizEvent.DismissMoreQuestionsDialog) },
                        title = { Text("Confirmar") },
                        text = {
                            Text("¿Desea continuar?")
                        },
                        confirmButton = {
                            Button(
                                onClick = { onEvent(MoodQuizEvent.ConfirmMoreQuestions) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50), // Green
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Continuar")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { onEvent(MoodQuizEvent.DismissMoreQuestionsDialog) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336), // Red
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
            if (scrollState.canScrollForward) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Deslizar para ver más",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .background(
                            color = colorResource(id = R.color.principal),
                            shape = CircleShape
                        )
                        .size(48.dp)
                )
            }
        }
    }
}

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

    val contentAlpha = if (disabled) 0.5f else 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.white)),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).alpha(contentAlpha)) {
            Text(
                text = question,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                fontSize = if (small) 13.sp else 15.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Top
                ) {
                    optionLabels.take(2).forEachIndexed { idx, label ->
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
                                    selectedColor = colorResource(R.color.light_principal),
                                    unselectedColor = Color.Gray,
                                    disabledSelectedColor = Color.LightGray,
                                    disabledUnselectedColor = Color(0xFFB0BEC5)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = if (small) 13.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Top
                ) {
                    optionLabels.drop(2).forEachIndexed { idx, label ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 64.dp)
                                .padding(horizontal = 6.dp)
                                .clickable(enabled = !disabled) { onSelect(idx + 2) }
                                .semantics { this.selected = (selected == idx + 2) },
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RadioButton(
                                selected = selected == idx + 2,
                                onClick = { if (!disabled) onSelect(idx + 2) },
                                enabled = !disabled,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colorResource(R.color.light_principal),
                                    unselectedColor = Color.Gray,
                                    disabledSelectedColor = Color.LightGray,
                                    disabledUnselectedColor = Color(0xFFB0BEC5)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = if (small) 13.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}