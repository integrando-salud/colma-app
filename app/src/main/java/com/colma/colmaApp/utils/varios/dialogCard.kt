package com.colma.colmaApp.utils.varios

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CardThatOpensEmptyDialog(
    title: String = "Abrir diálogo",
    description: String? = null,            // new parameter
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)        // allow to grow if description present
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // column for title + optional description; takes remaining horizontal space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }

            // trailing icon on the right
            Icon(
                painter = painterResource(id = R.drawable.ic_dialog_info),
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color.White,
            title = { Text(text = title) },
            text = {
                // show description in the dialog, or a small fallback
                if (!description.isNullOrBlank()) {
                    Text(text = description)
                } else {
                    Text(text = "No hay descripción disponible.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}