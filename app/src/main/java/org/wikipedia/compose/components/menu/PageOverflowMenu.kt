package org.wikipedia.compose.components.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun PageOverflowMenu(
    modifier: Modifier = Modifier,
    menuKey: String,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState?,
    onDismiss: () -> Unit,
    items: List<Pair<String, () -> Unit>>
) {
    val expanded = menuKey == overflowMenuState?.menuKey
    var animatedExpanded by remember(menuKey) { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            animatedExpanded = true
        } else if (animatedExpanded) {
            animatedExpanded = false
        }
    }

    LaunchedEffect(animatedExpanded, expanded) {
        if (!animatedExpanded && expanded) {
            delay(150)
            onDismiss()
        }
    }

    DropdownMenu(
        modifier = modifier,
        expanded = animatedExpanded,
        onDismissRequest = { animatedExpanded = false },
        containerColor = WikipediaTheme.colors.paperColor,
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
                    animatedExpanded = false
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
