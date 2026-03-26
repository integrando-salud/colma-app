package com.colma.colmaApp.features.MetasHistory.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.MetasHistory.data.MetasHistoryRepositoryImpl
import com.colma.colmaApp.features.MetasHistory.viewmodel.MetasHistoryUiState
import com.colma.colmaApp.features.MetasHistory.viewmodel.MetasHistoryViewModel
import com.colma.colmaApp.features.MetasHistory.viewmodel.MetasHistoryViewModelFactory
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import java.text.SimpleDateFormat
import java.util.*

class MetasHistoryUI : ComponentActivity() {

    private val viewModel: MetasHistoryViewModel by viewModels {
        MetasHistoryViewModelFactory(MetasHistoryRepositoryImpl(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColmaAppTheme {
                val uiState by viewModel.uiState.collectAsState()
                MetasHistoryScreen(
                    uiState = uiState,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasHistoryScreen(uiState: MetasHistoryUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Metas", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.principal)
                )
            )
        },
        containerColor = colorResource(id = R.color.background)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (uiState.metas.isEmpty()) {
                Text(
                    text = "Aún no has creado ninguna meta.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp).align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.metas, key = { it.id_meta }) { meta ->
                        MetaItemCard(meta = meta)
                    }
                }
            }
        }
    }
}

@Composable
fun MetaItemCard(meta: RegistrosSaludModelos.MetaRecord) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val friendlyDateFormatter = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.getDefault()) }

    val statusText: String
    val statusColor: Color

    val fechaObjetivoDate = meta.fecha_objetivo?.let {
        try {
            dateFormatter.parse(it)
        } catch (e: Exception) {
            null
        }
    }

    val isPast = fechaObjetivoDate?.before(Date()) ?: false

    when (meta.meta_cumplida) {
        0 -> { // Pendiente
            if (isPast) {
                statusText = "Pendiente (Vencida)"
                statusColor = Color(0xFFFF9800) // Orange
            } else {
                statusText = "Pendiente"
                statusColor = Color(0xFFFBC02D) // Yellow (Darker for readability)
            }
        }
        1 -> { // No Cumplida
            statusText = "No Cumplida"
            statusColor = Color.Red
        }
        2 -> { // Cumplida
            statusText = "Completada"
            statusColor = Color(0xFF008000) // Green
        }
        else -> {
            statusText = "Desconocido"
            statusColor = Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meta.actividad ?: "Actividad no especificada",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(statusColor, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            MetaInfoRow(label = "Objetivo para el", value = fechaObjetivoDate?.let { friendlyDateFormatter.format(it) } ?: "N/A")
            MetaInfoRow(label = "Lugar", value = meta.lugar)
            MetaInfoRow(label = "Acompañante", value = meta.acompañante)

            if (meta.meta_cumplida == 1 && !meta.motivo_incumplimiento.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                MetaInfoRow(label = "Motivo", value = meta.motivo_incumplimiento)
            }
        }
    }
}

@Composable
fun MetaInfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                text = "$label: ",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
