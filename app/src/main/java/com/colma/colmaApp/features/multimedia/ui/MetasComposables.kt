package com.colma.colmaApp.features.multimedia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.utils.varios.CardThatOpensDateTimeDialog
import com.colma.colmaApp.utils.varios.CardThatOpensEditDialog
import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasContent(
    metasId: String,
    metasOptions: List<String>? = null,
    currentWeekId: Int,
    currentSessionId: Int,
    tratamientoElementoId: Int? = null,
    onFinish: () -> Unit,
    navigationButtons: @Composable () -> Unit = {}
) {

    val treatmentType = try {
        TreatmentType.valueOf(metasId.uppercase(Locale.ROOT))
    } catch (e: Exception) {
        TreatmentType.HTA
    }
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val dbHelper = remember { DbHelper(context) }
    val scope = rememberCoroutineScope()
    val dbDateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    val actividad = rememberSaveable { mutableStateOf<String?>(null) }
    val conQuien = rememberSaveable { mutableStateOf<String?>(null) }
    val donde = rememberSaveable { mutableStateOf<String?>(null) }
    val today = Calendar.getInstance()
    val todayString = dbDateTimeFormat.format(today.time)
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
    val fecha = rememberSaveable { mutableStateOf(dbDateTimeFormat.format(tomorrow.time)) }
    val fechaEdicion = rememberSaveable { mutableStateOf(todayString) }
    var showSummaryDialog by rememberSaveable { mutableStateOf(false) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var summaryMessage by rememberSaveable { mutableStateOf("") }
    var shouldExecuteFinalAction by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var isBlockedByFutureGoal by rememberSaveable { mutableStateOf(false) }
    var blockedWeekNumber by rememberSaveable { mutableIntStateOf(-1) }
    var isBlockedByCompletedGoalThisWeek by rememberSaveable { mutableStateOf(false) }
    var isBlockedByExistingGoalThisWeek by rememberSaveable { mutableStateOf(false) }
    var isBlockedByActiveGoalInAnotherWeek by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val userId = SessionManager.getUserId() ?: 1
            val highestWeek = dbHelper.getHighestWeekWithGoal(userId, treatmentType.name.lowercase())
            val hasCompletedThisWeek = dbHelper.hasCompletedGoalForWeek(userId, currentWeekId, treatmentType.name.lowercase())
            val hasGoalThisWeek = dbHelper.hasGoalForWeek(userId, currentWeekId, treatmentType.name.lowercase())
            val pendingFutureMetasCount = dbHelper.getPendingFutureMetasCount(userId)

            if (hasGoalThisWeek) {
                val uniqueKey = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils.generateMultimediaUniqueKey(
                    type = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS,
                    title = "Metas",
                    contentAssetId = null,
                    metasId = metasId,
                    treatmentId = treatmentType.name,
                    weekId = currentWeekId.toLong(),
                    sessionId = currentSessionId.toLong()
                )
                dbHelper.upsertMultimediaRead(
                    idUsuario = userId,
                    treatmentId = treatmentType.name,
                    weekId = currentWeekId,
                    sessionId = currentSessionId,
                    multimediaType = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS.name,
                    multimediaUniqueKey = uniqueKey
                )
            }

            withContext(Dispatchers.Main) {
                if (highestWeek != -1 && currentWeekId < highestWeek) {
                    isBlockedByFutureGoal = true
                    blockedWeekNumber = highestWeek
                } else if (hasCompletedThisWeek) {
                    isBlockedByCompletedGoalThisWeek = true
                } else if (hasGoalThisWeek) {
                    isBlockedByExistingGoalThisWeek = true
                } else if (pendingFutureMetasCount > 0) {
                    isBlockedByActiveGoalInAnotherWeek = true
                }
            }
        }
    }

    if (isBlockedByFutureGoal) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("No puedes crear metas para esta semana") },
            text = {
                Column {
                    Text("Ya creaste una meta para la **Semana $blockedWeekNumber**.")
                    Spacer(Modifier.height(8.dp))
                    Text("Por favor crea metas posteriores a esta semana.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return
    }

    if (isBlockedByCompletedGoalThisWeek) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("Meta ya completada") },
            text = {
                Column {
                    Text("Ya completaste una meta para esta semana.")
                    Spacer(Modifier.height(8.dp))
                    Text("Por favor avanza a la siguiente semana para crear una nueva meta.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return
    }

    if (isBlockedByExistingGoalThisWeek) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("Meta ya seleccionada") },
            text = {
                Column {
                    Text("Ya tienes una meta seleccionada para esta semana.")
                    Spacer(Modifier.height(8.dp))
                    Text("Puedes consultar su avance o marcarla como completada desde el menú principal.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return
    }

    if (isBlockedByActiveGoalInAnotherWeek) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text("Meta activa en curso") },
            text = {
                Column {
                    Text("Ya tienes una meta activa pendiente.")
                    Spacer(Modifier.height(8.dp))
                    Text("Debes completarla antes de fijar una nueva meta.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    )
                ) {
                    Text("Volver")
                }
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlexibleFooterColumn(
                minHeight = screenHeight,
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val (fallbackActividades, conQuienOptions) = getMetasOptions(treatmentType)
                        val actividades = if (!metasOptions.isNullOrEmpty()) metasOptions else fallbackActividades
                        CardThatOpensSelectionDialog(
                            title = "¿Qué actividad deseas hacer?",
                            items = actividades,
                            label = { it },
                            onItemSelected = { actividad.value = it }
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        CardThatOpensSelectionDialog(
                            title = "¿Con quién la harás? (Opcional)\n",
                            items = conQuienOptions,
                            label = { it },
                            onItemSelected = { conQuien.value = it }
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        CardThatOpensEditDialog(
                            title = "¿Dónde la harás? (Opcional)",
                            placeholder = "Escribe el lugar...",
                            onSave = { donde.value = it }
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        CardThatOpensDateTimeDialog(
                            title = "Fecha límite",
                            initialMillis = tomorrow.timeInMillis,
                            showTime = false,
                            onDateTimeSelected = {
                                it.set(Calendar.SECOND, 0)
                                fecha.value = dbDateTimeFormat.format(it.time)
                            }
                        )
                    }
                },
                footer = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                val missing = buildList {
                                    if (actividad.value.isNullOrBlank()) add("Actividad")
                                    if (fecha.value.isNullOrBlank()) add("Fecha Objetivo")
                                    if (fechaEdicion.value.isNullOrBlank()) add("Fecha de Creación")
                                }
                                shouldExecuteFinalAction = missing.isEmpty()
                                if (missing.isNotEmpty()) {
                                    summaryMessage = "Faltan completar los siguientes campos:\n\n• " + missing.joinToString("\n• ")
                                } else {
                                    val fObj = dbDateTimeFormat.parse(fecha.value)?.let { displayDateFormat.format(it) } ?: fecha.value
                                    summaryMessage = "• Actividad: ${actividad.value}\n• ¿Con quién?: ${conQuien.value ?: "No especificado"}\n• ¿Dónde?: ${donde.value ?: "No especificado"}\n• Fecha límite: $fObj"
                                }
                                showSummaryDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.contraste),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Guardar", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }

                        Spacer(Modifier.height(16.dp))
                        navigationButtons()
                    }
                }
            )
        }

        if (showSummaryDialog) {
            CustomAlertDialog(
                show = showSummaryDialog,
                onDismissRequest = { showSummaryDialog = false },
                title = if (shouldExecuteFinalAction) "Confirmar meta" else "Faltan datos",
                description = summaryMessage,
                yesLabel = if (shouldExecuteFinalAction) "Guardar" else "Entendido",
                noLabel = if (shouldExecuteFinalAction) "Cancelar" else "",
                onYes = {
                    if (shouldExecuteFinalAction) {
                        val userId = SessionManager.getUserId() ?: 1
                        scope.launch {
                            val newMeta = RegistrosSaludModelos.MetaRecord(
                                id_meta = 0,
                                id_usuario = userId,
                                semana = currentWeekId,
                                sesion = currentSessionId,
                                tipo_actividad = treatmentType.name.lowercase(),
                                actividad = actividad.value,
                                lugar = donde.value,
                                acompañante = conQuien.value,
                                fecha_regisitro = null,
                                fecha_edicion = fechaEdicion.value,
                                fecha_sincronizacion = null,
                                meta_cumplida = 0,
                                fecha_objetivo = fecha.value,
                                tratamiento_elemento_id = tratamientoElementoId
                            )
                            val result = withContext(Dispatchers.IO) {
                                val hWeek = dbHelper.getHighestWeekWithGoal(userId, treatmentType.name.lowercase())
                                if (hWeek != -1 && currentWeekId < hWeek) {
                                    -3L
                                } else if (dbHelper.getPendingFutureMetasCount(userId) > 0) {
                                    -2L
                                } else {
                                    dbHelper.insertMeta(newMeta)
                                }
                            }
                            if (result > 0L) {
                                withContext(Dispatchers.IO) {
                                    val uniqueKey = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils.generateMultimediaUniqueKey(
                                        type = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS,
                                        title = "Metas",
                                        contentAssetId = null,
                                        metasId = metasId,
                                        treatmentId = treatmentType.name,
                                        weekId = currentWeekId.toLong(),
                                        sessionId = currentSessionId.toLong()
                                    )
                                    dbHelper.upsertMultimediaRead(
                                        idUsuario = userId,
                                        treatmentId = treatmentType.name,
                                        weekId = currentWeekId,
                                        sessionId = currentSessionId,
                                        multimediaType = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS.name,
                                        multimediaUniqueKey = uniqueKey,
                                        tratamientoMultimediaId = tratamientoElementoId,
                                        tratamientoElementoId = tratamientoElementoId
                                    )
                                }

                                val scheduledMeta = newMeta.copy(id_meta = result)
                                com.colma.colmaApp.utils.notifications.NotificationHelper.scheduleGoalNotification(context, scheduledMeta)
                                withContext(Dispatchers.Main) {
                                    showSuccessDialog = true
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    errorMessage = if (result == -3L) {
                                        "Ya tienes una meta creada para la Semana ${dbHelper.getHighestWeekWithGoal(userId, treatmentType.name.lowercase())}. No puedes crear metas para semanas anteriores."
                                    } else if (result == -2L) {
                                        "Ya tienes una meta activa."
                                    } else {
                                        "Error al guardar la meta"
                                    }
                                    showErrorDialog = true
                                }
                            }
                        }
                    }
                    showSummaryDialog = false
                },
                onNo = { showSummaryDialog = false }
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false; onFinish() },
                title = { Text("La meta se ha guardado exitosamente") },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false; onFinish() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.principal_boton),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Guardar")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.rojo_boton),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Guardar")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

fun getMetasOptions(treatmentType: TreatmentType): Pair<List<String>, List<String>> {
    val conQuienOptions = listOf("Solo/a", "Familiar", "Pareja", "Amigo/a", "Vecino/a", "Mascota")
    return when (treatmentType) {
        TreatmentType.DIABETES -> Pair(
            listOf(
                "Ordenar la cocina o una habitación.",
                "Ir de compras o ir al mercado.",
                "Hacer un trámite pendiente",
                "Revisar mis documentos personales o médicos.",
                "Limpiar un espacio de la casa.",
                "Reparar o arreglar algo roto.",
                "Lavar o planchar ropa acumulada.",
                "Ir al banco, correo u oficina municipal.",
                "Organizar la semana."
            ),
            conQuienOptions
        )
        TreatmentType.HTA -> Pair(
            listOf(
                "Subir escaleras en lugar de usar el ascensor.",
                "Bajar una parada de colectivo antes.",
                "Bailar una canción en casa.",
                "Caminar al mercado.",
                "Pasear en bicicleta.",
                "Jugar algún deporte recreativo."
            ),
            conQuienOptions
        )
        TreatmentType.DBT_HTA -> Pair(
            listOf(
                "Subir escaleras en lugar de usar el ascensor.",
                "Bajar una parada de colectivo antes.",
                "Bailar una canción en casa.",
                "Caminar al mercado.",
                "Pasear en bicicleta.",
                "Ordenar la cocina o una habitación.",
                "Ir de compras o ir al mercado.",
                "Hacer un trámite pendiente",
                "Limpiar un espacio de la casa.",
                "Organizar la semana."
            ),
            conQuienOptions
        )
    }
}
