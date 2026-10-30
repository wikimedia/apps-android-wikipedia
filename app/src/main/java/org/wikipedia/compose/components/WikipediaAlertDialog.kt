package org.wikipedia.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WikipediaAlertDialog(
    modifier: Modifier = Modifier,
    title: String,
    titleModifier: Modifier = Modifier,
    titleTextAlign: TextAlign = TextAlign.Start,
    message: String,
    messageModifier: Modifier = Modifier,
    messageTextAlign: TextAlign = TextAlign.Start,
    image: @Composable (() -> Unit)? = null,
    confirmButtonText: String,
    confirmButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    dismissButtonText: String? = null,
    dismissButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit = { onDismissRequest() },
) {
    AlertDialog(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        icon = image,
        title = {
            Text(
                modifier = titleModifier,
                text = title,
                color = WikipediaTheme.colors.primaryColor,
                textAlign = titleTextAlign
            )
        },
        text = {
            Text(
                modifier = messageModifier,
                text = message,
                color = WikipediaTheme.colors.secondaryColor,
                textAlign = messageTextAlign
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
            dismissButtonText?.let {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = dismissButtonColor),
                    onClick = onDismissButtonClick
                ) {
                    Text(dismissButtonText)
                }
            }
        }
    )
}
