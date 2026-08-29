package com.colma.colmaApp.features.main.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween


import com.colma.colmaApp.R
import com.colma.colmaApp.features.login.ui.LoadingScreen
import com.colma.colmaApp.features.login.ui.LoginScreen
import com.colma.colmaApp.features.login.viewmodel.LoginViewModel
import com.colma.colmaApp.features.login.viewmodel.LoginViewModelFactory
import com.colma.colmaApp.features.content.ui.TreatmentDownloadScreen
import com.colma.colmaApp.features.MenuMain.data.MenuMainRepositoryImpl
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainEvent
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainViewModel
import com.colma.colmaApp.features.MenuMain.viewmodel.MenuMainViewModelFactory
import com.colma.colmaApp.features.MenuMain.ui.MenuScreen
import com.colma.colmaApp.features.MenuMain.ui.CongratulationAnimation
import com.colma.colmaApp.features.MenuMain.ui.DrawerItem
import com.colma.colmaApp.features.sesiones.SessionList.ui.SessionListScreen
import com.colma.colmaApp.features.sesiones.SessionList.viewmodel.SessionListEvent
import com.colma.colmaApp.features.sesiones.SessionList.viewmodel.SessionListViewModel
import com.colma.colmaApp.features.sesiones.SessionList.viewmodel.SessionListViewModelFactory
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionListRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.ui.SessionStepsScreen
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsEvent
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsViewModel
import com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel.SessionStepsViewModelFactory
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.multimedia.ui.MultimediaScreen
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaEvent
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaViewModel
import com.colma.colmaApp.features.multimedia.viewmodel.MultimediaViewModelFactory
import com.colma.colmaApp.features.RegistroSalud.ui.RegistrosSaludScreen
import com.colma.colmaApp.features.RegistroSalud.data.RegistroSaludRepositoryImpl
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludEvent
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludViewModel
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludViewModelFactory
import com.colma.colmaApp.features.glucosa.ui.GlucosaScreen
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaEvent
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaViewModel
import com.colma.colmaApp.features.glucosa.viewmodel.GlucosaViewModelFactory
import com.colma.colmaApp.features.glucosa.data.GlucosaRepositoryImpl
import com.colma.colmaApp.features.presion.ui.PresionScreen
import com.colma.colmaApp.features.presion.viewmodel.PresionEvent
import com.colma.colmaApp.features.presion.viewmodel.PresionViewModel
import com.colma.colmaApp.features.presion.viewmodel.PresionViewModelFactory
import com.colma.colmaApp.features.presion.data.PresionRepositoryImpl
import com.colma.colmaApp.features.mood.MoodMenuScreen
import com.colma.colmaApp.features.mood.viewmodel.MoodMenuEvent
import com.colma.colmaApp.features.mood.viewmodel.MoodMenuViewModel
import com.colma.colmaApp.features.mood.viewmodel.MoodMenuViewModelFactory
import com.colma.colmaApp.features.mood.data.MoodRepositoryImpl
import com.colma.colmaApp.features.mood.MoodQuizScreen
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizEvent
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizViewModel
import com.colma.colmaApp.features.mood.viewmodel.MoodQuizViewModelFactory
import com.colma.colmaApp.features.MetasHistory.ui.MetasHistoryScreen
import com.colma.colmaApp.features.MetasHistory.viewmodel.MetasHistoryViewModel
import com.colma.colmaApp.features.MetasHistory.viewmodel.MetasHistoryViewModelFactory
import com.colma.colmaApp.features.MetasHistory.data.MetasHistoryRepositoryImpl

import com.colma.colmaApp.features.instructions.ui.AdditionalInfoScreen
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.TopBarConfig
import com.colma.colmaApp.utils.varios.CustomAlertDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
inline fun <reified VM : ViewModel> entryViewModel(
    storeOwner: ViewModelStoreOwner,
    factory: ViewModelProvider.Factory
): VM {
    return remember(storeOwner, VM::class.java) {
        ViewModelProvider(storeOwner, factory)[VM::class.java]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val navController = rememberNavController()
    var topBarConfig by remember { mutableStateOf(TopBarConfig()) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var isBackClickPending by remember { mutableStateOf(false) }

    LaunchedEffect(isBackClickPending) {
        if (isBackClickPending) {
            delay(800)
            isBackClickPending = false
        }
    }

    val context = LocalContext.current
    val activity = context as ComponentActivity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Scope MenuMainViewModel to the Activity so it can be shared with the Navigation Drawer
    val menuViewModel = entryViewModel<MenuMainViewModel>(
        storeOwner = activity,
        factory = MenuMainViewModelFactory(
            MenuMainRepositoryImpl(context.applicationContext, DbHelper(context.applicationContext))
        )
    )
    val menuUiState by menuViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(menuUiState.snackbarMessage) {
        menuUiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            menuViewModel.onEvent(MenuMainEvent.SnackbarDismissed)
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Modal navigation drawer dialogs (recreated from MenuMainUI) - Only shown on main menu
    if (currentRoute == "menu_main" && !menuUiState.isLoading) {
        if (menuUiState.showObjectiveDialog && menuUiState.todaysObjective != null) {
            CustomAlertDialog(
                show = true,
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.ObjectiveDialogDismissed) },
                title = "Objetivo para Hoy",
                description = "¿Completaste este objetivo?: ${menuUiState.todaysObjective!!.actividad ?: "No hay detalles."}",
                onYes = { menuViewModel.onEvent(MenuMainEvent.ObjectiveCompleted) },
                onNo = { menuViewModel.onEvent(MenuMainEvent.ObjectiveNotCompleted("")) }
            )
        }

        if (menuUiState.showReasonDialog) {
            val reasons = listOf(
                "No tenía energía / ánimo para hacerlo",
                "Otras actividades me quitaron tiempo (trabajo, familia, etc.)",
                "Tuve dificultades para hacer la actividad",
                "Tuve un problema de salud / estuve enfermo(a)",
                "La persona con quien iba a hacer la actividad no pudo acompañarme",
                "Me olvidé de hacer la actividad.",
                "otro"
            )
            var selectedReason by remember { mutableStateOf(menuUiState.selectedReason) }

            AlertDialog(
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.ObjectiveDialogDismissed) },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = { Text("¿Por qué no completaste el objetivo?") },
                text = {
                    Column {
                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp),
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
                                menuViewModel.onEvent(MenuMainEvent.ObjectiveNotCompleted(selectedReason))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White)
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

        if (menuUiState.showOtherReasonDialog) {
            AlertDialog(
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.ObjectiveDialogDismissed) },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = { Text("Escribe otro motivo") },
                text = {
                    var otherReason by remember { mutableStateOf(menuUiState.otherReasonInput) }
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = {
                            otherReason = it
                            menuViewModel.onEvent(MenuMainEvent.OtherReasonChanged(it))
                        },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (menuUiState.otherReasonInput.isNotBlank()) {
                                menuViewModel.onEvent(MenuMainEvent.OtherReasonConfirmed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White)
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

        if (menuUiState.showFeedbackDialog) {
            AlertDialog(
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.DismissFeedbackDialog) },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                icon = {
                    if (menuUiState.showTrophyInFeedback) {
                        CongratulationAnimation()
                    } else if (menuUiState.isHelpRequestFeedback && menuUiState.isSuccessFeedback) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                },
                title = {
                    Text(
                        text = menuUiState.feedbackMessage,
                        fontSize = if (menuUiState.isHelpRequestFeedback) 20.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = if (menuUiState.isHelpRequestFeedback) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { menuViewModel.onEvent(MenuMainEvent.DismissFeedbackDialog) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White)
                    ) {
                        Text("Aceptar", fontSize = 16.sp)
                    }
                }
            )
        }

        if (menuUiState.showNoInternetDialog) {
            AlertDialog(
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.DismissNoInternetDialog) },
                title = { Text("Sin conexión") },
                text = { Text("Los datos se actualizarán cuando tenga acceso a internet.", fontSize = 16.sp) },
                confirmButton = {
                    Button(
                        onClick = { menuViewModel.onEvent(MenuMainEvent.DismissNoInternetDialog) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White)
                    ) {
                        Text("Aceptar", fontSize = 16.sp)
                    }
                }
            )
        }

        if (menuUiState.showHelpRequestDialog) {
            CustomAlertDialog(
                show = true,
                onDismissRequest = { menuViewModel.onEvent(MenuMainEvent.DismissHelpRequestDialog) },
                title = "¿Está seguro/a que quieres pedir ayuda técnica a tu promotora de salud?",
                description = "",
                yesLabel = "Confirmar",
                noLabel = "Cancelar",
                onYes = { menuViewModel.onEvent(MenuMainEvent.ConfirmHelpRequest) },
                onNo = { menuViewModel.onEvent(MenuMainEvent.DismissHelpRequestDialog) }
            )
        }
    }

    // Modal navigation drawer
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute != null && currentRoute != "login" && currentRoute != "treatment_download",
            modifier = Modifier.systemBarsPadding(),
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(drawerContainerColor = Color.White) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.colma_logo),
                                contentDescription = "Logo de la aplicación",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(Modifier.height(12.dp))

                            DrawerItem("Inicio", Icons.Default.Home) {
                                scope.launch { drawerState.close() }
                                navController.navigate("menu_main") {
                                    popUpTo("menu_main") { inclusive = false }
                                }
                            }

                            DrawerItem("Ayuda técnica de la promotora de salud", Icons.Default.Call) {
                                scope.launch { drawerState.close() }
                                menuViewModel.onEvent(MenuMainEvent.OnRequestHelpClick)
                            }

                            DrawerItem("Información adicional", Icons.Default.Info) {
                                scope.launch { drawerState.close() }
                                navController.navigate("additional_info")
                            }

                            DrawerItem("Tutorial", Icons.Default.Face) {
                                scope.launch { drawerState.close() }
                            }

                            DrawerItem("Descargar recursos de nuevo", Icons.Default.Refresh) {
                                scope.launch { drawerState.close() }
                                navController.navigate("treatment_download?force=true")
                            }

                            Spacer(Modifier.height(28.dp))

                            Button(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    menuViewModel.onEvent(MenuMainEvent.OnSalirClick)
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
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (topBarConfig.showTopBar) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = topBarConfig.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            initialDelayMillis = 2000,
                                            repeatDelayMillis = 1500
                                        )
                                    )
                                },
                                navigationIcon = {
                                    if (topBarConfig.showBackButton) {
                                        IconButton(
                                            onClick = {
                                                if (!isBackClickPending) {
                                                    isBackClickPending = true
                                                    topBarConfig.onBackClick?.invoke() ?: navController.popBackStack()
                                                }
                                            },
                                            enabled = !isBackClickPending
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Volver"
                                            )
                                        }
                                    } else if (topBarConfig.showMenuButton) {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Abrir menú"
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                                    if (currentRoute == "menu_main") {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colorResource(R.color.contraste),
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .heightIn(min = 36.dp)
                                                .clickable { scope.launch { drawerState.open() } }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Menú",
                                                    color = colorResource(R.color.white),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = colorResource(R.color.principal),
                                    titleContentColor = Color.White,
                                    navigationIconContentColor = Color.White
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") { backStackEntry ->
                            val loginViewModel = entryViewModel<LoginViewModel>(
                                storeOwner = backStackEntry,
                                factory = LoginViewModelFactory(context.applicationContext)
                            )
                            val uiState by loginViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(showTopBar = false)
                            }

                            LaunchedEffect(Unit) {
                                loginViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is LoginViewModel.NavigationEvent.NavigateToHome -> {
                                            navController.navigate("treatment_download") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isCheckingSession) {
                                LoadingScreen()
                            } else {
                                LoginScreen(
                                    uiState = uiState,
                                    onEvent = loginViewModel::onEvent
                                )
                            }
                        }

                        composable(
                            route = "treatment_download?force={force}",
                            arguments = listOf(navArgument("force") {
                                type = NavType.BoolType
                                defaultValue = false
                            })
                        ) { backStackEntry ->
                            val force = backStackEntry.arguments?.getBoolean("force") ?: false
                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(showTopBar = false)
                            }
                            TreatmentDownloadScreen(
                                navController = navController,
                                context = context.applicationContext,
                                force = force
                            )
                        }

                        composable("menu_main") {
                            val uiState by menuViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                menuViewModel.onEvent(MenuMainEvent.LoadInitialData)
                            }

                            LaunchedEffect(uiState.userName) {
                                topBarConfig = TopBarConfig(
                                    title = "Bienvenido/a ${uiState.userName}",
                                    showMenuButton = false,
                                    showBackButton = false,
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                menuViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is MenuMainViewModel.NavigationEvent.NavigateToSessions -> {
                                            navController.navigate("sesiones_menu/${event.treatmentTypes}")
                                        }
                                        is MenuMainViewModel.NavigationEvent.NavigateToHealthRegisters -> {
                                            navController.navigate("registros_salud")
                                        }
                                        is MenuMainViewModel.NavigationEvent.NavigateToMetasHistory -> {
                                            navController.navigate("metas_history")
                                        }
                                        is MenuMainViewModel.NavigationEvent.NavigateToLogin -> {
                                            navController.navigate("login") {
                                                popUpTo("menu_main") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }

                            MenuScreen(
                                uiState = uiState,
                                onEvent = menuViewModel::onEvent,
                                snackbarHostState = snackbarHostState
                            )
                        }

                        composable(
                            route = "sesiones_menu/{treatmentTypes}",
                            arguments = listOf(navArgument("treatmentTypes") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val treatmentTypesString = backStackEntry.arguments?.getString("treatmentTypes") ?: "HTA"
                            val treatmentTypes = remember(treatmentTypesString) {
                                treatmentTypesString.split(',').mapNotNull {
                                    try {
                                        TreatmentType.valueOf(it.trim().uppercase())
                                    } catch (e: IllegalArgumentException) {
                                        null
                                    }
                                }.ifEmpty { listOf(TreatmentType.HTA) }
                            }

                            val sessionListViewModel = entryViewModel<SessionListViewModel>(
                                storeOwner = backStackEntry,
                                factory = SessionListViewModelFactory(context.applicationContext)
                            )
                            val uiState by sessionListViewModel.uiState.collectAsState()

                            LaunchedEffect(treatmentTypes) {
                                sessionListViewModel.onEvent(SessionListEvent.SetTreatmentTypes(treatmentTypes))
                            }

                            val topBarTitle = remember(uiState.treatmentTypes) {
                                when {
                                    uiState.treatmentTypes.contains(TreatmentType.DBT_HTA) -> "Hipertensión y Diabetes"
                                    uiState.treatmentTypes.contains(TreatmentType.HTA) && uiState.treatmentTypes.contains(TreatmentType.DIABETES) -> "Hipertensión y Diabetes"
                                    uiState.treatmentTypes.contains(TreatmentType.HTA) -> "Hipertensión"
                                    uiState.treatmentTypes.contains(TreatmentType.DIABETES) -> "Diabetes"
                                    else -> "Sesiones"
                                }
                            }

                            LaunchedEffect(topBarTitle) {
                                topBarConfig = TopBarConfig(
                                    title = topBarTitle,
                                    showBackButton = true,
                                    onBackClick = { sessionListViewModel.onEvent(SessionListEvent.BackClicked) },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                sessionListViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is SessionListViewModel.NavigationEvent.NavigateToSessionSteps -> {
                                            navController.navigate("multimedia_selector/${event.parentSessionId}/${event.sessionTitle}/${event.subSessionId}/${event.subSessionTitle}/${event.treatmentType}")
                                        }
                                        SessionListViewModel.NavigationEvent.NavigateBack -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }

                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        sessionListViewModel.onEvent(SessionListEvent.Refresh)
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }

                            SessionListScreen(
                                uiState = uiState,
                                onEvent = sessionListViewModel::onEvent
                            )
                        }

                        composable(
                            route = "multimedia_selector/{sessionId}/{sessionTitle}/{subSessionId}/{subSessionTitle}/{treatmentTypes}",
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("sessionTitle") { type = NavType.StringType },
                                navArgument("subSessionId") { type = NavType.LongType },
                                navArgument("subSessionTitle") { type = NavType.StringType },
                                navArgument("treatmentTypes") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                            val sessionTitle = backStackEntry.arguments?.getString("sessionTitle") ?: "Sesión"
                            val subSessionId = backStackEntry.arguments?.getLong("subSessionId") ?: -1L
                            val subSessionTitle = backStackEntry.arguments?.getString("subSessionTitle") ?: ""
                            val treatmentTypesString = backStackEntry.arguments?.getString("treatmentTypes") ?: "HTA"
                            val treatmentTypes = remember(treatmentTypesString) {
                                treatmentTypesString.split(',').mapNotNull {
                                    try {
                                        TreatmentType.valueOf(it.trim().uppercase())
                                    } catch (e: IllegalArgumentException) {
                                        null
                                    }
                                }.ifEmpty { listOf(TreatmentType.HTA) }
                            }

                            val sessionStepsViewModel = entryViewModel<SessionStepsViewModel>(
                                storeOwner = backStackEntry,
                                factory = SessionStepsViewModelFactory(context.applicationContext)
                            )
                            val uiState by sessionStepsViewModel.uiState.collectAsState()

                            LaunchedEffect(sessionId, sessionTitle, subSessionId, subSessionTitle, treatmentTypes) {
                                sessionStepsViewModel.initialize(sessionId, sessionTitle, subSessionId, subSessionTitle, treatmentTypes)
                            }

                            LaunchedEffect(subSessionTitle) {
                                topBarConfig = TopBarConfig(
                                    title = subSessionTitle,
                                    showBackButton = true,
                                    onBackClick = { sessionStepsViewModel.onEvent(SessionStepsEvent.BackClicked) },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                sessionStepsViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is SessionStepsViewModel.NavigationEvent.NavigateToMultimedia -> {
                                            navController.navigate("multimedia/${uiState.sessionId}/${uiState.sessionTitle}/${uiState.subSessionId}/${uiState.subSessionTitle}/${uiState.treatmentTypes.joinToString(",") { it.name }}/${event.index}")
                                        }
                                        SessionStepsViewModel.NavigationEvent.NavigateBack -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }

                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        if (sessionStepsViewModel.uiState.value.sessionId != -1L) {
                                            sessionStepsViewModel.loadMultimediaOptions()
                                        }
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }

                            SessionStepsScreen(
                                uiState = uiState,
                                onEvent = sessionStepsViewModel::onEvent
                            )
                        }

                        composable(
                            route = "multimedia/{sessionId}/{sessionTitle}/{subSessionId}/{subSessionTitle}/{treatmentTypes}/{index}",
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("sessionTitle") { type = NavType.StringType },
                                navArgument("subSessionId") { type = NavType.LongType },
                                navArgument("subSessionTitle") { type = NavType.StringType },
                                navArgument("treatmentTypes") { type = NavType.StringType },
                                navArgument("index") { type = NavType.IntType }
                            ),
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(400)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(400)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(400)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(400)
                                )
                            }
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                            val sessionTitle = backStackEntry.arguments?.getString("sessionTitle") ?: "Sesión"
                            val subSessionId = backStackEntry.arguments?.getLong("subSessionId") ?: -1L
                            val subSessionTitle = backStackEntry.arguments?.getString("subSessionTitle") ?: ""
                            val treatmentTypesString = backStackEntry.arguments?.getString("treatmentTypes") ?: ""
                            val currentMultimediaIndex = backStackEntry.arguments?.getInt("index") ?: 0

                            val multimediaViewModel = entryViewModel<MultimediaViewModel>(
                                storeOwner = backStackEntry,
                                factory = MultimediaViewModelFactory(context.applicationContext)
                            )
                            val uiState by multimediaViewModel.uiState.collectAsState()

                            LaunchedEffect(sessionId, sessionTitle, subSessionId, subSessionTitle, treatmentTypesString, currentMultimediaIndex) {
                                multimediaViewModel.onEvent(
                                    MultimediaEvent.LoadMultimedia(
                                        sessionId = sessionId,
                                        sessionTitle = sessionTitle,
                                        subSessionId = subSessionId,
                                        subSessionTitle = subSessionTitle,
                                        treatmentTypesString = treatmentTypesString,
                                        startIndex = currentMultimediaIndex
                                    )
                                )
                            }

                            val currentOption = uiState.currentMultimediaOption
                            val topBarTitle = remember(currentOption) {
                                currentOption?.type?.let { type ->
                                    when (type) {
                                        MultimediaContentType.DESCRIPTION -> "Descripción"
                                        MultimediaContentType.VIDEO -> "Video"
                                        MultimediaContentType.AUDIO -> "Audio"
                                        MultimediaContentType.METAS -> "Metas"
                                        MultimediaContentType.QUIZ -> "Pregunta del día"
                                        else -> type.name
                                    }
                                } ?: ""
                            }

                            LaunchedEffect(topBarTitle) {
                                topBarConfig = TopBarConfig(
                                    title = topBarTitle,
                                    showBackButton = true,
                                    onBackClick = { multimediaViewModel.onEvent(MultimediaEvent.Finish) },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                multimediaViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is MultimediaViewModel.NavigationEvent.NavigateBack -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }

                            MultimediaScreen(
                                uiState = uiState,
                                onEvent = multimediaViewModel::onEvent
                            )
                        }

                        composable("registros_salud") { backStackEntry ->
                            val registrosSaludViewModel = entryViewModel<RegistroSaludViewModel>(
                                storeOwner = backStackEntry,
                                factory = RegistroSaludViewModelFactory(RegistroSaludRepositoryImpl(context.applicationContext))
                            )

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Mis registros de salud",
                                    showBackButton = true,
                                    onBackClick = { registrosSaludViewModel.onEvent(RegistroSaludEvent.OnBackClicked) },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                registrosSaludViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is RegistroSaludViewModel.NavigationEvent.NavigateToGlucosa -> {
                                            navController.navigate("glucosa")
                                        }
                                        is RegistroSaludViewModel.NavigationEvent.NavigateToPresion -> {
                                            navController.navigate("presion")
                                        }
                                        is RegistroSaludViewModel.NavigationEvent.NavigateToEstadoAnimo -> {
                                            navController.navigate("mood_menu")
                                        }
                                        is RegistroSaludViewModel.NavigationEvent.NavigateBack -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }

                            RegistrosSaludScreen(onEvent = registrosSaludViewModel::onEvent)
                        }

                        composable("glucosa") { backStackEntry ->
                            val glucosaViewModel = entryViewModel<GlucosaViewModel>(
                                storeOwner = backStackEntry,
                                factory = GlucosaViewModelFactory(GlucosaRepositoryImpl(context.applicationContext))
                            )
                            val uiState by glucosaViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Medición de Glucosa",
                                    showBackButton = true,
                                    onBackClick = { navController.popBackStack() },
                                    showTopBar = true
                                )
                            }

                            GlucosaScreen(
                                uiState = uiState,
                                onEvent = glucosaViewModel::onEvent,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("presion") { backStackEntry ->
                            val presionViewModel = entryViewModel<PresionViewModel>(
                                storeOwner = backStackEntry,
                                factory = PresionViewModelFactory(PresionRepositoryImpl(context.applicationContext))
                            )
                            val uiState by presionViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Medición de Presión",
                                    showBackButton = true,
                                    onBackClick = { navController.popBackStack() },
                                    showTopBar = true
                                )
                            }

                            PresionScreen(
                                uiState = uiState,
                                onEvent = presionViewModel::onEvent,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("mood_menu") { backStackEntry ->
                            val moodMenuViewModel = entryViewModel<MoodMenuViewModel>(
                                storeOwner = backStackEntry,
                                factory = MoodMenuViewModelFactory(MoodRepositoryImpl(context.applicationContext))
                            )
                            val uiState by moodMenuViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Estado de ánimo",
                                    showBackButton = true,
                                    onBackClick = { navController.popBackStack() },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                moodMenuViewModel.navigationEvent.collect { event ->
                                    when (event) {
                                        is MoodMenuViewModel.NavigationEvent.NavigateToQuiz -> {
                                            navController.navigate("mood_quiz")
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(Unit) {
                                moodMenuViewModel.onEvent(MoodMenuEvent.RefreshHistory)
                            }

                            MoodMenuScreen(
                                uiState = uiState,
                                onEvent = moodMenuViewModel::onEvent,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("mood_quiz") { backStackEntry ->
                            val moodQuizViewModel = entryViewModel<MoodQuizViewModel>(
                                storeOwner = backStackEntry,
                                factory = MoodQuizViewModelFactory(MoodRepositoryImpl(context.applicationContext))
                            )
                            val uiState by moodQuizViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Estado de ánimo",
                                    showBackButton = true,
                                    onBackClick = { moodQuizViewModel.onEvent(MoodQuizEvent.BackPressed) },
                                    showTopBar = true
                                )
                            }

                            LaunchedEffect(Unit) {
                                moodQuizViewModel.sideEffects.collect { effect ->
                                    when (effect) {
                                        MoodQuizViewModel.SideEffect.NavigateBack -> {
                                            navController.popBackStack()
                                        }
                                        MoodQuizViewModel.SideEffect.NavigateBackWithResult -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }

                            MoodQuizScreen(
                                uiState = uiState,
                                onEvent = moodQuizViewModel::onEvent
                            )
                        }

                        composable("metas_history") { backStackEntry ->
                            val metasHistoryViewModel = entryViewModel<MetasHistoryViewModel>(
                                storeOwner = backStackEntry,
                                factory = MetasHistoryViewModelFactory(MetasHistoryRepositoryImpl(context.applicationContext))
                            )
                            val uiState by metasHistoryViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Mis metas",
                                    showBackButton = true,
                                    onBackClick = { navController.popBackStack() },
                                    showTopBar = true
                                )
                            }

                            MetasHistoryScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() }
                            )
                        }



                        composable("additional_info") {
                            LaunchedEffect(Unit) {
                                topBarConfig = TopBarConfig(
                                    title = "Información adicional",
                                    showBackButton = true,
                                    onBackClick = { navController.popBackStack() },
                                    showTopBar = true
                                )
                            }

                            AdditionalInfoScreen()
                        }
                    }
                }
            }
        }
    }
}
