package com.colma.colmaApp.utils.varios

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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

/**
 * Card that opens a Date+Time dialog. The selected datetime is returned as "yyyy-MM-dd HH:mm:ss".
 * The preview on the card updates while the user picks date/time (live feedback).
 */
@Composable
fun CardThatOpensDateTimeDialog(
    title: String = "Seleccionar fecha y hora",
    description: String? = null,
    initialMillis: Long? = null,
    modifier: Modifier = Modifier,
    onDateTimeSelected: (String) -> Unit = {}
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current

    // preview text state (keeps last confirmed selection or initial)
    val previewFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    var previewMillis by rememberSaveable { mutableStateOf(initialMillis ?: System.currentTimeMillis()) }

    // always compute previewText from previewMillis so it updates when previewMillis changes
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

                // if a description is provided, show it first (short)
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                // always show the live preview below (smaller)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Icon(
                painter = painterResource(id = R.drawable.outline_more_time_24),
                contentDescription = "Fecha y hora",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showDialog) {
        DateTimePickerDialog(
            context = ctx,
            initialMillis = previewMillis,
            onDismissRequest = { showDialog = false },
            // update preview while the user selects
            onSelectionChange = { cal ->
                previewMillis = cal.timeInMillis
            },
            onConfirm = { selectedCal ->
                val formatted = previewFormat.format(selectedCal.time)
                previewMillis = selectedCal.timeInMillis
                showDialog = false
                onDateTimeSelected(formatted)
            }
        )
    }
}

/**
 * Dialog that shows current selection and uses platform pickers.
 *
 * Uses a state backed Long (millis) so Compose will recompose on changes coming from pickers.
 */
@Composable
fun DateTimePickerDialog(
    context: Context,
    initialMillis: Long,
    onDismissRequest: () -> Unit,
    onSelectionChange: (Calendar) -> Unit = {},
    onConfirm: (Calendar) -> Unit
) {
    // state with selected millis (Compose will recompose when this changes)
    var selectedMillis by rememberSaveable { mutableStateOf(initialMillis) }

    // helper to get a calendar from millis
    fun calFromMillis(ms: Long): Calendar {
        return Calendar.getInstance().apply { timeInMillis = ms }
    }

    // formatters
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val fullFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // open date picker (reads current selectedMillis to initialize)
    fun openDatePicker() {
        val c = calFromMillis(selectedMillis)
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH)
        val d = c.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, yy, mm, dd ->
            val cc = calFromMillis(selectedMillis)
            cc.set(Calendar.YEAR, yy)
            cc.set(Calendar.MONTH, mm)
            cc.set(Calendar.DAY_OF_MONTH, dd)
            // keep time fields unchanged
            selectedMillis = cc.timeInMillis
            onSelectionChange(cc) // emit selection change for live feedback
        }, y, m, d).show()
    }

    // open time picker (24-hour). We set seconds to previous seconds (or 0).
    fun openTimePicker() {
        val c = calFromMillis(selectedMillis)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)

        TimePickerDialog(context, { _, h, min ->
            val cc = calFromMillis(selectedMillis)
            cc.set(Calendar.HOUR_OF_DAY, h)
            cc.set(Calendar.MINUTE, min)
            // preserve seconds if previously set, otherwise set 0
            cc.set(Calendar.SECOND, second)
            selectedMillis = cc.timeInMillis
            onSelectionChange(cc) // emit selection change for live feedback
        }, hour, minute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        title = { Text(text = "Seleccionar fecha y hora") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val currentCal = calFromMillis(selectedMillis)
                Text(text = "Fecha: ${dateFmt.format(currentCal.time)}")
                Text(text = "Hora: ${timeFmt.format(currentCal.time)}")
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Completo: ${fullFmt.format(currentCal.time)}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { openDatePicker() },
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp) // keep consistent height if you want
                    ) {
                        Text("Fecha")
                    }

                    Button(
                        onClick = { openTimePicker() },
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                    ) {
                        Text("Hora")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cal = calFromMillis(selectedMillis)
                onConfirm(cal)
            }) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        }
    )
}
