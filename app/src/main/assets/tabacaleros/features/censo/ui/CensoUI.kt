package com.codeco.tabacaleros.features.censo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.DNI.ui.dniUI
import com.codeco.tabacaleros.features.censo.data.CensoRepositoryImpl
import com.codeco.tabacaleros.features.censo.viewmodel.CensoEvent
import com.codeco.tabacaleros.features.censo.viewmodel.CensoUiState
import com.codeco.tabacaleros.features.censo.viewmodel.CensoViewModel
import com.codeco.tabacaleros.features.censo.viewmodel.CensoViewModelFactory
import com.codeco.tabacaleros.features.utils.configureSystemBars
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesDesarrolloService
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesService
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadDesarrolloItem
import com.codeco.tabacaleros.features.Menu.ui.Menu
import java.text.NumberFormat

fun Int.formatThousands(): String {
    return NumberFormat.getNumberInstance(java.util.Locale.getDefault()).format(this)
}

class CensoUI : ComponentActivity() {
    private val viewModel: CensoViewModel by viewModels {
        val sessionManager = SessionManager(this)
        CensoViewModelFactory(
            CensoRepositoryImpl(
                ActividadesDesarrolloService(sessionManager),
                ActividadesService(sessionManager),
                sessionManager
            )
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        val fromConsulta = intent.getBooleanExtra(Menu.EXTRA_FROM_CONSULTA, false)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is CensoViewModel.NavigationEvent.NavigateToDni -> {
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CensoUI, dniUI::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }

            ProducerScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onBack = { finish() },
                fromConsulta = fromConsulta
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerScreen(
    uiState: CensoUiState,
    onEvent: (CensoEvent) -> Unit,
    onBack: () -> Unit,
    fromConsulta: Boolean
) {
    val context = LocalContext.current
    val signatureGreen = colorResource(R.color.principal)

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onEvent(CensoEvent.ErrorMessageShown)
        }
    }

    Box {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Registro Tabacalero", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver atras")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.principal),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Image(
                    painter = painterResource(id = R.drawable.logo_ministerio_tabaco),
                    contentDescription = "Footer",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 5.dp)
                        .height(80.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            containerColor = colorResource(R.color.background),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    uiState.productorInfo?.let { info ->
                        ProducerInfoCard(
                            name = info.nombre,
                            estado = info.estado,
                            dni = info.cuil,
                            modifier = Modifier.fillMaxWidth(),
                            actividadesCount = uiState.actividades.size
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Spacer(Modifier.height(15.dp))
                        }
                        items(items = uiState.actividades, key = { it.id }) { actividad ->
                            val selected = uiState.selectedItemId == actividad.id
                            val isCensado = actividad.snCensado == 1

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .padding(vertical = 6.dp)
                                    .padding(horizontal = 11.dp)
                            ) {
                                FieldItemCard(
                                    id = actividad.id,
                                    title = actividad.empresa.apellidoynombre,
                                    type = actividad.tabacoActividad.descripcion,
                                    plantCount = actividad.cupoPlantas,
                                    selected = selected,
                                    censado = isCensado,
                                    onClick = { onEvent(CensoEvent.ToggleItemSelection(actividad.id)) },
                                    signatureColor = signatureGreen
                                )

                                AnimatedVisibility(
                                    visible = selected,
                                    enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(280)) +
                                            fadeIn(animationSpec = tween(200)),
                                    exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(220)) +
                                            fadeOut(animationSpec = tween(180))
                                ) {
                                    SecondaryMenuCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        actividad = actividad,
                                        signatureColor = colorResource(R.color.white),
                                        inCensar = isCensado,
                                        onEvent = onEvent,
                                        fromConsulta = fromConsulta
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(15.dp))
                        }
                    }

                    Button(
                        onClick = { onEvent(CensoEvent.Submit) },
                        enabled = uiState.actividades.any { it.snCensado == 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.accent),
                            contentColor = Color.White,
                            disabledContainerColor = colorResource(R.color.disabled_button_gray),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = if (uiState.actividades.any { it.snCensado == 1 }) colorResource(R.color.accent) else colorResource(
                                    R.color.disabled_button_gray
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ),
                    ) {
                        Text("Continuar")
                    }

                    if (uiState.showSendConfirmationDialog) {
                        AlertDialog(
                            onDismissRequest = { onEvent(CensoEvent.DismissDialog) },
                            title = { Text("Confirmar Envío") },
                            text = { Text("¿Está seguro que desea enviar los datos de los campos registrados? Una vez enviados no podra cambiarlos") },
                            confirmButton = {
                                TextButton(onClick = { onEvent(CensoEvent.ConfirmSubmission) }) {
                                    Text("Sí")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { onEvent(CensoEvent.DismissDialog) }) {
                                    Text("No")
                                }
                            }
                        )
                    }
                }
            }
        }
        if (uiState.isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(id = R.color.principal))
            }
        }
    }
}

@Composable
fun ProducerInfoCard(
    name: String,
    estado: String?,
    dni: String,
    modifier: Modifier = Modifier,
    actividadesCount: Int
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 15.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.background))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CUIT: $dni",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                estado?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    val (estadoText, backgroundColor) = when (it) {
                        "Normal" -> "Activo" to Color(0xFF4CAF50)
                        "Pre-alta" -> "Pre-alta" to Color(0xFFFFC107) // Amber
                        else -> "Inactivo" to Color.Red
                    }
                    Text(
                        text = estadoText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(backgroundColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Actividades: $actividadesCount",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun FieldItemCard(
    id: Int,
    title: String,
    type: String,
    plantCount: Int,
    selected: Boolean = false,
    censado: Boolean = false,
    onClick: () -> Unit = {},
    signatureColor: Color = Color(0xFF6A994E)
) {
    val backgroundColor by animateColorAsState(targetValue = if (selected) signatureColor else colorResource(R.color.white), label = "")
    val titleColor by animateColorAsState(targetValue = if (selected) Color.White else Color.Black, label = "")
    val subtitleColor by animateColorAsState(targetValue = if (selected) Color(0xCCFFFFFF) else Color.Gray, label = "")
    val dividerColor = subtitleColor.copy(alpha = 0.25f)

    val shape = if (selected) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = dividerColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${plantCount.formatThousands()} plantas · ${type}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            val badgeBg = if (selected) Color.White else Color(0xFFF1F1F1)
            val iconTint = when {
                censado -> colorResource(R.color.accent_light)
                selected -> signatureColor
                else -> Color.Gray
            }

            val iconVector = if (censado) Icons.Filled.Check else Icons.Filled.Close

            val badgeSize = 38.dp
            val iconSize = 24.dp
            val iconScale = if (selected || censado) 1.08f else 1f

            Surface(
                shape = CircleShape,
                color = badgeBg,
                tonalElevation = if (selected) 2.dp else 0.dp,
                shadowElevation = if (selected) 4.dp else 0.dp,
                modifier = Modifier.size(badgeSize)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = if (censado) "Censado" else "No censado",
                        tint = iconTint,
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun SecondaryMenuCard(
    modifier: Modifier = Modifier,
    actividad: ActividadDesarrolloItem,
    signatureColor: Color = colorResource(R.color.white),
    inCensar: Boolean = false,
    onEvent: (CensoEvent) -> Unit,
    fromConsulta: Boolean
) {
    val paleColor = lerp(signatureColor, Color.White, 0.36f)
    val backgroundColor by animateColorAsState(targetValue = paleColor, label = "")
    val textColor = if (backgroundColor.luminance() < 0.5f) Color.White else Color.Black

    var showCuPicker by remember { mutableStateOf(false) }
    var showPlantInput by remember { mutableStateOf(false) }
    var showKgPicker by remember { mutableStateOf(false) }
    var showCensarConfirm by remember { mutableStateOf(false) }

    val plantas = if (actividad.censoPlantasProductor > 0) actividad.censoPlantasProductor else actividad.cupoPlantas
    val cuadros = actividad.censoCuadrosCantidad
    val kilos = actividad.censoKilosEstimadoProductor

    val enabled = !inCensar
    val contentAlpha = if (enabled) 1f else 0.45f

    val baseBtnColors = ButtonDefaults.buttonColors(
        containerColor = colorResource(R.color.background),
        contentColor = textColor,
        disabledContainerColor = Color(0xFFECECEC),
        disabledContentColor = Color(0xFF9E9E9E)
    )

    val censarBtnColors = ButtonDefaults.buttonColors(
        containerColor = colorResource(R.color.accent).copy(alpha = 0.80f),
        contentColor = colorResource(R.color.backgroundLight),
        disabledContainerColor = Color(0xFFBDBDBD),
        disabledContentColor = colorResource(R.color.backgroundLight).copy(alpha = 0.6f)
    )
    val censarLabel = if (inCensar) "Registrado" else "Registrar"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showPlantInput = true },
                    enabled = enabled && !fromConsulta,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp)),
                    colors = baseBtnColors,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                        Icon(
                            painter = painterResource(id = R.drawable.leaf_icon),
                            contentDescription = "Plant icon",
                            tint = colorResource(R.color.principal),
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(contentAlpha),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = plantas.formatThousands(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(contentAlpha),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { showCuPicker = true },
                    enabled = enabled && !fromConsulta,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp)),
                    colors = baseBtnColors,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                        Icon(
                            painter = painterResource(R.drawable.cuadros_logo),
                            contentDescription = "Cuadros",
                            tint = colorResource(R.color.principal),
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(contentAlpha),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cuadros.formatThousands(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(contentAlpha),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showKgPicker = true },
                    enabled = enabled && !fromConsulta,
                    modifier = Modifier
                        .weight(1f)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = baseBtnColors,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                        Icon(
                            painter = painterResource(id = R.drawable.peso_logo),
                            contentDescription = "Peso",
                            tint = colorResource(R.color.principal),
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(contentAlpha),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${kilos.formatThousands()} kg.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(contentAlpha),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (!fromConsulta) {
                    Button(
                        onClick = { showCensarConfirm = true },
                        enabled = !inCensar,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = censarBtnColors,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = censarLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorResource(R.color.backgroundLight),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCensarConfirm) {
        AlertDialog(
            onDismissRequest = { showCensarConfirm = false },
            title = { Text("Confirmar censado") },
            text = { Text("¿Desea marcar este campo como censado?") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(CensoEvent.MarkAsCensado(actividad.id))
                    showCensarConfirm = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCensarConfirm = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showCuPicker) {
        var tempValue by remember { mutableStateOf(cuadros.coerceAtLeast(1)) }
        val minValue = 1
        val maxValue = 15

        Dialog(onDismissRequest = { showCuPicker = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seleccionar número de cuadros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                this.minValue = minValue
                                this.maxValue = maxValue
                                this.value = tempValue
                                this.wrapSelectorWheel = true
                                setOnValueChangedListener { _, _, newVal ->
                                    tempValue = newVal
                                }
                            }
                        },
                        update = { picker ->
                            if (picker.value != tempValue) picker.value = tempValue
                        },
                        modifier = Modifier.height(150.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCuPicker = false }) {
                            Text(text = "Cancelar", color = Color(0xFF4CAF50))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onEvent(CensoEvent.UpdateCuadros(actividad.id, tempValue))
                            showCuPicker = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal), contentColor = Color.White)) {
                            Text(text = "Aceptar")
                        }
                    }
                }
            }
        }
    }

    if (showPlantInput) {
        var inputText by rememberSaveable { mutableStateOf(plantas.toString()) }
        var inputError by remember { mutableStateOf<String?>(null) }
        val focusManager = LocalFocusManager.current

        Dialog(onDismissRequest = { showPlantInput = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Editar plantas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { new ->
                            inputText = new.filter { it.isDigit() }
                            inputError = null
                        },
                        label = { Text("Cantidad de plantas") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputError != null,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF333333)),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = Color(0xFF689F38),
                            focusedBorderColor = Color(0xFF689F38),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = Color(0xFF689F38),
                            unfocusedLabelColor = Color(0xFF333333)
                        )
                    )

                    if (inputError != null) {
                        Text(text = inputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPlantInput = false }) {
                            Text(text = "Cancelar", color = Color(0xFF4CAF50))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val value = inputText.toIntOrNull()
                            if (value == null || value < 0) {
                                inputError = "Ingrese un número válido"
                            } else {
                                onEvent(CensoEvent.UpdatePlantas(actividad.id, value))
                                focusManager.clearFocus()
                                showPlantInput = false
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal), contentColor = Color.White)) {
                            Text("Aceptar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showKgPicker) {
        var inputKg by rememberSaveable { mutableStateOf(kilos.toString()) }
        var inputKgError by remember { mutableStateOf<String?>(null) }
        val focusManager = LocalFocusManager.current

        Dialog(onDismissRequest = { showKgPicker = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Editar kilogramos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputKg,
                        onValueChange = { new ->
                            val filtered = new.filter { it.isDigit() }
                            inputKg = filtered
                            inputKgError = null
                        },
                        label = { Text("Cantidad (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputKgError != null,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF333333)),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = Color(0xFF689F38),
                            focusedBorderColor = Color(0xFF689F38),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = Color(0xFF689F38),
                            unfocusedLabelColor = Color(0xFF333333)
                        )
                    )

                    if (inputKgError != null) {
                        Text(
                            text = inputKgError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showKgPicker = false }) {
                            Text(text = "Cancelar", color = colorResource(R.color.principal))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = inputKg.toIntOrNull()
                                if (value == null || value < 0) {
                                    inputKgError = "Ingrese un número válido"
                                } else {
                                    onEvent(CensoEvent.UpdateKilos(actividad.id, value))
                                    focusManager.clearFocus()
                                    showKgPicker = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.principal),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Aceptar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SetStatusBarColor(color: Color, darkIcons: Boolean) {
    val activity = LocalActivity.current as Activity
    SideEffect {
        activity.window.statusBarColor = color.toArgb()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = darkIcons
    }
}


