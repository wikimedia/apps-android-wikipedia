package org.wikipedia.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WikipediaAlertDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        title = {
            Text(
                text = title,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        text = {
            Text(
                text = message,
                color = WikipediaTheme.colors.secondaryColor
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WikipediaTheme.colors.progressiveColor
                ),
                onClick = onConfirmButtonClick
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WikipediaTheme.colors.progressiveColor
                ),
                onClick = onDismissButtonClick
            ) {
                Text(dismissButtonText)
            }
        }
    )
}
