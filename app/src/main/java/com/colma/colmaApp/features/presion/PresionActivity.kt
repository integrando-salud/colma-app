package com.colma.colmaApp.features.presion

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PresionActivity : ComponentActivity() {
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
                PresionScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresionScreen(onBack: () -> Unit) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { DBhelper(context.applicationContext) }

    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var presionSys by rememberSaveable { mutableStateOf("") }
    var presionDia by rememberSaveable { mutableStateOf("") }
    var presionError by rememberSaveable { mutableStateOf<String?>(null) }
    var lastPresionValue by rememberSaveable { mutableStateOf("") }
    var lastPresionTime by rememberSaveable { mutableStateOf("") }
    var lastPresionFasting by rememberSaveable { mutableStateOf(false) }

    var presionDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val presionCalendar = remember { Calendar.getInstance().apply { timeInMillis = presionDateTime } }

    var pendingPresionFechaRegistro by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPresionFechaSincronizacion by rememberSaveable { mutableStateOf<String?>(null) }

    val history = remember { mutableStateListOf<HealthEntry>() }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    var showPresionTimePicker by remember { mutableStateOf(false) }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var entryForDialog by remember { mutableStateOf<HealthEntry?>(null) }

    TimeInputDialog(
        show = showPresionTimePicker,
        onDismiss = { showPresionTimePicker = false },
        onTimeSelected = { hour, minute ->
            presionCalendar.set(Calendar.HOUR_OF_DAY, hour)
            presionCalendar.set(Calendar.MINUTE, minute)
            presionDateTime = presionCalendar.timeInMillis
            showPresionTimePicker = false
        },
        initialHour = presionCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = presionCalendar.get(Calendar.MINUTE)
    )

    LaunchedEffect(Unit) {
        try {
            val all = withContext(Dispatchers.IO) { dbHelper.getAllPresion(null) }

            fun fmt(v: Double?) = when {
                v == null -> ""
                v % 1.0 == 0.0 -> v.toInt().toString()
                else -> String.format(Locale.getDefault(), "%.1f", v)
            }

            val entries = all.mapNotNull { p ->
                val millis = parseDateTimeToMillis(p.fecha_hora_edicion) ?: System.currentTimeMillis()
                val syst = fmt(p.sistolica)
                val dia = fmt(p.diastolica)
                val valueText = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia mmHg" else ""
                HealthEntry(
                    label = "Presion",
                    value = valueText,
                    timestamp = millis,
                    rawValue1 = p.sistolica,
                    rawValue2 = p.diastolica
                )
            }.sortedByDescending { it.timestamp }

            history.addAll(entries)

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDialog() {
        presionSys = ""
        presionDia = ""
        presionError = null
        presionDateTime = System.currentTimeMillis()
        presionCalendar.timeInMillis = presionDateTime
        dialogOpen = true
    }

    fun closeDialog() {
        dialogOpen = false
    }

    fun handleSave() {
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
                val fechaRegistroIso = isoFormatter.format(Date())
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
                    lastPresionValue = if (sys % 1.0 == 0.0 && dia % 1.0 == 0.0) {
                        "${sys.toInt()}/${dia.toInt()}"
                    } else {
                        "${String.format(Locale.getDefault(), "%.1f", sys)}/${String.format(Locale.getDefault(), "%.1f", dia)}"
                    }
                    val millis = parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis()
                    lastPresionTime = formatTimestamp(millis)
                    history.add(
                        0, HealthEntry(
                            label = "Presion",
                            value = "$lastPresionValue mmHg",
                            timestamp = millis,
                            rawValue1 = sys,
                            rawValue2 = dia
                        )
                    )
                    Toast.makeText(context, "Guardado: Presión = $lastPresionValue", Toast.LENGTH_SHORT).show()

                    val highS = sys > 140
                    val lowS = sys < 90
                    val highD = dia > 90
                    val lowD = dia < 60
                    val parts = mutableListOf<String>()
                    if (lowS || lowD) parts.add("presión baja")
                    if (highS || highD) parts.add("presión alta")
                    if (parts.isNotEmpty()) {
                        val warningText = "Advertencia: " + parts.joinToString(" y ") + ". Por favor consulte con un médico."
                        Toast.makeText(context, warningText, Toast.LENGTH_LONG).show()
                    }

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

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Medición de Presión", fontWeight = FontWeight.Bold) },
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
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HealthRecordCard(label = "Presión", onClick = { openDialog() }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(18.dp))
                HistoryCard(history = history, onClear = { history.clear() }, onEntryClick = {
                    entryForDialog = it
                    showDetailsDialog = true
                }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp))
            }

            if (showDetailsDialog) {
                val entry = entryForDialog!!
                val (isDangerous, warningText) = remember(entry) {
                    var dangerous = false
                    var text: String? = null
                    val s = entry.rawValue1
                    val d = entry.rawValue2
                    if (s != null && d != null) {
                        val highS = s > 140
                        val lowS = s < 90
                        val highD = d > 90
                        val lowD = d < 60
                        if (highS || lowS || highD || lowD) {
                            dangerous = true
                            val parts = mutableListOf<String>()
                            if (lowS || lowD) parts.add("presión baja")
                            if (highS || highD) parts.add("presión alta")
                            text = parts.joinToString(" y ")
                        }
                    }
                    Pair(dangerous, text)
                }

                AlertDialog(
                    onDismissRequest = { showDetailsDialog = false },
                    title = { Text("Detalle del Registro") },
                    text = {
                        Column {
                            Text("Tipo: ${entry.label}")
                            Text("Valor: ${entry.value}")
                            Text("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))}")
                            if (isDangerous && warningText != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Advertencia: $warningText Por favor consulte con un médico.", color = Color.Red)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDetailsDialog = false }) {
                            Text("Cerrar")
                        }
                    }
                )
            }

            if (dialogOpen) {
                AlertDialog(
                    onDismissRequest = { closeDialog() },
                    title = { Text(text = "Ingresar Presión", color = Color.Black) },
                    text = {
                        Column {
                            Text(text = "Ingresar valor mayor", fontSize = 15.sp, color = Color.Black)
                            OutlinedTextField(
                                value = presionSys,
                                onValueChange = { newValue ->
                                    presionSys = if (newValue.length <= 3) newValue else newValue.substring(0, 3)
                                    presionError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Ingresar valor menor", fontSize = 15.sp, color = Color.Black)
                            OutlinedTextField(
                                value = presionDia,
                                onValueChange = { newValue ->
                                    presionDia = if (newValue.length <= 3) newValue else newValue.substring(0, 3)
                                    presionError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Fecha registro", fontSize = 15.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = dateFormatter.format(Date(presionDateTime)),
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 18.sp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val c = presionCalendar
                                        android.app.DatePickerDialog(context, { _, y, m, d ->
                                            c.set(Calendar.YEAR, y)
                                            c.set(Calendar.MONTH, m)
                                            c.set(Calendar.DAY_OF_MONTH, d)
                                            presionDateTime = c.timeInMillis
                                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = presionCalendar
                                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                                c.set(Calendar.YEAR, y)
                                                c.set(Calendar.MONTH, m)
                                                c.set(Calendar.DAY_OF_MONTH, d)
                                                presionDateTime = c.timeInMillis
                                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                        }) { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Fecha") }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Hora", fontSize = 15.sp, color = Color.Black)
                                OutlinedButton(onClick = { showPresionTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = timeFormatterOnly.format(Date(presionDateTime)), fontSize = 18.sp)
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
                        TextButton(
                            onClick = {
                                if (presionSys.isBlank() || presionDia.isBlank()) {
                                    presionError = "Ingrese ambos valores (mayor y menor)"
                                    return@TextButton
                                }
                                val sys = presionSys.replace(',', '.').toDoubleOrNull()
                                val dia = presionDia.replace(',', '.').toDoubleOrNull()
                                if (sys == null || dia == null) {
                                    presionError = "Ingrese números válidos"
                                    return@TextButton
                                }
                                if (sys <= dia) {
                                    presionError = "El valor mayor debe ser mayor que el menor"
                                    return@TextButton
                                }
                                if (sys < 40.0 || sys > 300.0) {
                                    presionError = "Valor imposible: presión sistólica fuera de rango plausible."
                                    return@TextButton
                                } else if (dia < 20.0 || dia > 200.0) {
                                    presionError = "Valor imposible: presión diastólica fuera de rango plausible."
                                    return@TextButton
                                }
                                presionError = null
                                handleSave()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                        ) { Text("Guardar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { closeDialog() }, colors = ButtonDefaults.textButtonColors(containerColor = Color.Red, contentColor = Color.White)) {
                            Text("Cancelar")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp
                )
            }
        }
    }
}
