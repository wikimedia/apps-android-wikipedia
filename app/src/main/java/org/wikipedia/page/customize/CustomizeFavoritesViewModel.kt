package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs

class CustomizeFavoritesViewModel : ViewModel() {

    private var menuOrder = listOf<Int>()
    private var quickActionsOrder = listOf<Int>()

    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Pair<Int, Any>>()

    init {
        setupDefaultOrder()
        menuOrder = Prefs.customizeFavoritesMenuOrder
        quickActionsOrder = Prefs.customizeFavoritesQuickActionsOrder
        processList()
    }

    private fun setupDefaultOrder() {
        if (Prefs.customizeFavoritesMenuOrder.isEmpty() && Prefs.customizeFavoritesQuickActionsOrder.isEmpty()) {
            Prefs.customizeFavoritesMenuOrder = listOf(0, 1, 2, 3, 4)
            Prefs.customizeFavoritesQuickActionsOrder = listOf(5, 6, 7, 8, 9, 10)
        }
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
