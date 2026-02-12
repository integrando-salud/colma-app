package com.colma.colmaApp.features.login.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.MenuMain.ui.MenuMainUI
import com.colma.colmaApp.features.login.viewmodel.LoginEvent
import com.colma.colmaApp.features.login.viewmodel.LoginUiState
import com.colma.colmaApp.features.login.viewmodel.LoginViewModel
import com.colma.colmaApp.features.login.viewmodel.LoginViewModelFactory
import com.colma.colmaApp.ui.theme.ColmaAppTheme

class LoginUI : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = android.graphics.Color.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is LoginViewModel.NavigationEvent.NavigateToHome -> {
                                startActivity(Intent(this@LoginUI, MenuMainUI::class.java))
                                finish()
                            }
                        }
                    }
                }
                LoginScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit
) {
    val isFormValid = uiState.dni.trim().isNotEmpty() && uiState.password.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(R.drawable.colma_logo),
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(0.40f)
            )

            Spacer(modifier = Modifier.height(50.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Ingresar",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Solicite usuario y contraseña a su promotor de salud asignado",
                        fontSize = 16.sp,
                        color = Color.hsl(0f, 0f, 0.70f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 5.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.dni,
                        onValueChange = { onEvent(LoginEvent.DniChanged(it)) },
                        label = { Text("DNI") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = colorResource(R.color.principal),
                            focusedBorderColor = colorResource(R.color.principal),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = colorResource(R.color.principal),
                            unfocusedLabelColor = Color(0xFF666666)
                        ),
                        textStyle = TextStyle(color = colorResource(R.color.black)),
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = colorResource(R.color.principal),
                            focusedBorderColor = colorResource(R.color.principal),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = colorResource(R.color.principal),
                            unfocusedLabelColor = Color(0xFF666666)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colorResource(R.color.black))
                    )

                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = { onEvent(LoginEvent.LoginClicked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isFormValid && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = false
                            )
                            .background(
                                colorResource(R.color.light_principal)
                            )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "Ingresar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Recordar contraseña",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.principal),
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}


