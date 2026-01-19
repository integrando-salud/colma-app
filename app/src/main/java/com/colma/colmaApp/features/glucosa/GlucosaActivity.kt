package com.colma.colmaApp.features.glucosa

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.colma.colmaApp.utils.varios.HealthEntry
import com.colma.colmaApp.utils.varios.HealthRecordCard
import com.colma.colmaApp.utils.varios.HistoryCard
import com.colma.colmaApp.utils.varios.TimeInputDialog
import com.colma.colmaApp.utils.varios.formatTimestamp
import com.colma.colmaApp.utils.varios.parseDateTimeToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlucosaActivity : ComponentActivity() {
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
                GlucosaScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucosaScreen(onBack: () -> Unit) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dbHelper = remember { DBhelper(context.applicationContext) }

    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var glucosaValue by rememberSaveable { mutableStateOf("") }
    var glucosaFasting by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var glucosaError by rememberSaveable { mutableStateOf<String?>(null) }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var entryForDialog by remember { mutableStateOf<HealthEntry?>(null) }

    var lastGlucosaValue by rememberSaveable { mutableStateOf("") }
    var lastGlucosaTime by rememberSaveable { mutableStateOf("") }
    var lastGlucosaFasting by rememberSaveable { mutableStateOf(false) }

    var glucosaDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val glucosaCalendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = glucosaDateTime } }
    var pendingGlucosaFechaRegistro by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingGlucosaFechaSincronizacion by rememberSaveable { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<HealthEntry>() }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    var showGlucosaTimePicker by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        try {
            val all = withContext(Dispatchers.IO) { dbHelper.getAllGlucosa(null) }
            val entries = all.mapNotNull { g ->
                val millis = parseDateTimeToMillis(g.fecha_hora_edicion) ?: System.currentTimeMillis()
                val valueText = g.valorMgdl?.let { v ->
                    if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.getDefault(), "%.1f", v)
                } ?: ""
                val fasting = (g.ayuno ?: 0) == 1
                val suffix = if (fasting) " · Ayuno" else " · Sin Ayuno"
                val display = "${valueText} mg/dl$suffix"
                HealthEntry(
                    label = "Glucosa",
                    value = display,
                    timestamp = millis,
                    rawValue1 = g.valorMgdl,
                    isFasting = fasting
                )
            }
            val sorted = entries.sortedByDescending { it.timestamp }
            history.addAll(sorted)

            val newestRecord = all.maxByOrNull { parseDateTimeToMillis(it.fecha_hora_edicion) ?: 0L }
            newestRecord?.let { g ->
                lastGlucosaValue = g.valorMgdl?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                } ?: ""
                val millis = parseDateTimeToMillis(g.fecha_hora_edicion) ?: System.currentTimeMillis()
                lastGlucosaTime = formatTimestamp(millis)
                lastGlucosaFasting = (g.ayuno ?: 0) == 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDialog() {
        glucosaValue = ""
        glucosaError = null
        glucosaFasting = null
        glucosaDateTime = System.currentTimeMillis()
        glucosaCalendar.timeInMillis = glucosaDateTime
        pendingGlucosaFechaSincronizacion = null
        dialogOpen = true
    }

    fun closeDialog() {
        dialogOpen = false
    }

    fun handleSave() {
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
                val fechaRegistroIso = isoFormatter.format(Date())
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
                    lastGlucosaValue = if (number % 1.0 == 0.0)
                        number.toInt().toString()
                    else
                        String.format(Locale.getDefault(), "%.1f", number)

                    lastGlucosaTime = formatTimestamp(parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis())
                    lastGlucosaFasting = glucosaFasting == true

                    val suffix = if (glucosaFasting == true) " · Ayuno" else " · Sin Ayuno"
                    val millis = parseDateTimeToMillis(fechaEdicionIso) ?: System.currentTimeMillis()
                    history.add(
                        0, HealthEntry(
                            label = "Glucosa",
                            value = "${lastGlucosaValue} mg/dl$suffix",
                            timestamp = millis,
                            rawValue1 = number,
                            isFasting = glucosaFasting
                        )
                    )

                    Toast.makeText(context, "Guardado: Glucosa = $lastGlucosaValue mg/dl", Toast.LENGTH_SHORT).show()

                    val low = number <= 69.0
                    val high = if (glucosaFasting == true) number >= 131.0 else number >= 181.0
                    var warningText: String? = null
                    when {
                        low -> warningText = "Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico."
                        high -> warningText = "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico."
                    }
                    warningText?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

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
                title = { Text(text = "Medición de Glucosa", fontWeight = FontWeight.Bold) },
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
                HealthRecordCard(label = "Glucosa", onClick = { openDialog() }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(28.dp))
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
                    entry.rawValue1?.let { v ->
                        val low = v < 70.0
                        val high = if (entry.isFasting == true) v >= 126.0 else v >= 180.0
                        if (low) {
                            dangerous = true
                            text = "Glucosa baja (hipoglucemia)."
                        } else if (high) {
                            dangerous = true
                            text = "Glucosa alta (hiperglucemia)."
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
                    title = { Text(text = "Ingresar Glucosa", color = Color.Black) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = glucosaValue,
                                onValueChange = { newValue ->
                                    glucosaValue = if (newValue.length <= 3) newValue else newValue.substring(0, 3)
                                    glucosaError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = glucosaFasting == true, onClick = { glucosaFasting = true })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Ayuno")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(selected = glucosaFasting == false, onClick = { glucosaFasting = false })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "No ayuno")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Fecha registro", fontSize = 15.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = dateFormatter.format(Date(glucosaDateTime)),
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 18.sp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val c = glucosaCalendar
                                        android.app.DatePickerDialog(context, { _, y, m, d ->
                                            c.set(java.util.Calendar.YEAR, y)
                                            c.set(java.util.Calendar.MONTH, m)
                                            c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                            glucosaDateTime = c.timeInMillis
                                        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = glucosaCalendar
                                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                                c.set(java.util.Calendar.YEAR, y)
                                                c.set(java.util.Calendar.MONTH, m)
                                                c.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                glucosaDateTime = c.timeInMillis
                                            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
                                        }) {
                                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Fecha")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Hora", fontSize = 15.sp, color = Color.Black)
                                OutlinedButton(onClick = { showGlucosaTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = timeFormatterOnly.format(Date(glucosaDateTime)), fontSize = 18.sp)
                                }
                            }
                            if (glucosaError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = glucosaError ?: "", color = Color.Red, fontSize = 12.sp)
                            } else if (glucosaFasting == null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Seleccione Ayuno o No Ayuno", color = Color.Red, fontSize = 12.sp)
                            } else if (lastGlucosaValue.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val fastingSuffix = if (lastGlucosaFasting) " · Ayuno" else ""
                                Text(text = "Último: $lastGlucosaValue mg/dl · $lastGlucosaTime$fastingSuffix", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (glucosaValue.isBlank()) {
                                    glucosaError = "Por favor ingrese un valor"
                                    return@TextButton
                                }
                                val number = glucosaValue.replace(',', '.').toDoubleOrNull()
                                if (number == null) {
                                    glucosaError = "Ingrese un número válido"
                                    return@TextButton
                                }
                                if (glucosaFasting == null) {
                                    glucosaError = "Seleccione si está en ayuno o no"
                                    return@TextButton
                                }
                                if (number < 10.0) {
                                    glucosaError = "Valor imposible: Glucosa demasiado baja para ser plausible."
                                    return@TextButton
                                } else if (number > 1000.0) {
                                    glucosaError = "Valor imposible: Glucosa extremadamente alta para ser plausible."
                                    return@TextButton
                                }
                                glucosaError = null
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
