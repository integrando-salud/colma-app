package com.colma.colmaApp.utils.varios

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R

/**
 * Simple dialog that shows a title, a description and a single confirm button.
 *
 * @param show whether the dialog is visible
 * @param onDismissRequest called when the user dismisses the dialog
 * @param title dialog title
 * @param description dialog body text
 * @param yesLabel label for the confirm button
 * @param noLabel label for the confirm button
 * @param onYes called when user presses confirm (dialog will still be dismissed by caller if desired)
 * @param onNo called when user presses dismiss
 */
@Composable
fun CustomAlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    description: String,
    yesLabel: String = "Sí",
    noLabel: String = "No",
    onYes: () -> Unit = {},
    onNo: () -> Unit = {}
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(text = title, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = description,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontSize = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onYes()
                },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.principal_boton), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(yesLabel, fontSize = 16.sp)
            }
        },
        dismissButton = {
            if (noLabel.isNotEmpty()) {
                Button(
                    onClick = {
                        onNo()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.rojo_boton), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(noLabel, fontSize = 16.sp)
                }
            }
        }
    )
}
