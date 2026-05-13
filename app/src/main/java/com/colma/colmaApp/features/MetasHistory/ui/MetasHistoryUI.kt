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

import androidx.compose.foundation.lazy.rememberLazyListState
import com.colma.colmaApp.features.multimedia.ui.scrollbar

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
                title = { /* Title removed as per request */ },
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
                val lazyListState = rememberLazyListState()
                
                // Separate meta de la semana (active pending) from history
                val metaDeLaSemana = uiState.metas.filter { it.meta_cumplida == 0 }.take(1)
                val historialDeMetas = uiState.metas.filter { it.meta_cumplida != 0 || (it.meta_cumplida == 0 && !metaDeLaSemana.contains(it)) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().scrollbar(lazyListState),
                    state = lazyListState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (metaDeLaSemana.isNotEmpty()) {
                        item {
                            Text(
                                text = "Meta de la semana",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = Color(0xFF212121),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = Color(0xFFBDBDBD),
                                thickness = 1.dp
                            )
                        }
                        items(metaDeLaSemana, key = { "active_${it.id_meta}" }) { meta ->
                            MetaItemCard(meta = meta)
                        }
                    }

                    if (historialDeMetas.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Historial de metas",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = Color(0xFF212121),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = Color(0xFFBDBDBD),
                                thickness = 1.dp
                            )
                        }
                        items(historialDeMetas, key = { "history_${it.id_meta}" }) { meta ->
                            MetaItemCard(meta = meta)
                        }
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
            statusText = "PENDIENTE"
            statusColor = Color(0xFFFBC02D)
        }
        1 -> { // No Cumplida
            statusText = "No completada"
            statusColor = Color.Red
        }
        2 -> { // Completada
            statusText = "Completada"
            statusColor = Color(0xFF4CAF50) // Green
        }
        else -> {
            statusText = "Desconocido"
            statusColor = Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Activity is always shown
            Text(
                text = meta.actividad ?: "Actividad no especificada",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color(0xFF212121)
            )

            // Info conditional based on status
            when (meta.meta_cumplida) {
                0 -> { // Pendiente
                    Spacer(modifier = Modifier.height(12.dp))
                    fechaObjetivoDate?.let { date ->
                        MetaInfoRowImproved(
                            label = "Objetivo para el",
                            value = friendlyDateFormatter.format(date)
                        )
                    }
                }
                1 -> { // No Completada
                    if (!meta.motivo_incumplimiento.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MetaInfoRowImproved(
                            label = "Motivo de incumplimiento",
                            value = meta.motivo_incumplimiento
                        )
                    }
                }
                2 -> { // Completada
                    // Only show the meta and the label (already handled)
                }
            }

            // Status badge at bottom right
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(statusColor, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MetaInfoRowImproved(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Color(0xFF757575),
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color(0xFF424242),
            lineHeight = 20.sp
        )
    }
}
