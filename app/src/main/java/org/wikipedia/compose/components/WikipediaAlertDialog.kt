package org.wikipedia.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WikipediaAlertDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    confirmButtonText: String,
    confirmButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    dismissButtonText: String,
    dismissButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit
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
                colors = ButtonDefaults.textButtonColors(contentColor = confirmButtonColor),
                onClick = onConfirmButtonClick
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = dismissButtonColor),
                onClick = onDismissButtonClick
            ) {
                Text(dismissButtonText)
            }
        }
    )
}
