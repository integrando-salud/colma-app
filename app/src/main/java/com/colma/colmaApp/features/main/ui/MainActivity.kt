package com.colma.colmaApp.features.main.ui

import android.content.Intent
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.features.instructions.ui.InstructionsActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Drawer state + coroutine scope
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color.White) {
                // replace with your logo
                Image(
                    painter = painterResource(id = R.drawable.colma_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(55.dp))

                Button(
                    onClick = { /* handle logout or exit */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE84589),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Salir")
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .systemBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Colma App",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFE84589),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* TODO: new session */ },
                    containerColor = Color(0xFFE84589),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        // push it *down* into the BottomAppBar by 28.dp
                        .offset(y = 45.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva sesión")
                }
            },
            floatingActionButtonPosition = FabPosition.Center,

            // Full-width BottomAppBar
            bottomBar = {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),      // <-- thinner than the default 56.dp
                    containerColor = Color(0xFFD55A95),
                    tonalElevation = 4.dp
                ) {
                }
            }
        ) { innerPadding ->
            // Here’s your original Greeting, with padding applied
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 1) Your main content goes here:
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp), // leave space for the bar + half the FAB
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(top = 32.dp, bottom = 15.dp)
                            .alpha(0.9f),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Selecciona una sesion o revisa tus registros",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(vertical = 1.dp)
                            .alpha(0.9f),

                    )

                    SessionRoulette()
                }
            }
        }
    }
}

@Composable
fun SessionRoulette() {
    val sessions = listOf(
        "Session Alpha",
        "Session Beta",
        "Session Gamma",
        "Session Delta",
        "Session Epsilon",
        "Session Zeta"
    )

    var selectedIndex by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Watermark
        Image(
            painter = painterResource(id = R.drawable.colma_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.45f),
            contentScale = ContentScale.Crop
        )

        // Sessions + Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1) The session items
            sessions.forEachIndexed { index, title ->
                val isSelected   = index == selectedIndex
                val bgColor      = if (isSelected) Color(0xFFE84589) else Color(0xFFF0F0F0)
                val contentColor = if (isSelected) Color.White     else Color.Black
                val fontSize     = if (isSelected) 24.sp            else 16.sp
                val verticalPad  = if (isSelected) 16.dp            else 12.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 4.dp)
                        .background(bgColor, shape = MaterialTheme.shapes.medium)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = index
                        }
                        .padding(vertical = verticalPad, horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Favorite else Icons.Default.Lock,
                            contentDescription = if (isSelected) "Unlocked" else "Locked",
                            tint = contentColor
                        )
                        Text(
                            text = title,
                            fontSize = fontSize,
                            color = contentColor
                        )
                    }
                }
            }

            // 2) Spacer to separate list from button
            Spacer(modifier = Modifier.height(24.dp))

            // 3) Your Intent‐launching Button
            Button(
                onClick = {
                    val intent = Intent(context, InstructionsActivity::class.java)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE84589),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Ver Instrucciones", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
