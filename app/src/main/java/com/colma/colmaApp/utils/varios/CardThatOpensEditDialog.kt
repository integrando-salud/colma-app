package com.colma.colmaApp.utils.varios

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colma.colmaApp.R


@Composable
fun CardThatOpensEditDialog(
    title: String = "Abrir diálogo",
    description: String? = null,
    placeholder: String = "Escribe una descripción...",
    confirmLabel: String = "Guardar",
    cancelLabel: String = "Cancelar",
    onSave: (String) -> Unit = {},
    initialText: String = "",
    modifier: Modifier = Modifier
) {
    var savedText by rememberSaveable { mutableStateOf(initialText) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                Spacer(modifier = Modifier.height(6.dp))
                if (savedText.isNotBlank()) {
                    Text(
                        text = savedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!description.isNullOrBlank()){
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Hint color
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showDialog) {
        var editBuffer by rememberSaveable { mutableStateOf(savedText) }

        LaunchedEffect(showDialog) {
            if (showDialog) editBuffer = savedText
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color.White,
            title = { Text(text = title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editBuffer,
                        onValueChange = { editBuffer = it },
                        placeholder = { Text(text = placeholder) },
                        singleLine = false,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 220.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editBuffer.trim()
                        savedText = trimmed
                        onSave(trimmed)
                        showDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(containerColor = colorResource(id = R.color.rojo_boton), contentColor = Color.White)
                ) { Text(cancelLabel) }
            }
        )
    }
}