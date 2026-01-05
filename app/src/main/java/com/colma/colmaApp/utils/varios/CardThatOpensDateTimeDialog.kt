package com.colma.colmaApp.utils.varios

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CardThatOpensDateDialog(
    title: String = "Seleccionar fecha",
    description: String? = null,
    initialMillis: Long? = null,
    modifier: Modifier = Modifier,
    onDateSelected: (Calendar) -> Unit = {}
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current

    val previewFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var previewMillis by rememberSaveable { mutableStateOf(initialMillis ?: System.currentTimeMillis()) }

    val previewText = remember(previewMillis) { previewFormat.format(Date(previewMillis)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Icon(
                painter = painterResource(id = R.drawable.outline_more_time_24),
                contentDescription = "Fecha",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showDialog) {
        DatePickerDialogComposable(
            context = ctx,
            initialMillis = previewMillis,
            onDismissRequest = { showDialog = false },
            onConfirm = { selectedCal ->
                previewMillis = selectedCal.timeInMillis
                showDialog = false
                onDateSelected(selectedCal)
            }
        )
    }
}

@Composable
fun DatePickerDialogComposable(
    context: Context,
    initialMillis: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Calendar) -> Unit
) {
    var selectedMillis by rememberSaveable { mutableStateOf(initialMillis) }

    fun calFromMillis(ms: Long): Calendar {
        return Calendar.getInstance().apply { timeInMillis = ms }
    }

    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun openDatePicker() {
        val c = calFromMillis(selectedMillis)
        DatePickerDialog(context, { _, yy, mm, dd ->
            val cc = calFromMillis(selectedMillis)
            cc.set(Calendar.YEAR, yy)
            cc.set(Calendar.MONTH, mm)
            cc.set(Calendar.DAY_OF_MONTH, dd)
            selectedMillis = cc.timeInMillis
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        title = { Text(text = "Seleccionar fecha") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val currentCal = calFromMillis(selectedMillis)
                Text(text = "Fecha: ${dateFmt.format(currentCal.time)}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { openDatePicker() },
                    modifier = Modifier.fillMaxWidth().height(55.dp)
                ) {
                    Text("Cambiar Fecha")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = calFromMillis(selectedMillis)
                    onConfirm(cal)
                },
                colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(containerColor = Color.Red, contentColor = Color.White)
            ) { Text("Cancelar") }
        }
    )
}

