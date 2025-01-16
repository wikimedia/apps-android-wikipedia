package org.wikipedia.compose.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

data class NewTab(
    @StringRes val text: Int,
    val id: Int,
    @DrawableRes var selectedIcon: Int,
    @DrawableRes var unSelectedIcon: Int,
)

val newNavTabsList = listOf(
    NewTab(
        text = R.string.feed,
        id = R.id.nav_tab_explore,
        selectedIcon = R.drawable.explore_bold,
        unSelectedIcon = R.drawable.explore_bold,
    ),
    NewTab(
        text = R.string.nav_item_saved,
        id = R.id.nav_tab_reading_lists,
        selectedIcon = R.drawable.ic_bookmark_white_24dp,
        unSelectedIcon = R.drawable.ic_bookmark_border_white_24dp,
    ),
    NewTab(
        text = R.string.nav_item_search,
        id = R.id.nav_tab_search,
        selectedIcon = R.drawable.search_bold,
        unSelectedIcon = R.drawable.ic_search_white_24dp,
    ),
    NewTab(
        text = R.string.nav_item_suggested_edits,
        id = R.id.nav_tab_edits,
        selectedIcon = R.drawable.ic_mode_edit_white_24dp,
        unSelectedIcon = R.drawable.outline_edit_24,
    ),
    NewTab(
        text = R.string.nav_item_more,
        id = R.id.nav_tab_more,
        selectedIcon = R.drawable.ic_menu_white_24dp,
        unSelectedIcon = R.drawable.ic_menu_white_24dp,
    ),
)

@Composable
fun CustomNavigationBar(
    modifier: Modifier = Modifier,
    newTabs: List<NewTab>
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    NavigationBar(
        containerColor = WikipediaTheme.colors.paperColor,
        content = {
            newTabs.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = {
                       Icon(painter = painterResource(
                           if (selectedItem == index) item.selectedIcon else item.unSelectedIcon
                       ), contentDescription = null)
                    },
                    label = {
                        Text(
                            text = stringResource(item.text)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WikipediaTheme.colors.destructiveColor,
                        unselectedIconColor = WikipediaTheme.colors.placeholderColor,
                        selectedTextColor = WikipediaTheme.colors.destructiveColor,
                        unselectedTextColor = WikipediaTheme.colors.placeholderColor,
                    ),
                    selected = selectedItem == index,
                    onClick = {
                        selectedItem = index
                    },
                )
            }
        }
    )
}
