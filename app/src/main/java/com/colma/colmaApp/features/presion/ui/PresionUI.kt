package com.colma.colmaApp.features.presion.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.colma.colmaApp.features.presion.data.PresionRepositoryImpl
import com.colma.colmaApp.features.presion.viewmodel.PresionEvent
import com.colma.colmaApp.features.presion.viewmodel.PresionUiState
import com.colma.colmaApp.features.presion.viewmodel.PresionViewModel
import com.colma.colmaApp.features.presion.viewmodel.PresionViewModelFactory
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

class PresionUI : ComponentActivity() {
    private val viewModel: PresionViewModel by viewModels {
        PresionViewModelFactory(PresionRepositoryImpl(applicationContext))
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
                        onDismissRequest = { viewModel.onEvent(PresionEvent.DismissWarningDialog) },
                        title = { Text("Advertencia") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(uiState.warningDialog.message)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onEvent(PresionEvent.DismissWarningDialog) },
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
                        onDismissRequest = { viewModel.onEvent(PresionEvent.DismissSuccessDialog) },
                        title = { Text("Éxito") },
                        text = { Text("Registro guardado con éxito.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onEvent(PresionEvent.DismissSuccessDialog) },
                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text("Cerrar")
                            }
                        }
                    )
                }

                PresionScreen(
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
fun PresionScreen(
    uiState: PresionUiState,
    onEvent: (PresionEvent) -> Unit,
    onBack: () -> Unit
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatterOnly = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    var showPresionTimePicker by remember { mutableStateOf(false) }

    val measurementCalendar = remember { Calendar.getInstance() }
    measurementCalendar.timeInMillis = uiState.measurementTime

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirmar Registro") },
            text = { Text("¿Está seguro de que desea guardar este registro de presión?") },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(PresionEvent.SavePresionClicked)
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
        show = showPresionTimePicker,
        onDismiss = { showPresionTimePicker = false },
        onTimeSelected = { hour, minute ->
            onEvent(PresionEvent.TimeChanged(hour, minute))
            showPresionTimePicker = false
        },
        initialHour = measurementCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = measurementCalendar.get(Calendar.MINUTE)
    )

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
                HealthRecordCard(label = "Presión", onClick = { onEvent(PresionEvent.AddPresionClicked) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(18.dp))
                HistoryCard(
                    history = uiState.history,
                    onClear = { /* TODO */ },
                    onEntryClick = {
                        onEvent(PresionEvent.ShowDetails(it))
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                    showTitle = false,
                    showLabel = false
                )
            }

            if (uiState.isDetailsDialogVisible) {
                val entry = uiState.selectedEntry!!
                val (isDangerous, showWarningBlock, warningText) = remember(entry, uiState.userPaisId) {
                    var high = false
                    var severeHigh = false
                    var severeLow = false
                    var text: String? = null
                    val s = entry.rawValue1
                    val d = entry.rawValue2
                    if (s != null && d != null) {
                        severeHigh = s >= 180 || d >= 120
                        severeLow = s <= 90
                        high = s >= 140 || d >= 90
                        
                        if (severeHigh) {
                            text = getSevereHighPressureText(uiState.userPaisId)
                        } else if (severeLow) {
                            text = getSevereLowPressureText(uiState.userPaisId)
                        } else if (high) {
                            text = getHighPressureText(uiState.userPaisId)
                        }
                    }
                    val dangerous = high || severeLow
                    val showWarning = (severeHigh || severeLow || high) && text != null
                    Triple(dangerous, showWarning, text)
                }
                
                val valueTextColor = if (isDangerous) Color.Red else Color(0xFF008000)

                AlertDialog(
                    onDismissRequest = { onEvent(PresionEvent.DismissDetailsDialog) },
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
                                        color = valueTextColor,
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
                            if (showWarningBlock && warningText != null) {
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
                            onClick = { onEvent(PresionEvent.DismissDetailsDialog) },
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
                    onDismissRequest = { onEvent(PresionEvent.DismissFormDialog) },
                    title = { Text(text = "Registrar presión", color = Color.Black) },
                    text = {
                        Column {
                            Text(text = "Ingresar valor mayor", fontSize = 15.sp, color = Color.Black)
                            OutlinedTextField(
                                value = uiState.sistolica,
                                onValueChange = { onEvent(PresionEvent.SistolicaChanged(it)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Ingresar valor menor", fontSize = 15.sp, color = Color.Black)
                            OutlinedTextField(
                                value = uiState.diastolica,
                                onValueChange = { onEvent(PresionEvent.DiastolicaChanged(it)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(text = "") },
                                textStyle = TextStyle(color = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Fecha de registro", fontSize = 15.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = dateFormatter.format(Date(uiState.measurementTime)),
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 18.sp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val c = measurementCalendar
                                        android.app.DatePickerDialog(context, { _, y, m, d ->
                                            onEvent(PresionEvent.DateChanged(y,m,d))
                                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                                            .apply { datePicker.maxDate = System.currentTimeMillis() }
                                            .show()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = measurementCalendar
                                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                                onEvent(PresionEvent.DateChanged(y,m,d))
                                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                                                .apply { datePicker.maxDate = System.currentTimeMillis() }
                                                .show()
                                        }) { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Fecha") }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Hora", fontSize = 15.sp, color = Color.Black)
                                OutlinedButton(onClick = { showPresionTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
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
                            onClick = { onEvent(PresionEvent.DismissFormDialog) },
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
 * Helper function to generate nationality-based high pressure warning text.
 * Used in the details dialog for values between 140-179 / 90-119.
 */
private fun getHighPressureText(paisId: Int?): String {
    return when (paisId) {
        else -> {
            "La medición de presión que registraste es alta."
        }
    }
}

/**
 * Helper function to generate nationality-based severe high pressure warning text.
 * Used in both the warning dialog and the details dialog.
 */
private fun getSevereHighPressureText(paisId: Int?): String {
    return when (paisId) {
        1 -> {
            "Los valores de presión que ingresaste son altos, es necesario que consultes a una guardia/ Urgencias para recibir atención médica.\n" +
                    "También podés llamar al 107."
        }
        2 -> {
            "La medición de presión que registraste es alta, es importante que acudas a emergencia para recibir atención médica."
        }
        else -> {
            "Advertencia: Presión arterial severamente alta. Por favor consulte con un médico urgentemente."
        }
    }
}

/**
 * Helper function to generate nationality-based severe low pressure warning text.
 * Used in both the warning dialog and the details dialog.
 */
private fun getSevereLowPressureText(paisId: Int?): String {
    return when (paisId) {
        1 -> {
            "Los valores de presión que ingresaste son bajos, si tenés algún síntoma de malestar o desmayo es importante que consultes a una guardia para recibir atención médica.\n" +
                    "También podés llamar al 107."
        }
        2 -> {
            "La medición de presión que registraste es baja, si sientes malestar físico o has tenido un desmayo, es importante que vayas a tu centro de salud o a emergencia para recibir atención médica."
        }
        else -> {
            "Advertencia: Presión arterial severamente baja. Por favor consulte con un médico urgentemente."
        }
    }
}