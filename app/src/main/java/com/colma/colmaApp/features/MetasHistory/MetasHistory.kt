package com.colma.colmaApp.features.MetasHistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MetasHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColmaAppTheme {
                MetasHistoryScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DBhelper(context) }
    var metas by remember { mutableStateOf<List<RegistrosSaludModelos.MetaRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Assuming user id is 1 for now
        metas = dbHelper.getAllMetas(1)
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (metas.isEmpty()) {
                item {
                    Text(
                        text = "Aún no has creado ninguna meta.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(metas, key = { it.id_meta }) { meta ->
                    MetaItemCard(meta = meta)
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
                statusColor = Color.Red
            } else {
                statusText = "Pendiente"
                statusColor = Color(0xFFFFA500) // Orange
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
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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