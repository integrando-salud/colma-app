package com.codeco.tabacaleros.features.historial

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.utils.configureSystemBars
import java.time.LocalDate

class HistorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        setContent {
            TabacalerosTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HistorialScreen(onBack = { finish() })
                }
            }
        }
    }
}

data class HistoryEntry(
    val id: Int,
    val date: LocalDate,
    val plantsQuantity: Int,
    val cuadros: Int,
    val weight: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Historial de Registros", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.principal),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = colorResource(R.color.background)
    ) { paddingValues ->
        val sampleEntries = remember {
            listOf(
                HistoryEntry(1, LocalDate.of(2023, 10, 26), 20000, 5, 1500),
                HistoryEntry(2, LocalDate.of(2023, 11, 15), 15000, 4, 1200),
                HistoryEntry(3, LocalDate.of(2023, 12, 1), 22000, 6, 1800),
                HistoryEntry(4, LocalDate.of(2024, 1, 10), 18000, 5, 1400),
                HistoryEntry(5, LocalDate.of(2024, 2, 5), 25000, 7, 2000)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(sampleEntries) { entry ->
                HistoryEntryCard(entry = entry)
            }
        }
    }
}

@Composable
fun HistoryEntryCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.backgroundLight))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fecha: ${entry.date}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Plantas:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${entry.plantsQuantity}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Cuadros:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${entry.cuadros}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Peso (kg):", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${entry.weight}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

