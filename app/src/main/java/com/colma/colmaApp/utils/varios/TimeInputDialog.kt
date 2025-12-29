package com.colma.colmaApp.utils.varios

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    if (show) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        val signatureColor = colorResource(R.color.principal)

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Seleccionar Hora", color = Color.Black) },
            text = {
                TimeInput(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        timeSelectorSelectedContainerColor = signatureColor,
                        timeSelectorUnselectedContainerColor = Color(0xFFF0F0F0),
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = Color.Black
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        onDismiss()
                    }
                ) {
                    Text("OK", color = signatureColor)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = signatureColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        )
    }
}