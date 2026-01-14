package com.codeco.tabacaleros.features.login.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import androidx.core.view.WindowCompat

import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import com.codeco.tabacaleros.features.casaColono.ui.CasaColonoUI
import com.codeco.tabacaleros.features.login.data.LoginRepositoryImpl
import com.codeco.tabacaleros.features.login.viewmodel.LoginEvent
import com.codeco.tabacaleros.features.login.viewmodel.LoginUiState
import com.codeco.tabacaleros.features.login.viewmodel.LoginViewModel
import com.codeco.tabacaleros.features.login.viewmodel.LoginViewModelFactory
import com.codeco.tabacaleros.features.utils.configureSystemBars
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.LoginService


class LoginUI : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            LoginRepositoryImpl(
                LoginService(),
                SessionManager(this)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        configureSystemBars()

        setContent {
            TabacalerosTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is LoginViewModel.NavigationEvent.NavigateToHome -> {
                                startActivity(Intent(this@LoginUI, CasaColonoUI::class.java))
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
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardOpen = imeBottom > 0

    val isFormValid = uiState.email.trim().isNotEmpty() && uiState.password.isNotEmpty()

    val baseContainer = Color(0xFF67310A)
    val enabledContainer = baseContainer
    val disabledContainer = baseContainer.copy(alpha = 0.38f)

    val enabledContent = Color.White
    val disabledContent = Color.White.copy(alpha = 0.87f)

    val animatedContainer by animateColorAsState(targetValue = if (isFormValid) enabledContainer else disabledContainer)
    val animatedContent by animateColorAsState(targetValue = if (isFormValid) enabledContent else disabledContent)

    val logoFraction by animateFloatAsState(
        targetValue = if (keyboardOpen) 0.25f else 0.5f,
        animationSpec = tween(durationMillis = 300)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .background(color = colorResource(R.color.background))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.tabaco_logo),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(logoFraction)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f, fill = false),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(R.color.backgroundLight))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF333333)),
                    colors = OutlinedTextFieldDefaults.colors(
                        Color(0xFF333333),
                        cursorColor = colorResource(R.color.principal),
                        focusedBorderColor = colorResource(R.color.principal),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor = colorResource(R.color.principal),
                        unfocusedLabelColor = Color(0xFF333333)
                    ),
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
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF333333)),
                    colors = OutlinedTextFieldDefaults.colors(
                        Color(0xFF333333),
                        cursorColor = colorResource(R.color.principal),
                        focusedBorderColor = colorResource(R.color.principal),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor = colorResource(R.color.principal),
                        unfocusedLabelColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                        .background(
                            color = colorResource(R.color.accent)
                        ),

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

                Spacer(Modifier.height(22.dp))

            }
        }
    }
}
