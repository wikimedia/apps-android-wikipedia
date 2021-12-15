package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs
import java.util.*

class CustomizeFavoritesViewModel : ViewModel() {

    private var quickActionsOrder = mutableListOf<Int>()
    private var menuOrder = mutableListOf<Int>()

    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Pair<Int, Any>>()
        private set

    init {
        setupDefaultOrder(Prefs.customizeFavoritesMenuOrder.isEmpty() && Prefs.customizeFavoritesQuickActionsOrder.isEmpty())
        preProcessList()
    }

    private fun setupDefaultOrder(default: Boolean) {
        if (default) {
            Prefs.customizeFavoritesQuickActionsOrder = listOf(0, 1, 2, 3, 4)
            Prefs.customizeFavoritesMenuOrder = listOf(5, 6, 7, 8, 9, 10)
        }
        quickActionsOrder = Prefs.customizeFavoritesQuickActionsOrder.toMutableList()
        menuOrder = Prefs.customizeFavoritesMenuOrder.toMutableList()
    }

    private fun preProcessList() {
        fullList.clear()
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

    private fun collectCategoriesItems(): Pair<MutableList<Int>, MutableList<Int>> {
        var saveIntoQuickActions = true
        val quickActionsItems = mutableListOf<Int>()
        val menuItems = mutableListOf<Int>()
        fullList.filterNot { it.first == CustomizeFavoritesFragment.VIEW_TYPE_EMPTY_PLACEHOLDER }.forEach {
            if (it == headerPair(false)) {
                saveIntoQuickActions = false
            }
            if (it.first == CustomizeFavoritesFragment.VIEW_TYPE_ITEM) {
                if (saveIntoQuickActions) {
                    quickActionsItems.add((it.second as PageMenuItem).id)
                } else {
                    menuItems.add((it.second as PageMenuItem).id)
                }
            }
        }
        return quickActionsItems to menuItems
    }

    // First: Quick actions; Second: Menu
    private fun handleCategoryLimitation(pair: Pair<MutableList<Int>, MutableList<Int>>): List<Int> {
        val list = mutableListOf<Int>()
        // To avoid seeing the bug from the library, we have to use a while loop to manually swap items.
        while (pair.first.size > CustomizeFavoritesFragment.QUICK_ACTIONS_LIMIT) {
            // Last item swap with "Menu" header
            swapList(pair.first.size, pair.first.size + 1)
            // Add swapped item to list
            list.add(pair.first.size)
            // Add to "Menu" order list and remove the last item of Quick actions
            pair.second.add(0, pair.first.removeLast())
        }
        return list
    }

    fun resetToDefault() {
        setupDefaultOrder(true)
        preProcessList()
        saveChanges()
    }

    fun saveChanges(): List<Int> {
        val pair = collectCategoriesItems()
        val rearrangedItems = handleCategoryLimitation(pair)

        quickActionsOrder = pair.first
        menuOrder = pair.second
        Prefs.customizeFavoritesQuickActionsOrder = quickActionsOrder
        Prefs.customizeFavoritesMenuOrder = menuOrder

        return rearrangedItems
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(fullList, oldPosition, newPosition)
    }

    fun addEmptyPlaceholder(): Int {
        if (quickActionsOrder.isEmpty() && !fullList.contains(emptyPlaceholderPair(true))) {
            fullList.add(1, emptyPlaceholderPair(true))
            return 1
        }
        if (menuOrder.isEmpty() && !fullList.contains(emptyPlaceholderPair(false))) {
            fullList.add(emptyPlaceholderPair(false))
            return fullList.size - 1
        }
        return -1
    }

    fun removeEmptyPlaceholder(): Int {
        if (quickActionsOrder.isNotEmpty() && fullList.contains(emptyPlaceholderPair(true))) {
            val index = fullList.indexOf(emptyPlaceholderPair(true))
            fullList.removeAt(index)
            return index
        }
        if (menuOrder.isNotEmpty() && fullList.contains(emptyPlaceholderPair(false))) {
            val index = fullList.indexOf(emptyPlaceholderPair(false))
            fullList.removeAt(index)
            return index
        }
        return -1
    }
}
