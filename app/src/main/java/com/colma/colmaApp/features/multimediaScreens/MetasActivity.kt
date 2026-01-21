package com.colma.colmaApp.features.multimediaScreens

import com.colma.colmaApp.utils.varios.CardThatOpensEditDialog
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.CardThatOpensDateTimeDialog
import java.text.SimpleDateFormat
import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog
import java.util.Calendar
import java.util.Locale
import com.colma.colmaApp.features.sesiones.MultimediaSelector.TreatmentType

const val METAS_TYPE_HTA = "hta"
const val METAS_TYPE_DIABETES = "diabetes"

class MetasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        val sessionId = intent.getLongExtra("sessionId", -1L)
        val sessionTitle = intent.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent.getLongExtra("subSessionId", -1L)
        val subSessionTitle = intent.getStringExtra("subSessionTitle") ?: ""
        val treatmentTypesString = intent.getStringExtra("treatmentTypes") ?: ""
        val currentMultimediaIndex = intent.getIntExtra("currentMultimediaIndex", -1)
        val metasTypeString = intent.getStringExtra("metasType")

        val treatmentType = if (metasTypeString != null) {
            try {
                TreatmentType.valueOf(metasTypeString.uppercase(Locale.ROOT))
            } catch (e: IllegalArgumentException) {
                TreatmentType.HTA // Default to HTA if invalid string
            }
        } else {
            TreatmentType.HTA // Default to HTA if no extra is passed
        }

        setContent {
            ColmaAppTheme {
                MetasScreen(
                    treatmentType = treatmentType,
                    onBack = { finish() },
                    onFinish = {
                        navigateToNextOrFinish(
                            activity = this@MetasActivity,
                            sessionId = sessionId,
                            sessionTitle = sessionTitle,
                            subSessionId = subSessionId,
                            subSessionTitle = subSessionTitle,
                            treatmentTypesString = treatmentTypesString,
                            currentMultimediaIndex = currentMultimediaIndex
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(treatmentType: TreatmentType, onBack: () -> Unit = {}, onFinish: () -> Unit = {}) {

    val context = LocalContext.current
    val dbHelper = remember { DBhelper(context) }
    val scope = rememberCoroutineScope()

    val dbDateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }

    val tipoActividad = rememberSaveable { mutableStateOf<String?>(null) }
    val actividad = rememberSaveable { mutableStateOf<String?>(null) }
    val conQuien = rememberSaveable { mutableStateOf<String?>(null) }
    val donde = rememberSaveable { mutableStateOf<String?>(null) }

    // Calculate default dates
    val today = Calendar.getInstance()
    val todayString = dbDateTimeFormat.format(today.time)

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }

    val fecha = rememberSaveable { mutableStateOf(dbDateTimeFormat.format(tomorrow.time)) }
    val fechaEdicion = rememberSaveable { mutableStateOf(todayString) }

    var showSummaryDialog by rememberSaveable { mutableStateOf(false) }
    var summaryMessage by rememberSaveable { mutableStateOf("") }

    var shouldExecuteFinalAction by rememberSaveable { mutableStateOf(false) }

    val topBarTitle = remember(treatmentType) {
        when (treatmentType) {
            TreatmentType.DIABETES -> "Metas Diabetes"
            else -> "Metas HTA"
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle, color = colorResource(R.color.white)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = colorResource(R.color.background)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.principal))
                )
            },

            bottomBar = {

            },
            containerColor = colorResource(R.color.white),
            content = { innerPadding ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 84.dp), // Adjust this padding to accommodate the fixed button
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val (actividades, conQuienOptions) = getMetasOptions(treatmentType)

                        CardThatOpensSelectionDialog(
                            title = "Tipo de actividad",
                            description = "Seleccione el tipo de actividad para su meta.",
                            items = listOf("Actividad física"),
                            label = { it },
                            onItemSelected = { chosen ->
                                tipoActividad.value = chosen
                            }
                        )

                        Spacer(modifier = Modifier.padding(8.dp))

                        CardThatOpensSelectionDialog(
                            title = "Actividad",
                            description = "Seleccione la actividad concreta que desee realizar.",
                            items = actividades,
                            label = { it },
                            onItemSelected = { chosen ->
                                actividad.value = chosen
                            }
                        )

                        Spacer(modifier = Modifier.padding(8.dp))

                        CardThatOpensSelectionDialog(
                            title = "Con quien",
                            description = "Quien te acompañará en esta actividad?",
                            items = conQuienOptions,
                            label = { it },
                            onItemSelected = { chosen ->
                                conQuien.value = chosen
                            }
                        )

                        Spacer(modifier = Modifier.padding(8.dp))

                        CardThatOpensEditDialog(
                            title = "Donde?",
                            description = "Describe brevemente en donde lo harás",
                            placeholder = "Escribe el lugar...",
                            onSave = { newDesc ->
                                donde.value = newDesc
                            }
                        )

                        Spacer(modifier = Modifier.padding(8.dp))

                        CardThatOpensDateTimeDialog(
                            title = "Fecha Objetivo",
                            description = "Selecciona la fecha límite para tu meta.",
                            initialMillis = today.timeInMillis,
                            onDateTimeSelected = { selectedCal ->
                                selectedCal.set(Calendar.SECOND, 0)
                                fecha.value = dbDateTimeFormat.format(selectedCal.time)
                            }
                        )

                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter), // Align to the bottom center
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                val missing = buildList {
                                    if (tipoActividad.value.isNullOrBlank()) add("Tipo de actividad")
                                    if (actividad.value.isNullOrBlank()) add("Actividad")
                                    if (conQuien.value.isNullOrBlank()) add("Con quién")
                                    if (donde.value.isNullOrBlank()) add("Dónde")
                                    if (fecha.value.isNullOrBlank()) add("Fecha Objetivo")
                                    if (fechaEdicion.value.isNullOrBlank()) add("Fecha de Creación")
                                }
                                shouldExecuteFinalAction = missing.isEmpty()
                                if (missing.isNotEmpty()) {
                                    summaryMessage =
                                        "Faltan completar los siguientes campos:\n\n• " +
                                                missing.joinToString("\n• ")
                                } else {
                                    val fechaObjetivoForDisplay = dbDateTimeFormat.parse(fecha.value)?.let { displayDateFormat.format(it) } ?: fecha.value
                                    val fechaCreacionForDisplay = dbDateTimeFormat.parse(fechaEdicion.value)?.let { displayDateFormat.format(it) } ?: fechaEdicion.value
                                    summaryMessage = """
                                                            Resumen de registro:
                                                            • Tipo de actividad: ${tipoActividad.value}

                                                            • Actividad: ${actividad.value}

                                                            • Con quién: ${conQuien.value}

                                                            • Dónde: ${donde.value}

                                                            • Fecha Objetivo: $fechaObjetivoForDisplay
                                                            
                                                            • Fecha de Creación: $fechaCreacionForDisplay
                                                        """.trimIndent()
                                }
                                showSummaryDialog = true
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.principal),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
                        }
                        if (showSummaryDialog) {
                            CustomAlertDialog(
                                show = showSummaryDialog,
                                onDismissRequest = { showSummaryDialog = false },
                                title = "Resultado",
                                description = summaryMessage,
                                yesLabel = "OK",
                                onYes = {
                                    if (shouldExecuteFinalAction) {
                                        scope.launch {
                                            val newMeta = RegistrosSaludModelos.MetaRecord(
                                                id_meta = 0,
                                                id_usuario = 1,
                                                semana = null,
                                                sesion = null,
                                                tipo_actividad = tipoActividad.value,
                                                actividad = actividad.value,
                                                lugar = donde.value,
                                                acompañante = conQuien.value,
                                                fecha_regisitro = null,
                                                fecha_edicion = fechaEdicion.value,
                                                fecha_sincronizacion = null,
                                                meta_cumplida = 0,
                                                fecha_objetivo = fecha.value
                                            )
                                            val result = withContext(Dispatchers.IO) {
                                                dbHelper.insertMeta(newMeta)
                                            }
                                            if (result != -1L) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Meta guardada exitosamente", Toast.LENGTH_SHORT).show()
                                                    onFinish()
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Error al guardar la meta", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    showSummaryDialog = false
                                }
                            )
                        }
                    }

                }


            }
        )
    }
}

fun getMetasOptions(treatmentType: TreatmentType): Pair<List<String>, List<String>> {
    val conQuienOptions = listOf("Solo/a", "Familiar", "Pareja", "Amigo/a", "Vecino/a", "Mascota")

    return when (treatmentType) {
        TreatmentType.DIABETES -> Pair(
            listOf(
                "Ordenar la cocina o una habitación.",
                "Ir de compras o ir al mercado.",
                "Hacer un trámite pendiente (ej, hacer una llamada, realizar una consulta, sacar un turno)",
                "Revisar mis documentos personales o médicos.",
                "Limpiar un espacio de la casa que he dejado pendiente.",
                "Reparar o arreglar algo roto o que no funciona.",
                "Lavar o planchar ropa acumulada.",
                "Ir al banco, correo u oficina municipal.",
                "Organizar la semana con una lista de pendientes."
            ),
            conQuienOptions
        )
        TreatmentType.HTA -> Pair( // Explicitly handle HTA
            listOf(
                "Subir escaleras en lugar de usar el ascensor.",
                "Bajar una parada de colectivo antes para caminar un poco más",
                "Bailar una canción en casa.",
                "Caminar al mercado, centro de salud, casa de un familia",
                "Pasear en bicicleta.",
                "Jugar algún deporte recreativo (fútbol, voley,etc.)."
            ),
            conQuienOptions
        )
    }
}
