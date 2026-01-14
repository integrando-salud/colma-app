package com.codeco.tabacaleros.features.casaColono.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.Menu.ui.Menu
import com.codeco.tabacaleros.features.casaColono.data.CasaColonoRepositoryImpl
import com.codeco.tabacaleros.features.casaColono.viewmodel.CasaColonoEvent
import com.codeco.tabacaleros.features.casaColono.viewmodel.CasaColonoUiState
import com.codeco.tabacaleros.features.casaColono.viewmodel.CasaColonoViewModel
import com.codeco.tabacaleros.features.casaColono.viewmodel.CasaColonoViewModelFactory
import com.codeco.tabacaleros.features.utils.cards.ActionCard
import com.codeco.tabacaleros.features.utils.configureSystemBars
import com.codeco.tabacaleros.features.utils.sideDrawer.AppModalDrawer
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.RadicacionesService
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme
import kotlinx.coroutines.launch

class CasaColonoUI : ComponentActivity() {

    private val viewModel: CasaColonoViewModel by viewModels {
        val sessionManager = SessionManager(applicationContext)
        CasaColonoViewModelFactory(
            CasaColonoRepositoryImpl(
                RadicacionesService(sessionManager),
                sessionManager
            ),
            sessionManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        setContent {
            TabacalerosTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is CasaColonoViewModel.NavigationEvent.NavigateToMenu -> {
                                val intent = Intent(context, Menu::class.java)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
                CasaColonoScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasaColonoScreen(uiState: CasaColonoUiState, onEvent: (CasaColonoEvent) -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(contexto.applicationContext) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    AppModalDrawer(
        drawerState = drawerState,
        scope = scope,
        sessionManager = sessionManager
    ) {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Lugar de registro", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
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
            containerColor = colorResource(R.color.background)
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {

                Text(
                    text = "Seleccionar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.texto),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, start = 25.dp, bottom = 16.dp)
                )

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.radicaciones,
                            key = { it.id },
                            contentType = { "radicacion" }
                        ) { radicacion ->
                            ActionCard(
                                modifier = Modifier,
                                title = radicacion.descripcion ?: "Descripción no disponible",
                                subtitle = radicacion.direccionCalle ?: "",
                                iconRes = R.drawable.baseline_home_work_24,
                                iconContentDescription = "Icono para la casa del colono",
                                onClick = {
                                    onEvent(CasaColonoEvent.OnRadicacionClicked(radicacion))
                                }, )
                        }
                    }
                }
            }
        }
    }
}
