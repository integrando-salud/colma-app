package com.colma.colmaApp.features.EstadoAnimoMenu

import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.RegistroSalud.HealthEntry
import com.colma.colmaApp.features.RegistroSalud.HealthRecordCard
import com.colma.colmaApp.features.RegistroSalud.HistoryCard
import com.colma.colmaApp.features.estadoAnimo.EstadoAnimoActivity
import com.colma.colmaApp.features.estadoAnimo.EstadoAnimoScreen
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class EstadoAnimoMenuActivity : ComponentActivity() {
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
                EstadoAnimoMenuScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoAnimoMenuScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val dbHelper = remember { DBhelper(context) }

    // in-memory history (newest first)
    val history = remember { mutableStateListOf<HealthEntry>() }

    // Reuse the same visual formatter used elsewhere
    val displayFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // Helper: parse ISO "yyyy-MM-dd HH:mm:ss" -> millis, fallback tries Long
    fun parseDateTimeToMillis(datetime: String?): Long? {
        if (datetime.isNullOrBlank()) return null
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            iso.parse(datetime)?.time
        } catch (e: ParseException) {
            try {
                datetime.toLong()
            } catch (ex: Exception) {
                null
            }
        }
    }

    val scope = rememberCoroutineScope()

    // ActivityResult launcher that will refresh the list when EstadoAnimoActivity returns RESULT_OK
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh DB on IO
            scope.launch {
                try {
                    val all = withContext(Dispatchers.IO) { dbHelper.getAllEstadoAnimo(null) }
                    val mapped = all.mapNotNull { e ->
                        val datetime = e.fechaEdicion ?: e.fechaRegistro
                        val millis = parseDateTimeToMillis(datetime) ?: System.currentTimeMillis()
                        val answeredCount = e.preguntas.count { it != null }
                        val display = if (answeredCount > 0) "$answeredCount respuestas" else "Sin respuestas"
                        HealthEntry("Estado de ánimo", display, millis)
                    }.sortedByDescending { it.timestamp }

                    history.clear()
                    history.addAll(mapped)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // initial load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val all = withContext(Dispatchers.IO) { dbHelper.getAllEstadoAnimo(null) }
                val mapped = all.mapNotNull { e ->
                    val datetime = e.fechaEdicion ?: e.fechaRegistro
                    val millis = parseDateTimeToMillis(datetime) ?: System.currentTimeMillis()
                    val answeredCount = e.preguntas.count { it != null }
                    val display = if (answeredCount > 0) "$answeredCount respuestas" else "Sin respuestas"
                    HealthEntry("Estado de ánimo", display, millis)
                }.sortedByDescending { it.timestamp }

                history.clear()
                history.addAll(mapped)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Estado de ánimo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.res.colorResource(com.colma.colmaApp.R.color.principal),
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top card that navigates to the existing EstadoAnimoActivity via the launcher
            HealthRecordCard(
                label = "Estado de ánimo",
                onClick = {
                    val intent = Intent(context, EstadoAnimoActivity::class.java)
                    // Launch via ActivityResult to get a callback
                    launcher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // History: filtered to Estado de ánimo entries
            val estadoHistory = history.filter { it.label.equals("Estado de ánimo", ignoreCase = true) || it.label.equals("EstadoAnimo", ignoreCase = true) }
            HistoryCard(history = estadoHistory, onClear = { history.removeAll(estadoHistory) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp))
        }
    }
}