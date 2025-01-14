package org.wikipedia.compose.styles

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.ComposeStyles
import org.wikipedia.compose.theme.WikipediaTheme

object WikipediaStyle {

    @Composable
    fun Snackbar(
        message: String,
        actionLabel: String? = null,
        onActionClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        Snackbar(
            action = {
                if (actionLabel != null && onActionClick != null) {
                    TextButton(
                        onClick = onActionClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = ComposeStyles.H3_Button.copy(
                                color = WikipediaTheme.colors.progressiveColor
                            )
                        )
                    }
                }
            },
            modifier = modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = ComposeStyles.H3_Button.copy(
                    color = WikipediaTheme.colors.primaryColor,
                    letterSpacing = 0.sp
                ),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 0.dp, bottom = 0.dp, start = 0.dp, end = 8.dp)
            )
        }
    }
}
