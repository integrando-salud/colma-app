import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun CardThatOpensEditDialog(
    title: String = "Abrir diálogo",
    description: String? = null,
    placeholder: String = "Escribe una descripción...",
    confirmLabel: String = "Guardar",
    cancelLabel: String = "Cancelar",
    onSave: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // keep the "current" text shown on the card (editable)
    var currentText by rememberSaveable { mutableStateOf(description ?: "") }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Card UI: title + current description + trailing edit icon
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showDialog) {
        // Buffer for editing. Initialize from currentText when dialog opens.
        var editBuffer by rememberSaveable { mutableStateOf(currentText) }
        var error by remember { mutableStateOf<String?>(null) }

        // Keep the buffer in sync when the dialog re-opens (ensures it shows latest currentText)
        LaunchedEffect(showDialog) {
            if (showDialog) editBuffer = currentText
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {
                            editBuffer = it
                            if (!it.isBlank()) error = null
                        },
                        placeholder = { Text(text = placeholder) },
                        singleLine = false,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 220.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )
                    if (!error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editBuffer.trim()
                        if (trimmed.isBlank()) {
                            error = "La descripción no puede estar vacía"
                            return@TextButton
                        }
                        // update local card display + external callback
                        currentText = trimmed
                        onSave(trimmed)
                        showDialog = false
                    },
                    enabled = editBuffer.trim().isNotEmpty()
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(cancelLabel) }
            }
        )
    }
}