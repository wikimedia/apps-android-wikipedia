package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs

class CustomizeFavoritesViewModel : ViewModel() {

    private var menuOrder = mutableListOf<Int>()
    private var quickActionsOrder = mutableListOf<Int>()

    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Pair<Int, Any>>()

    init {
        menuOrder = Prefs.customizeFavoritesMenuOrder as MutableList<Int>
        quickActionsOrder = Prefs.customizeFavoritesQuickActionsOrder as MutableList<Int>
        processList()
    }

    private fun processList() {
        // Quick actions
        fullList.add(CustomizeFavoritesFragment.VIEW_TYPE_HEADER to R.string.customize_favorites_category_quick_actions)
        fullList.addAll(addItemsOrEmptyPlaceholder(quickActionsOrder))
        // Menu
        fullList.add(CustomizeFavoritesFragment.VIEW_TYPE_HEADER to R.string.customize_favorites_category_menu)
        fullList.addAll(addItemsOrEmptyPlaceholder(menuOrder))
    }

    private fun addItemsOrEmptyPlaceholder(list: List<Int>): List<Pair<Int, Any>> {
        return if (list.isEmpty()) {
            listOf(CustomizeFavoritesFragment.VIEW_TYPE_EMPTY_PLACEHOLDER to "")
        } else {
            quickActionsOrder.map { CustomizeFavoritesFragment.VIEW_TYPE_ITEM to PageMenuItem.find(it) }
        }
    }
}
