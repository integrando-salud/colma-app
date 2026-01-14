package com.codeco.tabacaleros.features.DNI.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.codeco.tabacaleros.features.casaColono.ui.CasaColonoUI
import com.codeco.tabacaleros.features.censo.ui.CensoUI
import com.codeco.tabacaleros.features.DNI.data.DNIRepositoryImpl
import com.codeco.tabacaleros.features.DNI.viewmodel.DNIViewModel
import com.codeco.tabacaleros.features.DNI.viewmodel.DNIViewModelFactory
import com.codeco.tabacaleros.features.DNI.viewmodel.DniEvent
import com.codeco.tabacaleros.features.DNI.viewmodel.DniUiState
import com.codeco.tabacaleros.features.home.ui.HomeActivity
import com.codeco.tabacaleros.features.utils.configureSystemBars
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesDesarrolloService
import com.codeco.tabacaleros.servicios.internetCommunication.DatosContactoService
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme


class dniUI : ComponentActivity() {

    private val viewModel: DNIViewModel by viewModels {
        val sessionManager = SessionManager(this)
        DNIViewModelFactory(
            DNIRepositoryImpl(
                DatosContactoService(this, sessionManager),
                ActividadesDesarrolloService(sessionManager),
                sessionManager
            ),
            sessionManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        val fromConsulta = intent.getBooleanExtra(Menu.EXTRA_FROM_CONSULTA, false)

        setContent {
            TabacalerosTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is DNIViewModel.NavigationEvent.NavigateToNextScreen -> {
                                if (fromConsulta) {
                                    val intent = Intent(this@dniUI, CensoUI::class.java)
                                    intent.putExtra(Menu.EXTRA_FROM_CONSULTA, true)
                                    intent.putExtra("cuil_digits", event.cuil)
                                    startActivity(intent)
                                } else {
                                    val intent = Intent(this@dniUI, HomeActivity::class.java)
                                    intent.putExtra("cuil_digits", event.cuil)
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                }

                DniScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    fromConsulta = fromConsulta
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DniScreen(
    uiState: DniUiState,
    onEvent: (DniEvent) -> Unit,
    fromConsulta: Boolean
) {
    val context = LocalContext.current
    val digitsOnly = uiState.documentNumber.filter { it.isDigit() }
    val isValid = digitsOnly.length in 11..12

    val errorText = when {
        !uiState.showTouched -> null
        digitsOnly.isEmpty() -> "Ingrese su CUIL"
        digitsOnly.length < 11 -> "El CUIL debe tener al menos 11 dígitos"
        digitsOnly.length > 12 -> "El CUIL no puede tener más de 12 dígitos"
        else -> null
    }

    if (uiState.showWarningDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(DniEvent.WarningDialogDismissed) },
            title = { Text("Atención") },
            text = { Text(uiState.dialogMessage) },
            confirmButton = {
                TextButton(onClick = { onEvent(DniEvent.WarningDialogDismissed) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (!uiState.selectedCasaColonoName.isNullOrBlank()) uiState.selectedCasaColonoName else "Tabacaleros", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(context, CasaColonoUI::class.java))
                            (context as? Activity)?.finish()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver para atras")
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

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                DNICard(
                    title = "Ingrese CUIL",
                    logoRes = R.drawable.person,
                    cardBackground = Color.White,
                    accentColor = Color(0xFF4CAF50),
                    initialNumber = uiState.documentNumber,
                    onNumberChange = { onEvent(DniEvent.DocumentNumberChanged(it)) },
                    onNumberSubmitted = { },
                    productorInfo = uiState.productorInfo,
                    onCallPhone = { phoneNumber ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    },
                    onPhoneNumberChange = { onEvent(DniEvent.PhoneNumberChanged(it)) },
                    phoneVerifiedStatus = uiState.phoneVerificationResult,
                    onVerificationChange = { onEvent(DniEvent.PhoneVerificationChanged(it)) },
                    fromConsulta = fromConsulta
                )
                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(120.dp)) // Spacer for sticky button
            }

            val verificationSatisfied = (uiState.productorInfo == null) || (uiState.phoneVerificationResult != null)

            Button(
                onClick = {
                    onEvent(DniEvent.ScreenTouched)
                    if (isValid) {
                        onEvent(DniEvent.ContinueClicked)
                    } else {
                        Toast.makeText(context, "Ingrese un CUIL válido (11–12 dígitos)", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isValid && (fromConsulta || verificationSatisfied) && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        color = colorResource(R.color.accent),
                        shape = RoundedCornerShape(8.dp)
                    ),
            ) {
                Text("Continuar")
            }
        }
    }
}
