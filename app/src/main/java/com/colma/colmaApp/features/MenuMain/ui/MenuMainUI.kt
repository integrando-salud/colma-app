package com.colma.colmaApp.features.MenuMain.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.MenuMain.data.MenuMainRepositoryImpl
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainEvent
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainUiState
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainViewModel
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainViewModelFactory
import com.colma.colmaApp.features.MetasHistory.ui.MetasHistoryUI
import com.colma.colmaApp.features.RegistroSalud.ui.RegistrosSaludUI
import com.colma.colmaApp.features.login.ui.LoginUI
import com.colma.colmaApp.features.sesiones.SesionesMenu.ui.SesionesMenuUI
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import kotlinx.coroutines.launch

class MenuMainUI : ComponentActivity() {

    private val viewModel: MenuMainViewModel by viewModels {
        MenuMainViewModelFactory(
            MenuMainRepositoryImpl(DBhelper(applicationContext))
        )
    }

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
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is MenuMainViewModel.NavigationEvent.NavigateToSessions -> {
                                val intent = Intent(this@MenuMainUI, SesionesMenuUI::class.java).apply {
                                    putExtra("treatmentTypes", "HTA,DIABETES") // TODO: Move to constants/config
                                }
                                startActivity(intent)
                            }
                            is MenuMainViewModel.NavigationEvent.NavigateToHealthRegisters -> {
                                val intent = Intent(this@MenuMainUI, RegistrosSaludUI::class.java)
                                startActivity(intent)
                            }
                            is MenuMainViewModel.NavigationEvent.NavigateToMetasHistory -> {
                                val intent = Intent(this@MenuMainUI, MetasHistoryUI::class.java)
                                startActivity(intent)
                            }
                            is MenuMainViewModel.NavigationEvent.NavigateToLogin -> {
                                val intent = Intent(this@MenuMainUI, LoginUI::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }

                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { message ->
                        snackbarHostState.showSnackbar(message)
                        viewModel.onEvent(MenuMainEvent.SnackbarDismissed)
                    }
                }

                MenuScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    uiState: MenuMainUiState,
    onEvent: (MenuMainEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val signatureColor = colorResource(R.color.principal)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope() // For drawer actions

    if (uiState.showObjectiveDialog && uiState.todaysObjective != null) {
        CustomAlertDialog(
            show = true,
            onDismissRequest = { onEvent(MenuMainEvent.ObjectiveDialogDismissed) },
            title = "Objetivo para Hoy",
            description = "¿Completaste este objetivo?: ${uiState.todaysObjective.actividad ?: "No hay detalles."}",
            onYes = { onEvent(MenuMainEvent.ObjectiveCompleted) },
            onNo = { onEvent(MenuMainEvent.ObjectiveNotCompleted("")) } // Empty string initially for reason
        )
    }

    if (uiState.showReasonDialog) {
        val reasons = listOf(
            "No tenía energía / ánimo para hacerlo",
            "Otras actividades me quitaron tiempo (trabajo, familia, etc.)",
            "Tuve dificultades para hacer la actividad",
            "Tuve un problema de salud / estuve enfermo(a)",
            "La persona con quien iba a hacer la actividad no pudo acompañarme",
            "Me olvidé de hacer la actividad.",
            "otro"
        )
        var selectedReason by remember { mutableStateOf(uiState.selectedReason) }

        AlertDialog(
            onDismissRequest = { onEvent(MenuMainEvent.ObjectiveDialogDismissed) }, // Dismiss reason dialog
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("¿Por qué no completaste el objetivo?") },
            text = {
                LazyColumn {
                    items(reasons) { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason }
                            )
                            Text(text = reason, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedReason.isNotBlank()) {
                            onEvent(MenuMainEvent.ObjectiveNotCompleted(selectedReason))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (uiState.showOtherReasonDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(MenuMainEvent.ObjectiveDialogDismissed) },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Por favor, explica la razón") },
            text = {
                OutlinedTextField(
                    value = uiState.otherReasonInput,
                    onValueChange = { onEvent(MenuMainEvent.OtherReasonChanged(it)) },
                    label = { Text("Razón") }
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(MenuMainEvent.OtherReasonConfirmed) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (uiState.showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(MenuMainEvent.DismissFeedbackDialog) },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            icon = {
                if (uiState.isSuccessFeedback) {
                    CongratulationAnimation()
                }
            },
            title = { Text("Mensaje") },
            text = { Text(uiState.feedbackMessage) },
            confirmButton = {
                Button(
                    onClick = { onEvent(MenuMainEvent.DismissFeedbackDialog) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.systemBarsPadding(),
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(drawerContainerColor = Color.White) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.colma_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .size(200.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(Modifier.height(12.dp))

                            val context = LocalContext.current

                            DrawerItem("Inicio", Icons.Default.Home) {
                                scope.launch { drawerState.close() }
                                // No specific action needed for "Inicio" as it's the current screen
                            }

                            DrawerItem(
                                "Ayuda técnica del promotor de salud",
                                Icons.Default.Call
                            ) {
                                scope.launch { drawerState.close() }
                                // Handle help technical click
                            }

                            DrawerItem("Información Adicional", Icons.Default.Info) {
                                scope.launch { drawerState.close() }
                                // Handle useful links click
                            }

                            DrawerItem("Tutorial de Usuario", Icons.Default.Face) {
                                scope.launch { drawerState.close() }
                                // Handle user tutorial click
                            }

                            Spacer(Modifier.height(28.dp))

                            Button(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onEvent(MenuMainEvent.OnSalirClick)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(R.color.principal),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Text(text = "Cerrar sesión")
                            }

                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Bienvenido ${uiState.userName}", fontWeight = FontWeight.Bold) },
                            actions = {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .height(36.dp)
                                        .clickable { scope.launch { drawerState.open() } }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Menu",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }, colors = TopAppBarDefaults.topAppBarColors(
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
                                .padding(horizontal = 24.dp)
                                .padding(top = 50.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Elige una opcion",
                                fontSize = 25.sp,
                                color = signatureColor,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(55.dp))

                            Button(
                                onClick = { onEvent(MenuMainEvent.OnSessionsClick) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(65.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        clip = false
                                    )
                                    .background(
                                        color = colorResource(R.color.principal),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.sesion_logo),
                                        contentDescription = "Sesiones",
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "Tema de hoy",
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onEvent(MenuMainEvent.OnHealthRegistersClick) },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(65.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F0),
                                    contentColor = Color.Black
                                ),
                                shape = MaterialTheme.shapes.medium,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 10.dp
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.outline_unknown_document_24),
                                        contentDescription = "Registros",
                                        tint = LocalContentColor.current,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "Mis Registros de Salud",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onEvent(MenuMainEvent.OnMetasHistoryClick) },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(65.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F0),
                                    contentColor = Color.Black
                                ),
                                shape = MaterialTheme.shapes.medium,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 10.dp
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.pendingMetasCount > 0) {
                                                Badge { Text("${uiState.pendingMetasCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.outline_trophy_24),
                                            contentDescription = "Metas",
                                            tint = LocalContentColor.current,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "Mis Metas",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { onEvent(MenuMainEvent.OnSyncClick) },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(65.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F0),
                                    contentColor = Color.Black
                                ),
                                shape = MaterialTheme.shapes.medium,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 10.dp
                                ),
                                enabled = !uiState.isLoading // Disable button while loading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = colorResource(R.color.principal),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_sync_24),
                                            contentDescription = "Sincronizar",
                                            tint = LocalContentColor.current,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = "Actualizar datos",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CongratulationAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "congratulation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_trophy_24),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color(0xFFFFA500) // Orange/Gold
        )
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF333333),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}
