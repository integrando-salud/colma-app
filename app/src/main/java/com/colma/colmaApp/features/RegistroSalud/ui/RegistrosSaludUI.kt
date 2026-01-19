package com.colma.colmaApp.features.RegistroSalud.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.EstadoAnimoMenu.EstadoAnimoMenuActivity
import com.colma.colmaApp.features.RegistroSalud.data.RegistroSaludRepositoryImpl
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludEvent
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludViewModel
import com.colma.colmaApp.features.RegistroSalud.viewmodel.RegistroSaludViewModelFactory
import com.colma.colmaApp.features.glucosa.GlucosaActivity
import com.colma.colmaApp.features.presion.PresionActivity
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import android.graphics.Color as AndroidColor

class RegistrosSaludUI : ComponentActivity() {
    private val viewModel: RegistroSaludViewModel by viewModels {
        RegistroSaludViewModelFactory(
            RegistroSaludRepositoryImpl(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }

        setContent {
            ColmaAppTheme {
                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RegistroSaludViewModel.NavigationEvent.NavigateToGlucosa -> {
                                startActivity(Intent(this@RegistrosSaludUI, GlucosaActivity::class.java))
                            }
                            is RegistroSaludViewModel.NavigationEvent.NavigateToPresion -> {
                                startActivity(Intent(this@RegistrosSaludUI, PresionActivity::class.java))
                            }
                            is RegistroSaludViewModel.NavigationEvent.NavigateToEstadoAnimo -> {
                                startActivity(Intent(this@RegistrosSaludUI, EstadoAnimoMenuActivity::class.java))
                            }
                            is RegistroSaludViewModel.NavigationEvent.NavigateBack -> {
                                finish()
                            }
                        }
                    }
                }
                RegistrosSaludScreen(onEvent = viewModel::onEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosSaludScreen(
    onEvent: (RegistroSaludEvent) -> Unit
) {
    val signatureColor = colorResource(R.color.principal)

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Registro de Salud", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(RegistroSaludEvent.OnBackClicked) }) {
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
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿Qué te gustaría hacer?",
                    fontSize = 25.sp,
                    color = signatureColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                // Glucosa button
                Button(
                    onClick = { onEvent(RegistroSaludEvent.OnGlucosaClicked) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0F0F0),
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_glucose_24),
                            contentDescription = "Registros",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Medicion de Glucosa",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presion button
                Button(
                    onClick = { onEvent(RegistroSaludEvent.OnPresionClicked) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0F0F0),
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_blood_pressure_24),
                            contentDescription = "Registros",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Medicion de Presion",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Depresion button
                Button(
                    onClick = { onEvent(RegistroSaludEvent.OnEstadoAnimoClicked) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF7F7F8),
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Registros",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Estado de animo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}