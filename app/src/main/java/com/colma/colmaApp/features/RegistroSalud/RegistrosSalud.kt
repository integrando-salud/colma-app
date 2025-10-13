package com.colma.colmaApp.features.RegistroSalud

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // in-memory history (newest first)
    val history = remember { mutableStateListOf<HealthEntry>() }

    val timeFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    fun formattedNow(): String = timeFormatter.format(Date())

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
                        val millis = parseDateTimeToMillis(g.fecha_registro) ?: System.currentTimeMillis()

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
                    val newestRecord = all.maxByOrNull { parseDateTimeToMillis(it.fecha_registro) ?: 0L }
                    newestRecord?.let { g ->
                        lastGlucosaValue = g.valorMgdl?.let {
                            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                        } ?: ""
                        val millis = parseDateTimeToMillis(g.fecha_registro) ?: System.currentTimeMillis()
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
                        val millis = parseDateTimeToMillis(p.fecha_registro) ?: System.currentTimeMillis()
                        val syst = fmt(p.sistolica)
                        val dia = fmt(p.diastolica)
                        val valueText = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia mmHg" else ""
                        HealthEntry("Presion", valueText, millis)
                    }.sortedByDescending { it.timestamp }

                    // clear previous Presion entries and add all loaded ones
                    history.removeAll { it.label.startsWith("Presion", ignoreCase = true) || it.label.startsWith("Presión", ignoreCase = true) }
                    history.addAll(0, entries)

                    // update the "last" summary fields from most recent DB record (if present)
                    val newestRecord = all.maxByOrNull { parseDateTimeToMillis(it.fecha_registro) ?: 0L }
                    newestRecord?.let { p ->
                        fun fmtSingle(v: Double?) = when {
                            v == null -> ""
                            v % 1.0 == 0.0 -> v.toInt().toString()
                            else -> String.format(Locale.getDefault(), "%.1f", v)
                        }
                        val syst = fmtSingle(p.sistolica)
                        val dia = fmtSingle(p.diastolica)
                        lastPresionValue = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia" else ""
                        val millis = parseDateTimeToMillis(p.fecha_registro) ?: System.currentTimeMillis()
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
                glucosaFasting = null        // require user selection each time
            }
            "Presion" -> {
                presionSys = ""
                presionDia = ""
                presionError = null
                presionFasting = null
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

                // require fasting choice
                if (glucosaFasting == null) {
                    glucosaError = "Seleccione si está en ayuno o no"
                    return
                }

                coroutineScope.launch {
                    try {
                        val rowId = withContext(Dispatchers.IO) {
                            dbHelper.insertGlucosa(
                                valorMgdl = number,
                                idUsuario = 1,
                                ayuno = glucosaFasting  // pass Boolean? directly
                            )
                        }
                        if (rowId > 0L) {
                            lastGlucosaValue = glucosaValue
                            lastGlucosaTime = formattedNow()
                            val now = System.currentTimeMillis()
                            val suffix = if (glucosaFasting == true) " · Ayuno" else " · Sin ayuno"
                            history.add(0, HealthEntry("Glucosa", "${glucosaValue} mg/dl$suffix", now))
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
                        val rowId = withContext(Dispatchers.IO) { dbHelper.insertPresion(sistolica = sys, diastolica = dia, idUsuario = 1) }
                        if (rowId > 0L) {
                            lastPresionValue = "${sys.toInt()}/${dia.toInt()}"
                            lastPresionTime = formattedNow()
                            val now = System.currentTimeMillis()
                            history.add(0, HealthEntry("Presion", "${sys.toInt()}/${dia.toInt()} mmHg", now))
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
                val now = System.currentTimeMillis()
                history.add(0, HealthEntry("Depresion", depresionValue, now))
                Toast.makeText(context, "Guardado: Depresion", Toast.LENGTH_SHORT).show()
                closeDialog()
            }
        }
    }

    // UI
    Scaffold(
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
                            .padding(top = 40.dp),
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
                                .height(56.dp),
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
                                .height(56.dp),
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
                            onClick = { screen = RSscreen.Depresion },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(56.dp),
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
                                    text = "Depresion",
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
                            .padding(top = 12.dp),
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
                            .padding(top = 12.dp),
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
                            .padding(top = 12.dp),
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
                                    Text("Actualizar")
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
                                        placeholder = { Text(text = "Ej: 100") },
                                        trailingIcon = {
                                            Text(text = "mmHg", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                        },
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
                                    Text("Actualizar")
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
                // Build a helpful message depending on what the user is about to save
                val confirmMessage = when (confirmLabel) {
                    "Glucosa" -> {
                        val v = if (glucosaValue.isBlank()) "(sin valor)" else "${glucosaValue} mg/dl"
                        val fast = when (glucosaFasting) {
                            true -> " · Ayuno"
                            false -> " · No ayuno"
                            null -> "" // nothing selected
                        }
                        "¿Está seguro que desea guardar Glucosa: $v$fast ?"
                    }
                    "Presion" -> {
                        val syst = presionSys.ifBlank { "(sin valor)" }
                        val dia = presionDia.ifBlank { "(sin valor)" }
                        "¿Está seguro que desea guardar Presión: $syst / $dia mmHg?"
                    }
                    else -> "¿Está seguro que desea guardar los datos?"
                }

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
                        Text(
                            text = confirmMessage,
                            color = colorResource(R.color.black)

                        )
                           },
                    confirmButton = {
                        TextButton(onClick = {
                            // user confirmed -> perform the save
                            confirmSaveDialogOpen = false
                            // handleSave will validate and insert and close the input dialog on success
                            handleSave()
                        }) {
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

            Text(text = entry.value, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = valueColor)
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