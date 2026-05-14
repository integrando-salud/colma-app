package com.colma.colmaApp.features.glucosa.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.glucosa.data.GlucosaRepositoryImpl
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaEvent
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaUiState
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaViewModel
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaViewModelFactory
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.HealthRecordCard
import com.colma.colmaApp.utils.varios.HistoryCard
import com.colma.colmaApp.utils.varios.TimeInputDialog
import com.colma.colmaApp.utils.varios.formatTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Date and time formatters for the details dialog
private val detailsDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val detailsTimeFormatter = SimpleDateFormat("hh:mm a", Locale("es"))

class GlucosaUI : ComponentActivity() {

    private val viewModel: GlucosaViewModel by viewModels {
        GlucosaViewModelFactory(GlucosaRepositoryImpl(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                if (uiState.warningDialog.show) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(GlucosaEvent.DismissWarningDialog) },
                        title = { Text("Advertencia") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(uiState.warningDialog.message)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onEvent(GlucosaEvent.DismissWarningDialog) },
                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text("Cerrar")
                            }
                        }
                    )
                }

                if (uiState.showSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(GlucosaEvent.DismissSuccessDialog) },
                        title = { Text("Éxito") },
                        text = { Text("Registro guardado con éxito.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onEvent(GlucosaEvent.DismissSuccessDialog) },
                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text("Cerrar")
                            }
                        }
                    )
                }

                GlucosaScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucosaScreen(
    uiState: GlucosaUiState,
    onEvent: (GlucosaEvent) -> Unit,
    onBack: () -> Unit
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    var showGlucosaTimePicker by remember { mutableStateOf(false) }

    val measurementCalendar = remember { Calendar.getInstance() }
    measurementCalendar.timeInMillis = uiState.measurementTime

    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirmar Registro") },
            text = { Text("¿Está seguro de que desea guardar este registro de glucosa?") },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(GlucosaEvent.SaveGlucosaClicked)
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showConfirmationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.rojo_boton), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    TimeInputDialog(
        show = showGlucosaTimePicker,
        onDismiss = { showGlucosaTimePicker = false },
        onTimeSelected = { hour, minute ->
            onEvent(GlucosaEvent.TimeChanged(hour, minute))
            showGlucosaTimePicker = false
        },
        initialHour = measurementCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = measurementCalendar.get(Calendar.MINUTE)
    )

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
                .background(colorResource(id = R.color.background))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HealthRecordCard(label = "Glucosa", onClick = { onEvent(GlucosaEvent.AddGlucosaClicked) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(28.dp))
                HistoryCard(
                    history = uiState.history,
                    onClear = { /* TODO */ },
                    onEntryClick = {
                        onEvent(GlucosaEvent.ShowDetails(it))
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                    showLabel = false,
                    showTitle = false
                )
            }

            if (uiState.isDetailsDialogVisible) {
                val entry = uiState.selectedEntry!!
                val (isDangerous, warningText) = remember(entry, uiState.userPaisId) {
                    var dangerous = false
                    var text: String? = null
                    entry.rawValue1?.let { v ->
                        val low = v < 70.0
                        val high = if (entry.isFasting == true) v >= 126.0 else v >= 180.0
                        val severeHigh = v > 300.0
                        if (low) {
                            dangerous = true
                            text = getLowGlucosaWarningText(entry.isFasting == true, uiState.userPaisId)
                        } else if (severeHigh) {
                            dangerous = true
                            text = getHighGlucosaWarningText(entry.isFasting == true, uiState.userPaisId)
                        } else if (high) {
                            dangerous = true
                            text = "Glucosa alta (hiperglucemia). Por favor, consulte con un médico."
                        }
                    }
                    Pair(dangerous, text)
                }

                AlertDialog(
                    onDismissRequest = { onEvent(GlucosaEvent.DismissDetailsDialog) },
                    title = {
                        Text("Detalle del registro:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    text = {
                        val detailsScrollState = rememberScrollState()
                        val density = LocalDensity.current
                        val containerHeightPx = with(density) { 280.dp.toPx() }
                        val thumbHeightPx = with(density) { 30.dp.toPx() }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp)
                                    .verticalScroll(detailsScrollState)
                            ) {
                            // Valor
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text("Valor:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = entry.value,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Fecha
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text("Fecha:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = detailsDateFormatter.format(Date(entry.timestamp)),
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Hora
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text("Hora:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = detailsTimeFormatter.format(Date(entry.timestamp)).lowercase(),
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Advertencia (if exists)
                            if (isDangerous && warningText != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Advertencia",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Advertencia:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFFE53935)
                                        )
                                        Text(
                                            text = warningText,
                                            fontSize = 14.sp,
                                            color = Color(0xFF212121),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } // Close Column

                        // Thin scrollbar indicator - constrained to container height
                        if (detailsScrollState.maxValue > 0) {
                            val scrollableRange = containerHeightPx - thumbHeightPx
                            val scrollFraction = detailsScrollState.value.toFloat() / detailsScrollState.maxValue
                            val thumbOffsetY = scrollFraction * scrollableRange
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .height(280.dp)
                                    .width(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .offset {
                                            IntOffset(0, thumbOffsetY.toInt())
                                        }
                                        .background(Color(0xFFBDBDBD), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    } // Close Box
                },
                confirmButton = {
                        Button(
                            onClick = { onEvent(GlucosaEvent.DismissDetailsDialog) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Cerrar")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (uiState.isFormVisible) {
                AlertDialog(
                    onDismissRequest = { onEvent(GlucosaEvent.DismissFormDialog) },
                    title = { Text(text = "Registrar glucosa", color = Color.Black) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = uiState.glucosaValue,
                                onValueChange = { onEvent(GlucosaEvent.GlucosaValueChanged(it)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onEvent(GlucosaEvent.FastingChanged(true)) }
                                ) {
                                    RadioButton(
                                        selected = uiState.isFasting == true,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Ayuno")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onEvent(GlucosaEvent.FastingChanged(false)) }
                                ) {
                                    RadioButton(
                                        selected = uiState.isFasting == false,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "No ayuno")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Fecha de registro", fontSize = 15.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = dateFormatter.format(Date(uiState.measurementTime)),
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 18.sp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val c = measurementCalendar
                                        android.app.DatePickerDialog(context, { _, y, m, d ->
                                            onEvent(GlucosaEvent.DateChanged(y, m, d))
                                        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
                                            .apply { datePicker.maxDate = System.currentTimeMillis() }
                                            .show()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = measurementCalendar
                                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                                onEvent(GlucosaEvent.DateChanged(y, m, d))
                                            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
                                                .apply { datePicker.maxDate = System.currentTimeMillis() }
                                                .show()
                                        }) {
                                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Fecha")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Hora de registro", fontSize = 15.sp, color = Color.Black)
                                OutlinedButton(onClick = { showGlucosaTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = timeFormatterOnly.format(Date(uiState.measurementTime)), fontSize = 18.sp)
                                }
                            }
                            if (uiState.formError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = uiState.formError, color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showConfirmationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) { Text("Guardar") }
                    },
                    dismissButton = {
                        Button(
                            onClick = { onEvent(GlucosaEvent.DismissFormDialog) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.rojo_boton), contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
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

/**
 * Helper function to generate nationality-based low glucose warning text.
 * Used in both the warning dialog and the details dialog.
 */
private fun getLowGlucosaWarningText(isFasting: Boolean, paisId: Int?): String {
    return when (paisId) {
        1 -> {
            "Tu nivel de azúcar está bajo.\n" +
                    "Si tenés temblores, sudor frío, mareo o debilidad, segui el consejo del equipo medico: comé un caramelo o tomá un vaso de jugo o gaseosa común.\n" +
                    "Si te seguís sintiendo mal o el valor sigue bajo, consultá con tu médico o andá a una guardia. También podés llamar al 107."
        }
        2 -> {
            if (isFasting) {
                "La medición de azúcar que registraste es baja, vuelve medirte en otro dedo. Si la medición sigue siendo baja, sigue las indicaciones de tu médico para estos casos y saca una cita en tu centro de salud para que te evalúen."
            } else {
                "La medición de azúcar que registraste es baja, vuelve medirte en otro dedo. Si la medición sigue siendo baja, saca una cita con tu médico para que te evalúe."
            }
        }
        else -> {
            "Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico."
        }
    }
}

/**
 * Helper function to generate nationality-based high glucose warning text.
 * Used in both the warning dialog and the details dialog.
 */
private fun getHighGlucosaWarningText(isFasting: Boolean, paisId: Int?): String {
    return when (paisId) {
        1 -> {
            "Tu azúcar está muy alta. Si sentís náuseas, vómitos, dolor abdominal, orinas muy seguido o te sientes muy mal es necesario que consultes a una guardia/ Urgencias.\n" +
                    "También podés llamar al 107."
        }
        2 -> {
            "La medición de azúcar que registraste es alta. Si sientes náuseas, vómitos, dolor abdominal, orinas muy seguido o te sientes muy mal es necesario que acudas a tu centro de salud o a emergencia para que recibas atención médica."
        }
        else -> {
            "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico."
        }
    }
}