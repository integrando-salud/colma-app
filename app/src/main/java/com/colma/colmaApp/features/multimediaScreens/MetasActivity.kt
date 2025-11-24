package com.colma.colmaApp.features.multimediaScreens

import CardThatOpensEditDialog
import SimpleTextDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.utils.varios.CardThatOpensDateTimeDialog
import com.colma.colmaApp.utils.varios.CardThatOpensEmptyDialog
import com.colma.colmaApp.utils.varios.CardThatOpensSelectionDialog


class MetasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                MetasScreen(
                    onBack = { finish() },
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(onBack: () -> Unit = {}, onFinish: () -> Unit = {}) {

    val tipoActividad = rememberSaveable { mutableStateOf<String?>(null) }
    val actividad = rememberSaveable { mutableStateOf<String?>(null) }
    val conQuien = rememberSaveable { mutableStateOf<String?>(null) }
    val donde = rememberSaveable { mutableStateOf<String?>(null) }
    val fecha = rememberSaveable { mutableStateOf<String?>(null) }

    var showSummaryDialog by rememberSaveable { mutableStateOf(false) }
    var summaryMessage by rememberSaveable { mutableStateOf("") }

    var shouldExecuteFinalAction by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Metas", color = colorResource(R.color.white)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = colorResource(R.color.white)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.principal))
                )
            },

            bottomBar = {

            },
            content = { innerPadding ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(bottom = 84.dp), // Adjust this padding to accommodate the fixed button
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CardThatOpensSelectionDialog(
                                                title = "Tipo de Actividad",
                                                description = "Selecciona el tipo de actividad que deseas realizar.",
                                                items = listOf("Actividad Fisica", "Habitos", "Bienestar"),
                                                label = { it },
                                                onItemSelected = { chosen ->
                                                    tipoActividad.value = chosen
                                                }

                                            )

                                            Spacer(modifier = Modifier.padding(8.dp))

                                            CardThatOpensSelectionDialog(
                                                title = "Actividad",
                                                description = "Seleccione la actividad concreta que desee realizar.",
                                                items = listOf("Caminar 30 minutos", "Yoga", "Correr"),
                                                label = { it },
                                                onItemSelected = { chosen ->
                                                    actividad.value = chosen
                                                }
                                            )

                                            Spacer(modifier = Modifier.padding(8.dp))

                                            CardThatOpensEditDialog(
                                                title = "Con quien?",
                                                description = "Describe brevemente con quien iras",
                                                onSave = { newDesc ->
                                                    conQuien.value = newDesc
                                                }
                                            )

                                            Spacer(modifier = Modifier.padding(8.dp))

                                            CardThatOpensEditDialog(
                                                title = "Donde?",
                                                description = "Describe brevemente en donde lo harás",
                                                onSave = { newDesc ->
                                                    donde.value = newDesc
                                                }
                                            )

                                            Spacer(modifier = Modifier.padding(8.dp))

                                            CardThatOpensDateTimeDialog(
                                                title = "Fecha de registro",
                                                description = "Toca para cambiar",
                                                initialMillis = System.currentTimeMillis(),
                                                onDateTimeSelected = { datetime ->
                                                    fecha.value = datetime
                                                }
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter), // Align to the bottom center
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Button(
                                                onClick = {
                                                    val missing = buildList {
                                                        if (tipoActividad.value.isNullOrBlank()) add("Tipo de actividad")
                                                        if (actividad.value.isNullOrBlank()) add("Actividad")
                                                        if (conQuien.value.isNullOrBlank()) add("Con quién")
                                                        if (donde.value.isNullOrBlank()) add("Dónde")
                                                        if (fecha.value.isNullOrBlank()) add("Fecha y Hora")
                                                    }
                                                    shouldExecuteFinalAction = missing.isEmpty()
                                                    if (missing.isNotEmpty()) {
                                                        summaryMessage =
                                                            "Faltan completar los siguientes campos:\n\n• " +
                                                                    missing.joinToString("\n• ")
                                                    } else {
                                                        summaryMessage = """
                                                            Resumen de registro:
                                                            • Tipo de actividad: ${tipoActividad.value}

                                                            • Actividad: ${actividad.value}

                                                            • Con quién: ${conQuien.value}

                                                            • Dónde: ${donde.value}

                                                            • Fecha y hora: ${fecha.value}
                                                        """.trimIndent()
                                                    }
                                                    showSummaryDialog = true
                                                },

                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorResource(R.color.principal),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
                                            }
                                            if (showSummaryDialog) {
                                                SimpleTextDialog(
                                                    show = showSummaryDialog,
                                                    onDismissRequest = { showSummaryDialog = false },
                                                    title = "Resultado",
                                                    description = summaryMessage,
                                                    confirmLabel = "OK",
                                                    onConfirm = {
                                                        if (shouldExecuteFinalAction) {
                                                            onFinish()
                                                        }
                                                    }
                                                )
                                            }
                                        }

                }


            }
        )
    }
}