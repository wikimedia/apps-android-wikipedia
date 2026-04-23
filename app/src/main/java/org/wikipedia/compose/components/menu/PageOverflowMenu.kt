package org.wikipedia.compose.components.menu

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun PageOverflowMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<Pair<String, () -> Unit>>
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = WikipediaTheme.colors.paperColor
    ) {
        items.forEach { (label, action) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                       },
                onClick = {
                    action()
                    onDismiss()
                }
            )
        }
    }
}
