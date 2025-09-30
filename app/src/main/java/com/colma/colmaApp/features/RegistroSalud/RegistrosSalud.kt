package com.colma.colmaApp.features.RegistroSalud

import android.content.Context
import androidx.compose.ui.geometry.Rect
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple data class for history items
data class HealthEntry(val label: String, val value: String, val timestamp: Long)

class RegistrosSalud : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColmaAppTheme {
                RegistrosSaludScreen(
                    onBack = { finish() },
                    onSave = { label, value ->
                        // persist if needed (ViewModel / DB)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosSaludScreen(
    onBack: () -> Unit = {},
    onSave: (label: String, value: String) -> Unit = { _, _ -> }
) {
    val signatureColor = Color(0xFFE84589)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val screenDensity = androidx.compose.ui.platform.LocalDensity.current.density

    val prefs = context.getSharedPreferences("colma_prefs", Context.MODE_PRIVATE)
    var showTutorial by rememberSaveable { mutableStateOf(!prefs.getBoolean("tutorial_shown", false)) }
    val STEP_HISTORY = 0
    val STEP_INPUTS = 1

// Sequence: change order here. Example shows INPUTS first, then HISTORY
    val tutorialSequence = remember { listOf(STEP_INPUTS, STEP_HISTORY) }

// index into the sequence (which actual step are we on?)
    var tutorialIndex by rememberSaveable { mutableStateOf(0) } // start at first item in tutorialSequence

// helper to get current step id (safe)
    val currentStepId: Int = if (tutorialIndex in tutorialSequence.indices) {
        tutorialSequence[tutorialIndex]
    } else {
        STEP_HISTORY
    }



    // capture bounds of the first card (Glucosa) so we can highlight it
    var historyCardBounds by remember { mutableStateOf<Rect?>(null) }
    var glucosaCardBounds by remember { mutableStateOf<Rect?>(null) }
    var presionCardBounds by remember { mutableStateOf<Rect?>(null) }
    var depresionCardBounds by remember { mutableStateOf<Rect?>(null) }

    var overlayBounds by remember { mutableStateOf<Rect?>(null) }
    var tutorialStep by rememberSaveable { mutableStateOf(0) }
    // Create a single DBhelper instance for this screen
    val dbHelper = remember { DBhelper(context.applicationContext) }

    // dialog state
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var dialogLabel by rememberSaveable { mutableStateOf("") }

    // Inputs & errors
    var glucosaValue by rememberSaveable { mutableStateOf("") }
    var glucosaError by rememberSaveable { mutableStateOf<String?>(null) }

    var presionSys by rememberSaveable { mutableStateOf("") }
    var presionDia by rememberSaveable { mutableStateOf("") }
    var presionError by rememberSaveable { mutableStateOf<String?>(null) }

    var depresionValue by rememberSaveable { mutableStateOf("") }
    var depresionError by rememberSaveable { mutableStateOf<String?>(null) }

    // LAST SAVED values (displayed under inputs):
    var lastGlucosaValue by rememberSaveable { mutableStateOf("") }      // e.g. "120"
    var lastGlucosaTime by rememberSaveable { mutableStateOf("") }       // formatted time

    var lastPresionValue by rememberSaveable { mutableStateOf("") }      // e.g. "120/80"
    var lastPresionTime by rememberSaveable { mutableStateOf("") }       // formatted time

    // In-memory history (newest first). Use ViewModel+DB for persistence across restarts.
    val history = remember { mutableStateListOf<HealthEntry>() }

    val timeFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    fun formattedNow(): String = timeFormatter.format(Date())

    // Load latest values from DB once when screen appears
    LaunchedEffect(Unit) {
        try {
            // fetch latest glucosa
            val latestGlucosa = withContext(Dispatchers.IO) {
                try { dbHelper.getLatestGlucosa(null) } catch (e: Exception) { e.printStackTrace(); null }
            }
            latestGlucosa?.let { g ->
                lastGlucosaValue = g.valorMgdl?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
                // parse DB date/time into millis
                val gMillis = parseDateTimeToMillis(g.fechaRegistro, g.horaRegistro) ?: System.currentTimeMillis()
                lastGlucosaTime = formatTimestamp(gMillis, timeFormatter)
                // use DB timestamp for history
                history.add(0, HealthEntry("Glucosa", "${lastGlucosaValue} mg/dl", gMillis))
            }

            // fetch latest presion
            val latestPresion = withContext(Dispatchers.IO) {
                try { dbHelper.getLatestPresion(null) } catch (e: Exception) { e.printStackTrace(); null }
            }
            latestPresion?.let { p ->
                fun fmtPressure(v: Double?): String {
                    return when {
                        v == null -> ""
                        v % 1.0 == 0.0 -> v.toInt().toString()
                        else -> String.format(Locale.getDefault(), "%.1f", v)
                    }
                }
                val syst = fmtPressure(p.sistolica)
                val dia = fmtPressure(p.diastolica)
                lastPresionValue = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia" else ""
                val pMillis = parseDateTimeToMillis(p.fechaRegistro, p.horaRegistro) ?: System.currentTimeMillis()
                lastPresionTime = formatTimestamp(pMillis, timeFormatter)
                history.add(0, HealthEntry("Presion", "${lastPresionValue} mmHg", pMillis))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // fail silently
        }
    }

    fun openDialogFor(label: String) {
        dialogLabel = label
        when (label) {
            "Glucosa" -> {
                glucosaValue = ""
                glucosaError = null
            }
            "Presion" -> {
                presionSys = ""
                presionDia = ""
                presionError = null
            }
            "Depresion" -> {
                depresionValue = ""
                depresionError = null
            }
        }
        dialogOpen = true
    }

    fun closeDialog() {
        dialogOpen = false
        dialogLabel = ""
    }

    // Save handler - unchanged behavior (Glucosa/Presion insert already implemented)
    fun handleSave() {
        when (dialogLabel) {
            "Glucosa" -> {
                if (glucosaValue.isBlank()) {
                    glucosaError = "Por favor ingrese un valor"
                    return
                }
                val number = glucosaValue.replace(',', '.').toDoubleOrNull()
                if (number == null) {
                    glucosaError = "Ingrese un número válido"
                    return
                }

                // Perform DB insert on IO dispatcher
                coroutineScope.launch {
                    try {
                        val rowId = withContext(Dispatchers.IO) {
                            dbHelper.insertGlucosa(valorMgdl = number, idUsuario = null)
                        }

                        if (rowId > 0L) {
                            lastGlucosaValue = glucosaValue
                            lastGlucosaTime = formattedNow()
                            history.add(0, HealthEntry("Glucosa", "${glucosaValue} mg/dl", System.currentTimeMillis()))
                            onSave("Glucosa", glucosaValue)
                            Toast.makeText(context, "Guardado: Glucosa = $glucosaValue mg/dl", Toast.LENGTH_SHORT).show()
                            closeDialog()
                        } else {
                            Toast.makeText(context, "Error guardando en la base de datos", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            "Presion" -> {
                if (presionSys.isBlank() || presionDia.isBlank()) {
                    presionError = "Ingrese ambos valores"
                    return
                }
                val sys = presionSys.replace(',', '.').toDoubleOrNull()
                val dia = presionDia.replace(',', '.').toDoubleOrNull()
                if (sys == null || dia == null) {
                    presionError = "Ingrese números válidos"
                    return
                }

                coroutineScope.launch {
                    try {
                        val rowId = withContext(Dispatchers.IO) {
                            dbHelper.insertPresion(sistolica = sys, diastolica = dia, idUsuario = null)
                        }

                        if (rowId > 0L) {
                            lastPresionValue = "${sys.toInt()}/${dia.toInt()}"
                            lastPresionTime = formattedNow()
                            history.add(0, HealthEntry("Presion", "${sys.toInt()}/${dia.toInt()} mmHg", System.currentTimeMillis()))
                            onSave("Presion", "${sys.toInt()}/${dia.toInt()}")
                            Toast.makeText(context, "Guardado: Presión = ${sys.toInt()}/${dia.toInt()} mmHg", Toast.LENGTH_SHORT).show()
                            closeDialog()
                        } else {
                            Toast.makeText(context, "Error guardando Presión en la base de datos", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            "Depresion" -> {
                if (depresionValue.isBlank()) {
                    depresionError = "Por favor ingrese un comentario"
                    return
                }
                onSave("Depresion", depresionValue)
                history.add(0, HealthEntry("Depresion", depresionValue, System.currentTimeMillis()))
                Toast.makeText(context, "Guardado: Depresion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearHistory() { history.clear() }

    // UI (unchanged)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Registro de Salud", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = signatureColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // top area: the three quick cards
                HealthRecordCard(
                    label = "Glucosa",
                    onClick = { openDialogFor("Glucosa") },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        glucosaCardBounds = coords.boundsInWindow()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                HealthRecordCard(
                    label = "Presion",
                    onClick = { openDialogFor("Presion") },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        presionCardBounds = coords.boundsInWindow()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                HealthRecordCard(
                    label = "Depresion",
                    onClick = { openDialogFor("Depresion") },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        depresionCardBounds = coords.boundsInWindow()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // History card shows the recent entries (scrollable)
                val glucosaHistory = history.filter { it.label.equals("Glucosa", ignoreCase = true) }
                val presionHistory = history.filter { it.label.startsWith("Presion", ignoreCase = true) || it.label.startsWith("Presión", ignoreCase = true) }

                // 🔹 Show Glucosa History
                HistoryCard(
                    history = glucosaHistory,
                    onClear = { history.removeAll(glucosaHistory) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        historyCardBounds = coords.boundsInWindow() // highlight for tutorial
                    },
                    onClick = {
                        Toast.makeText(context, "Historial Glucosa completo", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 🔹 Show Presion History
                HistoryCard(
                    history = presionHistory,
                    onClear = { history.removeAll(presionHistory) },
                    modifier = Modifier,
                    onClick = {
                        Toast.makeText(context, "Historial Presión completo", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Dialogs with last-saved shown under inputs
            if (dialogOpen) {
                when (dialogLabel) {
                    "Glucosa" -> {
                        AlertDialog(
                            onDismissRequest = { closeDialog() },
                            title = { Text(text = "Ingresar Glucosa", color = Color.Black) },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = glucosaValue,
                                        onValueChange = {
                                            glucosaValue = it
                                            glucosaError = null
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Text(
                                                text = "mg/dl",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        },
                                        placeholder = { Text(text = "Ej: 120") },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    // show error or last-saved
                                    if (glucosaError != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = glucosaError ?: "", color = Color.Red, fontSize = 12.sp)
                                    } else if (lastGlucosaValue.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Último: $lastGlucosaValue mg/dl · $lastGlucosaTime",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { handleSave() }) { Text("Guardar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { closeDialog() }) { Text("Cancelar") }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp
                        )
                    }

                    "Presion" -> {
                        AlertDialog(
                            onDismissRequest = { closeDialog() },
                            title = { Text(text = "Ingresar Presión", color = Color.Black) },
                            text = {
                                Column {
                                    Text(text = "Sistólica", fontSize = 15.sp, color = Color.Black)
                                    OutlinedTextField(
                                        value = presionSys,
                                        onValueChange = {
                                            presionSys = it
                                            presionError = null
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp),
                                        placeholder = { Text(text = "Ej: 100") },
                                        trailingIcon = {
                                            Text(text = "mmHg", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                        },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(text = "Diastólica", fontSize = 15.sp, color = Color.Black)
                                    OutlinedTextField(
                                        value = presionDia,
                                        onValueChange = {
                                            presionDia = it
                                            presionError = null
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp),
                                        placeholder = { Text(text = "Ej: 80") },
                                        trailingIcon = {
                                            Text(text = "mmHg", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                        },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    if (presionError != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = presionError ?: "", color = Color.Red, fontSize = 12.sp)
                                    } else if (lastPresionValue.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Último: $lastPresionValue mmHg · $lastPresionTime",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { handleSave() }) { Text("Guardar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { closeDialog() }) { Text("Cancelar") }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp
                        )
                    }

                    "Depresion" -> {
                        AlertDialog(
                            onDismissRequest = { closeDialog() },
                            title = { Text(text = "Ingresar Depresión") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = depresionValue,
                                        onValueChange = {
                                            depresionValue = it
                                            depresionError = null
                                        },
                                        singleLine = false,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        placeholder = { Text(text = "Escribe tus notas...") }
                                    )

                                    depresionError?.let {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = it, color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { handleSave() }) { Text("Guardar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { closeDialog() }) { Text("Cancelar") }
                            }
                        )


                    }
                }
            }

            if (showTutorial) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords -> overlayBounds = coords.boundsInWindow() }
                        .pointerInput(
                            historyCardBounds,
                            glucosaCardBounds,
                            presionCardBounds,
                            depresionCardBounds,
                            overlayBounds,
                            tutorialIndex
                        ) {
                            detectTapGestures { tapOffset ->
                                // convert local tap offset to absolute (window) coordinates
                                val canvasLeft = overlayBounds?.left ?: 0f
                                val canvasTop = overlayBounds?.top ?: 0f
                                val absX = canvasLeft + tapOffset.x
                                val absY = canvasTop + tapOffset.y

                                fun finishTutorial() {
                                    prefs.edit().putBoolean("tutorial_shown", true).apply()
                                    showTutorial = false
                                }

                                // pick the target based on the current step id
                                val targetWindowBounds: Rect? = when (currentStepId) {
                                    STEP_HISTORY -> historyCardBounds
                                    STEP_INPUTS -> unionBounds(glucosaCardBounds, presionCardBounds, depresionCardBounds)
                                    else -> null
                                }

                                if (targetWindowBounds != null) {
                                    // use screenDensity read earlier in the composable scope
                                    val paddingPx = 12f * screenDensity
                                    val left = targetWindowBounds.left - paddingPx
                                    val top = targetWindowBounds.top - paddingPx
                                    val right = targetWindowBounds.right + paddingPx
                                    val bottom = targetWindowBounds.bottom + paddingPx

                                    if (absX >= left && absX <= right && absY >= top && absY <= bottom) {
                                        // tapped inside highlight -> advance sequence or finish if last step
                                        if (tutorialIndex < tutorialSequence.size - 1) {
                                            tutorialIndex += 1
                                        } else {
                                            finishTutorial()
                                        }
                                    } else {
                                        // tapped outside highlight -> dismiss tutorial
                                        finishTutorial()
                                    }
                                } else {
                                    // no bounds yet -> dismiss
                                    finishTutorial()
                                }
                            }
                        }
                ) {
                    // Draw overlay
                    drawRect(color = Color(0x99000000))

                    val targetWindowBounds: Rect? = when (currentStepId) {
                        STEP_HISTORY -> historyCardBounds
                        STEP_INPUTS -> unionBounds(glucosaCardBounds, presionCardBounds, depresionCardBounds)
                        else -> null
                    }

                    val overlay = overlayBounds
                    if (targetWindowBounds != null && overlay != null) {
                        val relLeft = targetWindowBounds.left - overlay.left
                        val relTop = targetWindowBounds.top - overlay.top
                        val w = targetWindowBounds.width
                        val h = targetWindowBounds.height
                        val paddingPx = 12f * density

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.18f),
                            topLeft = Offset(relLeft - paddingPx, relTop - paddingPx),
                            size = Size(w + paddingPx * 2f, h + paddingPx * 2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                        )
                    }
                }

                // message box — content depends on currentStepId
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (tutorialIndex == 0) Alignment.Center else Alignment.TopCenter
                ) {
                    Column(
                        // when it's the first step we put a small bottom padding so the card sits above the nav / edge;
                        // otherwise we keep the original top padding
                        modifier = if (tutorialIndex == 0) Modifier.padding(bottom = 36.dp) else Modifier.padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 620.dp) // optional: keep message width reasonable on wide screens
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val message = when (currentStepId) {
                                    STEP_HISTORY -> "Toque el historial para ver entradas pasadas"
                                    STEP_INPUTS -> "Toque las tarjetas para ingresar un nuevo registro"
                                    else -> "Tutorial"
                                }

                                Text(
                                    text = message,
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(onClick = {
                                    if (tutorialIndex < tutorialSequence.size - 1) {
                                        tutorialIndex += 1
                                    } else {
                                        prefs.edit().putBoolean("tutorial_shown", true).apply()
                                        showTutorial = false
                                    }
                                }) {
                                    Text(if (tutorialIndex < tutorialSequence.size - 1) "Entendido" else "Hecho")
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}

private fun unionBounds(vararg rects: Rect?): Rect? {
    val list = rects.filterNotNull()
    if (list.isEmpty()) return null
    val left = list.minOf { it.left }
    val top = list.minOf { it.top }
    val right = list.maxOf { it.right }
    val bottom = list.maxOf { it.bottom }
    return Rect(left, top, right, bottom)
}

@Composable
fun HistoryCard(
    history: List<HealthEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick, // make the whole card clickable
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.hsl(0f, 0f, 0.10f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Ver completo",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No hay registros aún",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(history) { entry ->
                        HistoryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(entry: HealthEntry, modifier: Modifier = Modifier) {
    val formatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    // Visual mapping per type
    val signaturePink = Color(0xFFE84589)
    val pressureBlue = Color(0xFF2B8CFF)
    val (iconPainter, iconBg, valueColor) = when (entry.label.lowercase(Locale.getDefault())) {
        "glucosa" -> Triple(
            painterResource(id = R.drawable.outline_glucose_24),
            signaturePink.copy(alpha = 0.14f),
            signaturePink
        )
        "presion", "presión", "presion arterial" -> Triple(
            painterResource(id = R.drawable.outline_blood_pressure_24),
            pressureBlue.copy(alpha = 0.12f),
            pressureBlue
        )
        else -> Triple(
            rememberVectorPainter(Icons.Default.Edit), // convert ImageVector to Painter
            Color.LightGray.copy(alpha = 0.12f),
            Color.Black
        )
    }

    // Row container: small white card so it stands out from the grey background
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = entry.label,
                    tint = valueColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Label + timestamp
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = remember(entry.timestamp) { formatter.format(Date(entry.timestamp)) },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Value (right aligned)
            Text(
                text = entry.value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 72.dp) // keeps a stable width
            )
        }
    }
}

@Composable
fun HealthRecordCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Ingresar $label",
                tint = Color.Gray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun parseDateTimeToMillis(dateStr: String?, timeStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null

    val candidates = mutableListOf<String>()

    // Prefer combined date + time
    if (!timeStr.isNullOrBlank()) {
        candidates += "$dateStr $timeStr"
    }
    // Also try just the date
    candidates += dateStr

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss", // matches defaultDateFormat + time from DBhelper example
        "yyyy-MM-dd HH:mm",    // tolerant
        "yyyy-MM-dd",          // date only
        "dd/MM/yyyy HH:mm",    // in case older DB saved this way
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy"
    )

    for (candidate in candidates) {
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(candidate)
                if (date != null) return date.time
            } catch (_: Exception) {
                // try next format
            }
        }
    }

    return null
}

private fun formatTimestamp(millis: Long?, formatter: SimpleDateFormat): String {
    return if (millis == null) {
        ""
    } else {
        formatter.format(Date(millis))
    }
}


