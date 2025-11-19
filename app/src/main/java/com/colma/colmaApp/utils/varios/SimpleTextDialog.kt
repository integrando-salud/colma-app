import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier

/**
 * Simple dialog that shows a title, a description and a single confirm button.
 *
 * @param show whether the dialog is visible
 * @param onDismissRequest called when the user dismisses the dialog
 * @param title dialog title
 * @param description dialog body text
 * @param confirmLabel label for the confirm button
 * @param onConfirm called when user presses confirm (dialog will still be dismissed by caller if desired)
 */
@Composable
fun SimpleTextDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    description: String,
    confirmLabel: String = "OK",
    onConfirm: () -> Unit = {}
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(confirmLabel)
            }
        }
    )
}
