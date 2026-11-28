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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingList

enum class PageOverflowMenuItem {
    OPEN_PAGE,
    OPEN_IN_NEW_TAB,
    SAVE,
    ADD_TO_ANOTHER_LIST,
    REMOVE_FROM_LISTS,
    MOVE_TO_ANOTHER_LIST,
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
            PageOverflowMenuItem.entries.filter { it.isVisible(state.lists) }.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label(state.lists),
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

private fun PageOverflowMenuItem.isVisible(lists: List<ReadingList>): Boolean {
    return when (this) {
        PageOverflowMenuItem.SAVE -> lists.isEmpty()
        PageOverflowMenuItem.ADD_TO_ANOTHER_LIST,
        PageOverflowMenuItem.REMOVE_FROM_LISTS -> lists.isNotEmpty()
        PageOverflowMenuItem.MOVE_TO_ANOTHER_LIST -> lists.size == 1
        else -> true
    }
}

@Composable
private fun PageOverflowMenuItem.label(lists: List<ReadingList>): String {
    return when (this) {
        PageOverflowMenuItem.OPEN_PAGE -> stringResource(R.string.menu_long_press_open_page)
        PageOverflowMenuItem.OPEN_IN_NEW_TAB -> stringResource(R.string.menu_long_press_open_in_new_tab)
        PageOverflowMenuItem.SAVE -> stringResource(R.string.feed_card_add_to_default_list)
        PageOverflowMenuItem.ADD_TO_ANOTHER_LIST -> stringResource(R.string.reading_list_add_to_other_list)
        PageOverflowMenuItem.REMOVE_FROM_LISTS -> if (lists.size == 1) {
            stringResource(R.string.reading_list_remove_from_list, lists[0].title)
        } else {
            stringResource(R.string.reading_list_remove_from_lists)
        }
        PageOverflowMenuItem.MOVE_TO_ANOTHER_LIST -> stringResource(R.string.reading_list_move_from_to_other_list, lists.firstOrNull()?.title.orEmpty())
        PageOverflowMenuItem.SHARE -> stringResource(R.string.menu_page_share)
        PageOverflowMenuItem.COPY_LINK -> stringResource(R.string.menu_long_press_copy_page)
    }
}
