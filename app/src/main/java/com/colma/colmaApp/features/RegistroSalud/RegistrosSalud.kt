package com.colma.colmaApp.features.RegistroSalud

import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.EstadoAnimoMenu.EstadoAnimoMenuActivity
import com.colma.colmaApp.features.estadoAnimo.EstadoAnimoActivity
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import com.colma.colmaApp.utils.varios.TimeInputDialog
import java.text.SimpleDateFormat
import java.util.*

/** Simple data class for history items */
data class HealthEntry(val label: String, val value: String, val timestamp: Long)

/** Simple local screen enum */
private sealed class RSscreen {
    object Overview : RSscreen()
    object Glucosa : RSscreen()
    object Presion : RSscreen()
    object Depresion : RSscreen()
}

class RegistrosSalud : ComponentActivity() {
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
                RegistrosSaludScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosSaludScreen(
    onBack: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // DB helper (single instance)
    val dbHelper = remember { DBhelper(context.applicationContext) }

    // navigation state (Overview by default)
    var screen by remember { mutableStateOf<RSscreen>(RSscreen.Overview) }

    // dialog & inputs state (shared)
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var dialogLabel by rememberSaveable { mutableStateOf("") }

    var glucosaValue by rememberSaveable { mutableStateOf("") }
    var glucosaFasting by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var glucosaError by rememberSaveable { mutableStateOf<String?>(null) }

    var presionSys by rememberSaveable { mutableStateOf("") }
    var presionDia by rememberSaveable { mutableStateOf("") }
    var presionError by rememberSaveable { mutableStateOf<String?>(null) }
    var presionFasting by rememberSaveable { mutableStateOf<Boolean?>(null) }

    var confirmSaveDialogOpen by rememberSaveable { mutableStateOf(false) }
    var confirmLabel by rememberSaveable { mutableStateOf("") }

    var depresionValue by rememberSaveable { mutableStateOf("") }
    var depresionError by rememberSaveable { mutableStateOf<String?>(null) }

    // last-saved strings (for dialogs)
    var lastGlucosaValue by rememberSaveable { mutableStateOf("") }
    var lastGlucosaTime by rememberSaveable { mutableStateOf("") }
    var lastPresionValue by rememberSaveable { mutableStateOf("") }
    var lastPresionTime by rememberSaveable { mutableStateOf("") }

    // fasting selections for dialogs


// last-saved fasting flags (used when showing the "Último:" line)
    var lastGlucosaFasting by rememberSaveable { mutableStateOf(false) }
    var lastPresionFasting by rememberSaveable { mutableStateOf(false) }

    // for Presion dialog
    var presionDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) } // millis
    val presionCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = presionDateTime } }

// for Glucosa dialog
    var glucosaDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) } // millis
    val glucosaCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = glucosaDateTime } }

    var pendingPresionFechaRegistro by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingGlucosaFechaRegistro by rememberSaveable { mutableStateOf<String?>(null) }

    // in-memory history (newest first)
    val history = remember { mutableStateListOf<HealthEntry>() }

    val timeFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }
    // add near timeFormatter
    val isoFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    fun formattedNow(): String = timeFormatter.format(Date())

    var glucosaSyncChecked by rememberSaveable { mutableStateOf(false) }
    var presionSyncChecked by rememberSaveable { mutableStateOf(false) }

    var glucosaSyncDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val glucosaSyncCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = glucosaSyncDateTime } }

    var presionSyncDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val presionSyncCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = presionSyncDateTime } }

    // add near pendingPresionFechaRegistro / pendingGlucosaFechaRegistro
    var pendingGlucosaFechaSincronizacion by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPresionFechaSincronizacion by rememberSaveable { mutableStateOf<String?>(null) }

    var showGlucosaTimePicker by remember { mutableStateOf(false) }
    var showPresionTimePicker by remember { mutableStateOf(false) }

    TimeInputDialog(
        show = showGlucosaTimePicker,
        onDismiss = { showGlucosaTimePicker = false },
        onTimeSelected = { hour, minute ->
            glucosaCalendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            glucosaCalendar.set(java.util.Calendar.MINUTE, minute)
            glucosaDateTime = glucosaCalendar.timeInMillis
            showGlucosaTimePicker = false
        },
        initialHour = glucosaCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = glucosaCalendar.get(java.util.Calendar.MINUTE)
    )

    TimeInputDialog(
        show = showPresionTimePicker,
        onDismiss = { showPresionTimePicker = false },
        onTimeSelected = { hour, minute ->
            presionCalendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            presionCalendar.set(java.util.Calendar.MINUTE, minute)
            presionDateTime = presionCalendar.timeInMillis
            showPresionTimePicker = false
        },
        initialHour = presionCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = presionCalendar.get(java.util.Calendar.MINUTE)
    )



    // When the screen changes to a detail screen, attempt to load latest from DB for that metric
    LaunchedEffect(screen) {
        try {
            when (screen) {
                RSscreen.Glucosa -> {
                    // load all glucose records from DB on IO
                    val all = withContext(Dispatchers.IO) { dbHelper.getAllGlucosa(null) }

                    // map DB rows -> HealthEntry (with timestamp and formatted display)
                    val entries = all.mapNotNull { g ->
                        // parse DB datetime to millis (fallback to now)
                        val millis = parseDateTimeToMillis(g.fecha_hora_edicion) ?: System.currentTimeMillis()

                        // format value nicely (no decimals when integer, otherwise one decimal)
                        val valueText = g.valorMgdl?.let { v ->
                            if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.getDefault(), "%.1f", v)
                        } ?: ""

                        // fasting flag
                        val fasting = (g.ayuno ?: 0) == 1
                        val suffix = if (fasting) " · Ayuno" else " · Sin Ayuno"
                        val display = "${valueText} mg/dl$suffix"

                        // build HealthEntry — ensure label consistent with previous code
                        HealthEntry("Glucosa", display, millis)
                    }
                    // sort newest first
                    val sorted = entries.sortedByDescending { it.timestamp }

                    // remove any old Glucosa entries and add all the new ones (newest first at front)
                    history.removeAll { it.label.equals("Glucosa", ignoreCase = true) }
                    history.addAll(0, sorted)

                    // update the "last" summary fields from the most recent DB record (if present)
                    val newestRecord = all.maxByOrNull { parseDateTimeToMillis(it.fecha_hora_edicion) ?: 0L }
                    newestRecord?.let { g ->
                        lastGlucosaValue = g.valorMgdl?.let {
                            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                        } ?: ""
                        val millis = parseDateTimeToMillis(g.fecha_hora_edicion) ?: System.currentTimeMillis()
                        lastGlucosaTime = formatTimestamp(millis)
                        lastGlucosaFasting = (g.ayuno ?: 0) == 1
                    }
                }

                RSscreen.Presion -> {
                    // load all pressure records from DB on IO
                    val all = withContext(Dispatchers.IO) { dbHelper.getAllPresion(null) }

                    // helper formatter
                    fun fmt(v: Double?) = when {
                        v == null -> ""
                        v % 1.0 == 0.0 -> v.toInt().toString()
                        else -> String.format(Locale.getDefault(), "%.1f", v)
                    }

                    // map to HealthEntry
                    val entries = all.mapNotNull { p ->
                        val millis = parseDateTimeToMillis(p.fecha_hora_edicion) ?: System.currentTimeMillis()
                        val syst = fmt(p.sistolica)
                        val dia = fmt(p.diastolica)
                        val valueText = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia mmHg" else ""
                        HealthEntry("Presion", valueText, millis)
                    }.sortedByDescending { it.timestamp }

                    // clear previous Presion entries and add all loaded ones
                    history.removeAll { it.label.startsWith("Presion", ignoreCase = true) || it.label.startsWith("Presión", ignoreCase = true) }
                    history.addAll(0, entries)

                    // update the "last" summary fields from most recent DB record (if present)
                    val newestRecord = all.maxByOrNull { parseDateTimeToMillis(it.fecha_hora_edicion) ?: 0L }
                    newestRecord?.let { p ->
                        fun fmtSingle(v: Double?) = when {
                            v == null -> ""
                            v % 1.0 == 0.0 -> v.toInt().toString()
                            else -> String.format(Locale.getDefault(), "%.1f", v)
                        }
                        val syst = fmtSingle(p.sistolica)
                        val dia = fmtSingle(p.diastolica)
                        lastPresionValue = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia" else ""
                        val millis = parseDateTimeToMillis(p.fecha_hora_edicion) ?: System.currentTimeMillis()
                        lastPresionTime = formatTimestamp(millis)
                    }
                }

                else -> { /* nothing */ }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDialogFor(label: String) {
        dialogLabel = label
        when (label) {
            "Glucosa" -> {
                glucosaValue = ""
                glucosaError = null
                glucosaFasting = null
                glucosaDateTime = System.currentTimeMillis()
                glucosaCalendar.timeInMillis = glucosaDateTime

                // sync defaults
                glucosaSyncChecked = false
                glucosaSyncDateTime = System.currentTimeMillis()
                glucosaSyncCalendar.timeInMillis = glucosaSyncDateTime
            }

            "Presion" -> {
                presionSys = ""
                presionDia = ""
                presionError = null
                presionFasting = null
                presionDateTime = System.currentTimeMillis()
                presionCalendar.timeInMillis = presionDateTime

                // sync defaults
                presionSyncChecked = false
                presionSyncDateTime = System.currentTimeMillis()
                presionSyncCalendar.timeInMillis = presionSyncDateTime
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

    fun handleSave() {
        // do not call any @Composable here!
        when (dialogLabel) {
            "Glucosa" -> {
                // validate again defensively
                val number = glucosaValue.trim().replace(',', '.').toDoubleOrNull()
                if (number == null) {
                    glucosaError = "Ingrese un número válido"
                    return
                }
                if (glucosaFasting == null) {
                    glucosaError = "Seleccione si está en ayuno o no"
                    return
                }

                coroutineScope.launch {
                    try {
                        // --> NEW: fecha_registro = system now (ISO)
                        val fechaRegistroIso = isoFormatter.format(Date())

                        // fecha_hora_edicion = user-picked datetime (prefer pending ISO override, otherwise use glucosaDateTime)
                        val fechaEdicionIso = pendingGlucosaFechaRegistro ?: isoFormatter.format(Date(glucosaDateTime))

                        val rowId = withContext(Dispatchers.IO) {
                            dbHelper.insertGlucosa(
                                valorMgdl = number,
                                idUsuario = 1,
                                ayuno = glucosaFasting,
                                fechaRegistro = fechaRegistroIso,
                                fechaHoraEdicion = fechaEdicionIso
                            )
                        }

                        if (rowId > 0L) {
                            // Use the user-picked (edit) datetime for display/history
                            lastGlucosaValue = if (number % 1.0 == 0.0)
                                number.toInt().toString()
                            else
                                String.format(Locale.getDefault(), "%.1f", number)

                            // show the user-picked time
                            lastGlucosaTime = formatTimestamp(parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis())
                            lastGlucosaFasting = glucosaFasting == true

                            val suffix = if (glucosaFasting == true) " · Ayuno" else " · Sin Ayuno"
                            val millis = parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis()
                            history.add(0, HealthEntry("Glucosa", "${lastGlucosaValue} mg/dl$suffix", millis))

                            Toast.makeText(context, "Guardado: Glucosa = $lastGlucosaValue mg/dl", Toast.LENGTH_SHORT).show()

                            // ONLY write fecha_sincronizacion if there's an explicit pending sync timestamp
                            val syncIso = pendingGlucosaFechaSincronizacion
                            if (!syncIso.isNullOrBlank()) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        dbHelper.updateGlucosaSyncTimestamps(mapOf(rowId to syncIso))
                                        dbHelper.insertSincronizacion(syncIso, "OK", null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        dbHelper.insertSincronizacion(syncIso, "ERROR", e.message)
                                    }
                                }
                            }

                            // close dialogs
                            confirmSaveDialogOpen = false
                            closeDialog()
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error guardando en la base de datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            "Presion" -> {
                // validate
                val sys = presionSys.trim().replace(',', '.').toDoubleOrNull()
                val dia = presionDia.trim().replace(',', '.').toDoubleOrNull()
                if (sys == null || dia == null) {
                    presionError = "Ingrese números válidos"
                    return
                }
                if (sys <= dia) {
                    presionError = "El valor mayor debe ser mayor que el menor"
                    return
                }

                coroutineScope.launch {
                    try {
                        // fecha_registro = system now (ISO)
                        val fechaRegistroIso = isoFormatter.format(Date())

                        // fecha_hora_edicion = user-picked datetime (prefer pending ISO override, otherwise use presionDateTime)
                        val fechaEdicionIso = pendingPresionFechaRegistro ?: isoFormatter.format(Date(presionDateTime))

                        val rowId = withContext(Dispatchers.IO) {
                            dbHelper.insertPresion(
                                sistolica = sys,
                                diastolica = dia,
                                idUsuario = 1,
                                fechaRegistro = fechaRegistroIso,
                                fechaHoraEdicionIso = fechaEdicionIso
                            )
                        }

                        if (rowId > 0L) {
                            // update UI state using the user-picked (edit) datetime
                            lastPresionValue = if (sys % 1.0 == 0.0 && dia % 1.0 == 0.0) {
                                "${sys.toInt()}/${dia.toInt()}"
                            } else {
                                "${String.format(Locale.getDefault(), "%.1f", sys)}/${String.format(Locale.getDefault(), "%.1f", dia)}"
                            }

                            // show the user-picked edit time
                            val millis = parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis()
                            lastPresionTime = formatTimestamp(millis)
                            history.add(0, HealthEntry("Presion", "$lastPresionValue mmHg", millis))

                            Toast.makeText(context, "Guardado: Presión = $lastPresionValue", Toast.LENGTH_SHORT).show()

                            // ONLY write fecha_sincronizacion if there's an explicit pending sync timestamp
                            val syncIso = pendingPresionFechaSincronizacion
                            if (!syncIso.isNullOrBlank()) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        dbHelper.updatePresionSyncTimestamps(mapOf(rowId to syncIso))
                                        dbHelper.insertSincronizacion(syncIso, "OK", null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        dbHelper.insertSincronizacion(syncIso, "ERROR", e.message)
                                    }
                                }
                            }

                            confirmSaveDialogOpen = false
                            closeDialog()
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error guardando en la base de datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            "Depresion" -> {
                if (depresionValue.isBlank()) {
                    depresionError = "Por favor ingrese un comentario"
                    return
                }
                val now = System.currentTimeMillis()
                history.add(0, HealthEntry("Depresion", depresionValue, now))
                Toast.makeText(context, "Guardado: Depresion", Toast.LENGTH_SHORT).show()
                confirmSaveDialogOpen = false
                closeDialog()
            }
        }
    }
    // UI
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Registro de Salud", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        // If in detail screen go back to overview, otherwise finish activity
                        when (screen) {
                            RSscreen.Overview -> onBack()
                            else -> screen = RSscreen.Overview
                        }
                    }) {
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
            when (screen) {
                RSscreen.Overview -> {
                    // Overview with three buttons
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(top = 40.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¿Qué te gustaría hacer?",
                            fontSize = 25.sp,
                            color = signatureColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )

                        // Glucosa button (signature color)
                        Button(
                            onClick = { screen = RSscreen.Glucosa },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(65.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F0F0),
                                contentColor = Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_glucose_24),
                                    contentDescription = "Registros",
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Glucosa",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Presion button (neutral)
                        Button(
                            onClick = { screen = RSscreen.Presion },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(65.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F0F0),
                                contentColor = Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_blood_pressure_24),
                                    contentDescription = "Registros",
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Presion Arterial",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Depresion button
                        Button(
                            onClick = {
                                val intent = Intent(context, EstadoAnimoMenuActivity::class.java)

                                // Optional: pass extras, e.g. patient id
                                // intent.putExtra("idPaciente", thePatientId)

                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(65.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF7F7F8),
                                contentColor = Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Registros",
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Estado de animo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }

                RSscreen.Glucosa -> {
                    // Glucosa detail screen: one card + history filtered
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // a single HealthRecordCard for Glucosa (opens the input dialog)
                        HealthRecordCard(label = "Glucosa", onClick = { openDialogFor("Glucosa") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(28.dp))

                        // history filtered to Glucosa
                        val glucosaHistory = history.filter { it.label.equals("Glucosa", ignoreCase = true) }
                        HistoryCard(history = glucosaHistory, onClear = { history.removeAll(glucosaHistory) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp))
                    }
                }

                RSscreen.Presion -> {
                    // Presion detail screen: one card + history filtered
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HealthRecordCard(label = "Presion", onClick = { openDialogFor("Presion") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(18.dp))

                        val presionHistory = history.filter { it.label.startsWith("Presion", ignoreCase = true) || it.label.startsWith("Presión", ignoreCase = true) }
                        HistoryCard(history = presionHistory, onClear = { history.removeAll(presionHistory) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp))
                    }
                }

                RSscreen.Depresion -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HealthRecordCard(label = "Depresion", onClick = { openDialogFor("Depresion") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(18.dp))
                        val depresionHistory = history.filter { it.label.equals("Depresion", ignoreCase = true) }
                        HistoryCard(history = depresionHistory, onClear = { history.removeAll(depresionHistory) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp))
                    }
                }
            }

            // Dialogs (shared)
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

                                        placeholder = { Text(text = "") },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // fasting radio row
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = glucosaFasting == true,
                                            onClick = { glucosaFasting = true }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Ayuno")

                                        Spacer(modifier = Modifier.width(16.dp))

                                        RadioButton(
                                            selected = glucosaFasting == false,
                                            onClick = { glucosaFasting = false }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "No ayuno")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        // date display field (read-only, opens DatePicker) — shows only the date
                                        OutlinedTextField(
                                            value = dateFormatter.format(java.util.Date(glucosaDateTime)),
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = glucosaCalendar
                                                    val dpd = android.app.DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            // set only date components, preserve time (hour/minute)
                                                            c.set(java.util.Calendar.YEAR, y)
                                                            c.set(java.util.Calendar.MONTH, m)
                                                            c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                            glucosaDateTime = c.timeInMillis
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
                                                    val c = glucosaCalendar
                                                    val dpd = android.app.DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            c.set(java.util.Calendar.YEAR, y)
                                                            c.set(java.util.Calendar.MONTH, m)
                                                            c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                            glucosaDateTime = c.timeInMillis
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

                                        // time button opens TimePicker and shows only time (HH:mm)
                                        OutlinedButton(onClick = {
                                            showGlucosaTimePicker = true
                                        }) {
                                            Text(text = timeFormatterOnly.format(java.util.Date(glucosaDateTime)))
                                        }
                                    }

                                    // validation / last saved
                                    if (glucosaError != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = glucosaError ?: "", color = Color.Red, fontSize = 12.sp)
                                    } else if (glucosaFasting == null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Seleccione Ayuno o No Ayuno", color = Color.Red, fontSize = 12.sp)
                                    } else if (lastGlucosaValue.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val fastingSuffix = if (lastGlucosaFasting) " · Ayuno" else ""
                                        Text(
                                            text = "Último: $lastGlucosaValue mg/dl · $lastGlucosaTime$fastingSuffix",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    // validate value
                                    if (glucosaValue.isBlank()) {
                                        glucosaError = "Por favor ingrese un valor"
                                        return@TextButton
                                    }

                                    // validate numeric
                                    val number = glucosaValue.replace(',', '.').toDoubleOrNull()
                                    if (number == null) {
                                        glucosaError = "Ingrese un número válido"
                                        return@TextButton
                                    }

                                    // validate fasting selection
                                    if (glucosaFasting == null) {
                                        glucosaError = "Seleccione si está en ayuno o no"
                                        return@TextButton
                                    }

                                    // all good -> clear error and open confirmation
                                    glucosaError = null
                                    confirmLabel = "Glucosa"
                                    confirmSaveDialogOpen = true
                                }) {
                                    Text("Guardar")
                                }
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
                                    Text(text = "Ingresar valor mayor", fontSize = 15.sp, color = Color.Black)
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
                                        placeholder = { Text(text = "") },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(text = "Ingresar valor menor", fontSize = 15.sp, color = Color.Black)
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
                                        placeholder = { Text(text = "") },
                                        textStyle = TextStyle(color = Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = dateFormatter.format(java.util.Date(presionDateTime)),
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val c = presionCalendar
                                                    val dpd = android.app.DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            c.set(java.util.Calendar.YEAR, y)
                                                            c.set(java.util.Calendar.MONTH, m)
                                                            c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                            presionDateTime = c.timeInMillis
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
                                                    val c = presionCalendar
                                                    val dpd = android.app.DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            c.set(java.util.Calendar.YEAR, y)
                                                            c.set(java.util.Calendar.MONTH, m)
                                                            c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                            presionDateTime = c.timeInMillis
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

                                        OutlinedButton(onClick = {
                                            showPresionTimePicker = true
                                        }) {
                                            Text(text = timeFormatterOnly.format(java.util.Date(presionDateTime)))
                                        }
                                    }

                                    if (presionError != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = presionError ?: "", color = Color.Red, fontSize = 12.sp)
                                    } else if (lastPresionValue.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val fastingSuffix = if (lastPresionFasting) " · Ayuno" else ""
                                        Text(
                                            text = "Último: $lastPresionValue mmHg · $lastPresionTime$fastingSuffix",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    // validate presence
                                    if (presionSys.isBlank() || presionDia.isBlank()) {
                                        presionError = "Ingrese ambos valores (mayor y menor)"
                                        return@TextButton
                                    }

                                    // validate numeric
                                    val sys = presionSys.replace(',', '.').toDoubleOrNull()
                                    val dia = presionDia.replace(',', '.').toDoubleOrNull()
                                    if (sys == null || dia == null) {
                                        presionError = "Ingrese números válidos"
                                        return@TextButton
                                    }

                                    // optional: ensure systolic > diastolic (if desired)
                                    if (sys <= dia) {
                                        presionError = "El valor mayor debe ser mayor que el menor"
                                        return@TextButton
                                    }

                                    // optional fasting check if you want:
                                    // if (presionFasting == null) { presionError = "Seleccione si está en ayuno o no"; return@TextButton }

                                    // all good -> clear errors and open confirm
                                    presionError = null
                                    confirmLabel = "Presion"
                                    confirmSaveDialogOpen = true
                                }) {
                                    Text("Guardar")
                                }
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

            if (confirmSaveDialogOpen) {
                // Build basic confirm message depending on what the user is about to save
                val confirmMessage = when (confirmLabel) {
                    "Glucosa" -> {
                        val v = if (glucosaValue.isBlank()) "(sin valor)" else glucosaValue
                        val fast = when (glucosaFasting) {
                            true -> " · Ayuno"
                            false -> " · No ayuno"
                            null -> ""
                        }
                        "¿Está seguro que desea guardar Glucosa: $v$fast ?"
                    }
                    "Presion" -> {
                        val syst = presionSys.ifBlank { "(sin valor)" }
                        val dia = presionDia.ifBlank { "(sin valor)" }
                        "¿Está seguro que desea guardar Presión: $syst / $dia?"
                    }
                    else -> "¿Está seguro que desea guardar los datos?"
                }

                // Compute warning and impossible detection in one remember block
                val (warningText, warningColor, impossibleReason) = remember(
                    confirmLabel, glucosaValue, glucosaFasting, presionSys, presionDia
                ) {
                    var wTxt: String? = null
                    var wColor: Color? = null
                    var impossible: String? = null

                    try {
                        when (confirmLabel) {
                            "Glucosa" -> {
                                // normalize decimal comma to dot and parse
                                val v = glucosaValue.trim().replace(',', '.').toDoubleOrNull()
                                if (v != null) {
                                    // --- IMPOSSIBLE thresholds (adjust if you want) ---
                                    if (v < 10.0) {
                                        impossible = "Valor imposible: Glucosa demasiado baja para ser plausible."
                                    } else if (v > 1000.0) {
                                        impossible = "Valor imposible: Glucosa extremadamente alta para ser plausible."
                                    }

                                    // --- WARNING thresholds (still allow save) ---
                                    // low < 70 ; high: fasting >=126 else >=180
                                    val low = v < 70.0
                                    val high = if (glucosaFasting == true) v >= 126.0 else v >= 180.0
                                    when {
                                        low -> {
                                            wTxt = "Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico."
                                            wColor = Color(0xFFD32F2F) // red
                                        }
                                        high -> {
                                            wTxt = "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico."
                                            wColor = Color(0xFFFFA000) // amber
                                        }
                                        else -> {
                                            wTxt = null
                                            wColor = null
                                        }
                                    }
                                }
                            }

                            "Presion" -> {
                                // parse integers (ignore blanks)
                                val s = presionSys.trim().toIntOrNull()
                                val d = presionDia.trim().toIntOrNull()

                                // --- IMPOSSIBLE checks ---
                                if (s != null && (s < 40 || s > 300)) {
                                    impossible = "Valor imposible: presión sistólica fuera de rango plausible."
                                } else if (d != null && (d < 20 || d > 200)) {
                                    impossible = "Valor imposible: presión diastólica fuera de rango plausible."
                                } else if (s != null && d != null && s < d) {
                                    impossible = "Valor imposible: la presión sistólica no puede ser menor que la diastólica."
                                }

                                // --- WARNING checks (still allow save) ---
                                val highS = s != null && s > 140
                                val lowS = s != null && s < 90
                                val highD = d != null && d > 90
                                val lowD = d != null && d < 60

                                val parts = mutableListOf<String>()
                                if (lowS || lowD) parts.add("presión baja")
                                if (highS || highD) parts.add("presión alta")
                                if (parts.isNotEmpty()) {
                                    wTxt = "Advertencia: " + parts.joinToString(" y ") + ". Por favor consulte con un médico."
                                    wColor = if (lowS || lowD || highS) Color(0xFFD32F2F) else Color(0xFFFFA000)
                                }
                            }

                            else -> {
                                // no special validations
                            }
                        }
                    } catch (e: Exception) {
                        // swallow parsing errors and treat as no warning/no impossible
                        wTxt = null
                        wColor = null
                        impossible = null
                    }

                    Triple(wTxt, wColor, impossible)
                }

                // derive boolean quickly
                val isImpossible = impossibleReason != null

                AlertDialog(
                    onDismissRequest = { confirmSaveDialogOpen = false },
                    title = {
                        Text(
                            text = "Confirmar",
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.black)
                        )
                    },
                    text = {
                        Column {
                            Text(text = confirmMessage, color = colorResource(R.color.black))

                            Spacer(modifier = Modifier.height(10.dp))

                            // show impossible message first (if present) — blocks saving
                            if (isImpossible) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = impossibleReason ?: "Valor imposible",
                                        color = Color(0xFFD32F2F),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // show the regular warning row if needed (but this is not blocking)
                            if (warningText != null && warningColor != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = warningColor.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Advertencia",
                                        tint = warningColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = warningText,
                                        color = warningColor,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        // disable the confirm button if impossible
                        TextButton(
                            onClick = {
                                if (!isImpossible) {
                                    confirmSaveDialogOpen = false
                                    handleSave()
                                }
                            },
                            enabled = !isImpossible
                        ) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmSaveDialogOpen = false }) {
                            Text("No")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
            }

        }
    }
}

/** HistoryCard same as you had — keeps an onClear callback */
@Composable
fun HistoryCard(
    history: List<HealthEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
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
                Text(text = "Historial", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 16.sp, color = Color.hsl(0f, 0f, 0.10f))
                Spacer(modifier = Modifier.weight(1f))
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
    val (iconTint, bgAlpha, valueColor) = when (entry.label.lowercase(Locale.getDefault())) {
        "glucosa" -> Triple(painterResource(R.drawable.outline_glucose_24), 0.14f, signaturePink)
        "presion", "presión", "presion arterial" -> Triple(painterResource(R.drawable.outline_blood_pressure_24), 0.12f, pressureBlue)
        else -> Triple(rememberVectorPainter(Icons.Default.Face), 0.12f, Color.Black)
    }

    // Build display value:
    // 1) detect and keep a fasting-like suffix (e.g. "· Ayuno", "· Sin Ayuno", "Ayuno", "fasting")
    //    - capture optional separator (· • - |) preceding the token
    // 2) clean the main part removing unit tokens (mg/dl, mmHg, etc.)
    // 3) trim and reattach the suffix (preserving the separator)
    val displayValue = remember(entry.value) {
        val raw = (entry.value ?: "").trim()
        if (raw.isEmpty()) return@remember raw

        // regex to capture suffix (including optional preceding separator like "·")
        val fastingRegex = Regex(
            """(?i)(?:\s*[·•\-\|]\s*)?(ayuno|sin ayuno|sinayuno|ayunas?|fasting|no fasting)\b.*$"""
        )
        val fastingMatch = fastingRegex.find(raw)

        val mainPart: String
        val suffixPart: String
        if (fastingMatch != null) {
            // keep the suffix exactly as it appears (including separator)
            suffixPart = raw.substring(fastingMatch.range.first).trim()
            mainPart = raw.substring(0, fastingMatch.range.first).trim()
        } else {
            suffixPart = ""
            mainPart = raw
        }

        // remove common explicit unit tokens from the main part
        var main = mainPart.replace(Regex("""(?i)\b(mg\/dl|mmhg|mmol\/l|bpm|kg|cm|%|mmol\/L)\b"""), "").trim()

        // remove any trailing punctuation/words that are not part of numeric values.
        // keep digits, slash (for pressure), dot/comma, hyphen.
        main = main.replace(Regex("""\s*[^\d\/\.,-]+$"""), "").trim()

        // avoid dangling separators: if main becomes only a separator (like "/"), drop it
        if (main.matches(Regex("""^[\/\-,\.]+$"""))) {
            main = ""
        }

        // reattach suffix if present
        val combined = when {
            main.isNotBlank() && suffixPart.isNotBlank() -> "$main $suffixPart"
            main.isBlank() && suffixPart.isNotBlank() -> suffixPart
            else -> main
        }

        combined
    }

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
            // icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(valueColor.copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = iconTint, contentDescription = entry.label, tint = valueColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.label, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = remember(entry.timestamp) { formatter.format(Date(entry.timestamp)) }, fontSize = 12.sp, color = Color.Gray)
            }

            Text(
                text = displayValue,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = valueColor
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Color.Black, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Add, contentDescription = "Ingresar $label", tint = Color.Gray)
        }
    }
}

private fun parseDateTimeToMillis(dateTimeStr: String?): Long? {
    if (dateTimeStr.isNullOrBlank()) return null

    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(dateTimeStr)?.time
    } catch (_: Exception) {
        null
    }
}

private fun formatTimestamp(millis: Long?): String {
    if (millis == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}