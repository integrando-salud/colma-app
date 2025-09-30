package com.colma.colmaApp.features.login.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import androidx.core.view.WindowCompat


import androidx.compose.material3.OutlinedTextField as OutlinedTextField1

import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.features.main.ui.MainActivity
import com.colma.colmaApp.R
import com.colma.colmaApp.features.MenuMain.MenuMain
import com.colma.colmaApp.features.login.viewmodel.LoginViewModel
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import android.graphics.Color as AndroidColor


class LoginUI : ComponentActivity(){

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // --- SET STATUS BAR + NAVIGATION BAR COLORS TO WHITE ---
        // Use android.graphics.Color (aliased as AndroidColor above)
        window.statusBarColor = AndroidColor.WHITE
        window.navigationBarColor = AndroidColor.WHITE

        // Make status & nav bar icons dark so they are visible over white background
        // WindowInsetsControllerCompat works across Android versions
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true       // dark status bar icons
            isAppearanceLightNavigationBars = true   // dark nav bar icons
        }
        setContent {
            ColmaAppTheme {
                LaunchedEffect(viewModel.navigateToMain) {
                    viewModel.navigateToMain.collect {
                        startActivity(Intent(this@LoginUI, MenuMain::class.java))
                        finish()
                    }
                }

                LoginScreen(viewModel)
            }
        }
    }

    @Composable
    fun LoginScreen(viewModel: LoginViewModel) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val dbHelper = DBhelper(applicationContext)
        val db = dbHelper.readableDatabase

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .background(Color.White)
                .padding(horizontal = 24.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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

                    OutlinedTextField1(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = Color(0xFFE84589),
                            focusedBorderColor = Color(0xFFE84589),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = Color(0xFFE84589),
                            unfocusedLabelColor = Color(0xFF666666)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField1(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            Color(0xFF333333),
                            cursorColor = Color(0xFFE84589),
                            focusedBorderColor = Color(0xFFE84589),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = Color(0xFFE84589),
                            unfocusedLabelColor = Color(0xFF666666)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = { viewModel.onLoginClick() },
                        colors = ButtonDefaults.buttonColors(
                            Color(0xFFE84589),
                            Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "Ingresar")
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "¿Desea recuperar su contraseña?",
                        color = Color(0xC4E84589)
                    )

                }


            }
        }
    }
}

