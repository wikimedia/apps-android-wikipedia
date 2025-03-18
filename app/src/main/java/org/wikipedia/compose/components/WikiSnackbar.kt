package org.wikipedia.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun Snackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Snackbar(
        action = {
            if (actionLabel != null && onActionClick != null) {
                TextButton(
                    onClick = onActionClick
                ) {
                    Text(
                        text = actionLabel,
                        style = WikipediaTheme.typography.h3.copy(
                            color = WikipediaTheme.colors.progressiveColor
                        )
                    )
                }
            }
        },
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = WikipediaTheme.colors.borderColor
    ) {
        HtmlText(
            text = message,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor
            ),
            maxLines = 10
        )
    }
}

@Preview
@Composable
private fun SnackbarPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        Snackbar(
            message = "This is an <strong>example</strong> Snackbar (with <a href=\"#foo\">html</a>)!",
            actionLabel = "Click here!",
            onActionClick = {}
        )
    }
}
