package com.codeco.tabacaleros.features.Menu.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.DNI.ui.dniUI
import com.codeco.tabacaleros.features.Menu.data.MenuRepositoryImpl
import com.codeco.tabacaleros.features.Menu.viewmodel.MenuEvent
import com.codeco.tabacaleros.features.Menu.viewmodel.MenuViewModel
import com.codeco.tabacaleros.features.Menu.viewmodel.MenuViewModelFactory
import com.codeco.tabacaleros.features.Menu.viewmodel.MenuUiState
import com.codeco.tabacaleros.features.utils.cards.ActionCard
import com.codeco.tabacaleros.features.utils.configureSystemBars
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme

class Menu : ComponentActivity() {

    private val viewModel: MenuViewModel by viewModels {
        MenuViewModelFactory(
            MenuRepositoryImpl(
                SessionManager(this)
            )
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
                        when(event) {
                            is MenuViewModel.NavigationEvent.NavigateToDni -> {
                                val intent = Intent(context, dniUI::class.java)
                                context.startActivity(intent)
                            }
                            is MenuViewModel.NavigationEvent.NavigateToDniConsulta -> {
                                val intent = Intent(context, dniUI::class.java)
                                intent.putExtra(EXTRA_FROM_CONSULTA, true)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
                MenuScreen(uiState = uiState, onEvent = viewModel::onEvent)
            }
        }
    }

    companion object {
        const val EXTRA_FROM_CONSULTA = "from_consulta"
    }
}

/**
 * Contenido principal
 * -------------------------------------------------------------------------------------------------
 */



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(uiState: MenuUiState, onEvent: (MenuEvent) -> Unit) {

    val context = LocalActivity.current

    Scaffold (
        modifier = Modifier.systemBarsPadding(),
        topBar = {

            TopAppBar(
                title = { Text(text = uiState.radicacionName ?: "Tabacaleros", fontSize = 20.sp, fontWeight = FontWeight.Bold)},
                navigationIcon = {
                    IconButton(onClick = { context?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atras"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.principal),
                    titleContentColor = Color.White,
                    navigationIconContentColor =Color.White
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
    ) {
            innerPadding ->

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 25.dp)
        ) {

            item {
                Text(text = "¿Que desea hacer?",
                    fontSize = 25.sp,
                    color = colorResource(R.color.texto),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                )
            }

            if (uiState.components.any { it.nombre == "Registro Tabacalero" }) {
                item {
                    ActionCard(
                        modifier = Modifier,
                        title = "Registro Tabacalero",
                        subtitle = "Produccion 2025",
                        iconRes = R.drawable.outline_checkbook_24,
                        iconContentDescription = "Icono para registro",
                        onClick = {
                            onEvent(MenuEvent.OnRegistroClicked)
                        }, )
                }
            }

            if (uiState.components.any { it.nombre == "Consulta Registro Tabacalero" }) {
                item {
                    ActionCard(
                        modifier = Modifier,
                        title = "Registro Tabacalero-Consulta",
                        subtitle = "Información del productor",
                        iconRes = R.drawable.outline_list_alt_24,
                        iconContentDescription = "Icono para consulta",
                        onClick = {
                            onEvent(MenuEvent.OnConsultaClicked)
                        },
                    )
                }
            }
        }
    }
}