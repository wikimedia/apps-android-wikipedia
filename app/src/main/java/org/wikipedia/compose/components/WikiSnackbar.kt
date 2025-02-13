package org.wikipedia.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.WikipediaTheme

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
                    onClick = onActionClick,
                    modifier = Modifier.padding(end = 8.dp)
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
        modifier = modifier,
        containerColor = WikipediaTheme.colors.borderColor
    ) {
        Text(
            text = message,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
                letterSpacing = 0.sp,
            ),
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 0.dp, bottom = 0.dp, start = 0.dp, end = 8.dp)
        )
    }
}
