package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs
import java.util.*

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
            Prefs.customizeFavoritesQuickActionsOrder = listOf(0, 1, 2, 3, 4)
            Prefs.customizeFavoritesMenuOrder = listOf(5, 6, 7, 8, 9, 10)
        }
    }

    private fun processList() {
        // Quick actions
        fullList.add(headerPair(true))
        fullList.addAll(addItemsOrEmptyPlaceholder(quickActionsOrder, true))
        // Menu
        fullList.add(headerPair(false))
        fullList.addAll(addItemsOrEmptyPlaceholder(menuOrder, false))
    }

    private fun addItemsOrEmptyPlaceholder(list: List<Int>, quickActions: Boolean): List<Pair<Int, Any>> {
        return if (list.isEmpty()) {
            listOf(emptyPlaceholderPair(quickActions))
        } else {
            list.map { CustomizeFavoritesFragment.VIEW_TYPE_ITEM to PageMenuItem.find(it) }
        }
    }

    private fun headerPair(quickActions: Boolean): Pair<Int, Any> {
        return CustomizeFavoritesFragment.VIEW_TYPE_HEADER to
                if (quickActions) R.string.customize_favorites_category_quick_actions else R.string.customize_favorites_category_menu
    }

    private fun emptyPlaceholderPair(quickActions: Boolean): Pair<Int, Any> {
        return CustomizeFavoritesFragment.VIEW_TYPE_EMPTY_PLACEHOLDER to quickActions
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(fullList, oldPosition, newPosition)
        // First category is empty
        if (fullList[1].first == CustomizeFavoritesFragment.VIEW_TYPE_HEADER) {
            fullList.add(1, emptyPlaceholderPair(true))
        }
        // Last category is empty
        if (fullList.last().first == CustomizeFavoritesFragment.VIEW_TYPE_HEADER) {
            fullList.add(emptyPlaceholderPair(false))
        }
    }

    fun removePlaceholder() {
        // TODO: fix the crash
//        if (fullList.indexOf(headerPair(true)) > 2) {
//            fullList.remove(emptyPlaceholderPair(true))
//        }
//        if (fullList.indexOf(headerPair(false)) < fullList.size - 1) {
//            fullList.remove(emptyPlaceholderPair(false))
//        }
    }
}
