package com.codeco.tabacaleros.features.utils.sideDrawer

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.login.ui.LoginUI
import com.codeco.tabacaleros.servicios.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DrawerContentBody(
    drawerState: DrawerState,
    scope: CoroutineScope,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.tabaco_logo),
            contentDescription = "Logo Empresa",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .size(120.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    // close drawer then clear session and navigate to Login
                    drawerState.close()
                    sessionManager.clearSession()

                    val intent = Intent(context, LoginUI::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.rojo),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Salir",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(text = "Salir")
        }

        Spacer(Modifier.height(50.dp))
    }
}

/**
 * Small wrapper that exposes a cleaner API for the rest of your screen.
 * Usage: wrap your screen content in AppModalDrawer(...){ /* main UI */ }
 */
@Composable
fun AppModalDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    sessionManager: SessionManager,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                DrawerContentBody(
                    drawerState = drawerState,
                    scope = scope,
                    sessionManager = sessionManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    ) {
        content()
    }
}