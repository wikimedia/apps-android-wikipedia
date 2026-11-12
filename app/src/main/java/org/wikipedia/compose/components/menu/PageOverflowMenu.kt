package org.wikipedia.compose.components.menu

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.history.HistoryEntry

enum class PageOverflowMenuItem {
    OPEN_PAGE,
    OPEN_IN_NEW_TAB,
    SAVE,
    SHARE,
    COPY_LINK
}

@Composable
fun PageOverflowMenu(
    modifier: Modifier = Modifier,
    menuKey: String,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState?,
    onDismiss: () -> Unit,
    onItemClick: (PageOverflowMenuItem, HistoryEntry) -> Unit
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
        overflowMenuState?.let { state ->
            PageOverflowMenuItem.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(item.labelRes(state.isArticleSaved)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    },
                    onClick = {
                        onItemClick(item, state.entry)
                        animatedExpanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@StringRes
private fun PageOverflowMenuItem.labelRes(isArticleSaved: Boolean): Int {
    return when (this) {
        PageOverflowMenuItem.OPEN_PAGE -> R.string.menu_long_press_open_page
        PageOverflowMenuItem.OPEN_IN_NEW_TAB -> R.string.menu_long_press_open_in_new_tab
        // A single entry either way: both states open the save sheet, which is where adding to and
        // removing from collections now happens.
        PageOverflowMenuItem.SAVE -> if (isArticleSaved) R.string.link_preview_dialog_saved_button else R.string.feed_card_add_to_default_list
        PageOverflowMenuItem.SHARE -> R.string.menu_page_share
        PageOverflowMenuItem.COPY_LINK -> R.string.menu_long_press_copy_page
    }
}
