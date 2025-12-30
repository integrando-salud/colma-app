package com.colma.colmaApp.utils.varios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun <T> CardThatOpensSelectionDialog(
    title: String = "Seleccionar",
    description: String? = null,
    items: List<T>,
    label: (T) -> String = { it.toString() },
    onItemSelected: (T) -> Unit = {},
    initialSelection: String? = null, // New parameter
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Initialize selectedLabel using initialSelection if provided, else description, else empty string
    var selectedLabel by rememberSaveable {
        mutableStateOf(initialSelection ?: description ?: "")
    }

    // index used inside dialog to highlight selection; not saved across process death (T might not be savable)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Card UI (same visual as your base card)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable {
                // preselect previous selection if we have a label that matches an item
                selectedIndex = items.indexOfFirst { label(it) == selectedLabel }.takeIf { it >= 0 }
                showDialog = true
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (selectedLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!description.isNullOrBlank()) {
                    // show initial description if nothing selected yet
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                painter = painterResource(id = android.R.drawable.ic_dialog_info),
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
                if (items.isEmpty()) {
                    Text(text = "No hay elementos para seleccionar.")
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                    ) {
                        LazyColumn {
                            itemsIndexed(items) { idx, item ->
                                val isSelected = selectedIndex == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedIndex = idx }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedIndex = idx }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label(item),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                                if (idx < items.lastIndex) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idx = selectedIndex
                        if (idx != null && idx in items.indices) {
                            val chosen = items[idx]
                            selectedLabel = label(chosen)
                            onItemSelected(chosen)
                        }
                        showDialog = false
                    },
                    enabled = selectedIndex != null,
                    colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}