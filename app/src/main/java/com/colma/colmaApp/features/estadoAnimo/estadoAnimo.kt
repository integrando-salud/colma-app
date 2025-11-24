package com.colma.colmaApp.features.estadoAnimo

import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import com.colma.colmaApp.R
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class EstadoAnimoActivity : ComponentActivity() {
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
                // pass an onBack that returns RESULT_OK so caller can detect success
                EstadoAnimoScreen(
                    onBack = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onSubmitResults = { /* optional callback */ }
                )
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

    val context = LocalContext.current
    val dbHelper = remember { DBhelper(context) }

    val allQuestions = listOf(
        "Poco interés o placer en hacer las cosas",
        "Sentirse deprimido, abatido o sin esperanza",
        "Problemas para quedarse o permanecer dormido",
        "Sentirse cansado o con poca energía",
        "Poco apetito o comer mucho?",
        "Te sientes mal sobre ti mismo -- o que eres un fracaso o has decepcionado a ti mismo o tu familia",
        "Problemas para concentrarte en cosas, como leer el periodico o ver la television",
        "¿Se mueve o habla tan despacio que otras personas podrían haberlo notado? ¿O está tan inquieto que se ha estado moviendo mucho más de lo habitual?",
        "¿Pensamientos de que estarías mejor muerto o pensamientos de hacerte daño de alguna manera?"
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

    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }

// NEW ISO formatter used for DB timestamps (YYYY-MM-DD HH:mm:ss)
    val isoFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    var estadoDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) } // epoch millis
    val estadoCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = estadoDateTime } }

    var confirmSubmitOpen by rememberSaveable { mutableStateOf(false) }

// pending payload to persist when user confirms
    var pendingFullMap by remember { mutableStateOf<Map<Int, Int?>?>(null) }
    var pendingIdPaciente by remember { mutableStateOf<Int?>(null) }
    var pendingIsPartialSubmit by remember { mutableStateOf(false) }

    var pendingDraftRowId by rememberSaveable { mutableStateOf<Long?>(null) }


    fun setAnswer(index: Int, value: Int) {
        val tmp = answersState.value.toMutableList()
        tmp[index] = value
        answersState.value = tmp.toList()

        // If user answered one of the first two, immediately save partial
        if (index == 0 || index == 1) {
            val p1 = answersState.value.getOrNull(0)
            val p2 = answersState.value.getOrNull(1)
            // only persist when at least one of them has non-negative value
            if ((p1 ?: -1) >= 0 || (p2 ?: -1) >= 0) {
                // launch IO save
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val idPaciente = 1
                            val map = mutableMapOf<Int, Int?>()
                            if (p1 != null && p1 >= 0) map[1] = p1
                            if (p2 != null && p2 >= 0) map[2] = p2
                            if (map.isNotEmpty()) {
                                if (pendingDraftRowId != null) {
                                    dbHelper.updateEstadoAnimoRow(pendingDraftRowId!!, map, fechaEdicion = isoFormatter.format(java.util.Date(estadoDateTime)))
                                } else {
                                    // fallback: create one (very unlikely because LaunchedEffect creates it)
                                    val newId = dbHelper.insertEstadoAnimo(idPaciente, map, fechaRegistro = null, fechaEdicion = isoFormatter.format(java.util.Date(estadoDateTime)))
                                    if (newId > 0L) pendingDraftRowId = newId
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // handle/log error as needed
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {

        if (pendingDraftRowId == null) {
            try {
                withContext(Dispatchers.IO) {
                    val idPaciente = 1
                    val fechaEdicionIso = isoFormatter.format(java.util.Date(estadoDateTime))
                    val newId = dbHelper.insertEstadoAnimo(idPaciente, emptyMap(), fechaRegistro = null, fechaEdicion = fechaEdicionIso)
                    pendingDraftRowId = if (newId > 0L) newId else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
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
                .verticalScroll(rememberScrollState()) // Moved verticalScroll here
        ) {
            Text(
                text = "Contesta las siguientes preguntas seleccionando la opción que mejor te describe:",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedTextField(
                    value = dateFormatter.format(java.util.Date(estadoDateTime)),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val c = estadoCalendar
                            // ensure calendar reflects current state
                            c.timeInMillis = estadoDateTime
                            val dpd = android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    c.set(java.util.Calendar.YEAR, y)
                                    c.set(java.util.Calendar.MONTH, m)
                                    c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                    estadoDateTime = c.timeInMillis
                                },
                                c.get(java.util.Calendar.YEAR),
                                c.get(java.util.Calendar.MONTH),
                                c.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            dpd.show()
                        }
                        .padding(end = 8.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val c = estadoCalendar
                            c.timeInMillis = estadoDateTime
                            val dpd = android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    c.set(java.util.Calendar.YEAR, y)
                                    c.set(java.util.Calendar.MONTH, m)
                                    c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                    estadoDateTime = c.timeInMillis
                                },
                                c.get(java.util.Calendar.YEAR),
                                c.get(java.util.Calendar.MONTH),
                                c.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            dpd.show()
                        }) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Fecha")
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Time button - shows only HH:mm and opens TimePickerDialog
                OutlinedButton(onClick = {
                    val c = estadoCalendar
                    c.timeInMillis = estadoDateTime
                    val tpd = android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            c.set(java.util.Calendar.HOUR_OF_DAY, hour)
                            c.set(java.util.Calendar.MINUTE, minute)
                            estadoDateTime = c.timeInMillis
                        },
                        c.get(java.util.Calendar.HOUR_OF_DAY),
                        c.get(java.util.Calendar.MINUTE),
                        true
                    )
                    tpd.show()
                }) {
                    Text(text = timeFormatterOnly.format(java.util.Date(estadoDateTime)))
                }
            }

            Spacer(Modifier.height(12.dp))
            // ---------------------------------------------------------

            Column(
                modifier = Modifier, // Removed .weight(1f)
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

                            if (!lockedFirstTwo) lockedFirstTwo = true

                            if (!showRest) {
                                // validate first two answered
                                if (!firstTwoAnswered) {
                                    // unlock to allow correction
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
                                    // submit only first two answers (persist as a full row where remaining preguntas are NULL)
                                    val fullMap = (1..9).associateWith { idx ->
                                        val v = answersState.value.getOrNull(idx - 1)
                                        if (v != null && v >= 0) v else null
                                    }

                                    // prepare pending submission and open confirmation dialog
                                    pendingFullMap = fullMap
                                    pendingIdPaciente = 1
                                    pendingIsPartialSubmit = true
                                    confirmSubmitOpen = true
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

                                // Build full map 1..9, unanswered -> null
                                val fullMap = (1..9).associateWith { idx ->
                                    val v = answersState.value.getOrNull(idx - 1)
                                    if (v != null && v >= 0) v else null
                                }

                                // prepare pending submission and open confirmation dialog
                                pendingFullMap = fullMap
                                pendingIdPaciente = 1
                                pendingIsPartialSubmit = false
                                confirmSubmitOpen = true
                                return@launch
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage ?: e.message}")
                        }
                    }
                },
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

            if (confirmSubmitOpen) {
                val summaryCount = pendingFullMap?.count { it.value != null } ?: 0
                val dateLabel = dateFormatter.format(java.util.Date(estadoDateTime))
                val timeLabel = timeFormatterOnly.format(java.util.Date(estadoDateTime))
                AlertDialog(
                    onDismissRequest = { confirmSubmitOpen = false },
                    title = { Text("Confirmar envío") },
                    text = {
                        Column {
                            Text("Vas a guardar $summaryCount respuestas.")
                            Spacer(Modifier.height(8.dp))
                            Text("Fecha de guardado seleccionada: $dateLabel $timeLabel")
                            Spacer(Modifier.height(8.dp))
                            Text("¿Deseas confirmar el guardado?")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {

                            confirmSubmitOpen = false
                            val toSave = pendingFullMap
                            val idPac = pendingIdPaciente ?: 1

                            pendingFullMap = null
                            pendingIdPaciente = null


                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val fechaEdicionIso = isoFormatter.format(java.util.Date(estadoDateTime))
                                        val idPac = pendingIdPaciente ?: 1

                                        if (pendingDraftRowId != null) {
                                            dbHelper.updateEstadoAnimoRow(pendingDraftRowId!!, toSave ?: emptyMap(), fechaEdicion = fechaEdicionIso)
                                            pendingDraftRowId = null
                                        } else {

                                            dbHelper.insertEstadoAnimo(idPac, toSave ?: emptyMap(), fechaRegistro = null, fechaEdicion = fechaEdicionIso)
                                        }
                                    }


                                    val results = allQuestions.mapIndexed { idx, q -> q to (toSave?.get(idx + 1) ?: -1) }.toMap()
                                    onSubmitResults(results)

                                    // show snackbar in a separate coroutine so it does not block navigation
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Respuestas guardadas")
                                    }

                                    // navigate back (calls the onBack callback passed from the Activity)
                                    onBack()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // show error snackbar (non-blocking)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error guardando: ${e.localizedMessage ?: e.message}")
                                    }
                                }
                            }
                        }) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmSubmitOpen = false }) {
                            Text("No")
                        }
                    }
                )
            }

            // optional: provide a "Modificar" button that unlocks first two (if you want)
            if (lockedFirstTwo) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        // Let user change first two again (if desired)
                        lockedFirstTwo = false
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
