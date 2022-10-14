package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import java.util.*

class CustomizeToolbarViewModel : ViewModel() {

    private var toolbarOrder = mutableListOf<Int>()
    private var menuOrder = mutableListOf<Int>()

    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Pair<Int, Any>>()
        private set

    init {
        setupDefaultOrder(Prefs.customizeToolbarMenuOrder.isEmpty() && Prefs.customizeToolbarOrder.isEmpty())
        preProcessList()
    }

    private fun setupDefaultOrder(default: Boolean) {
        if (default) {
            Prefs.resetToolbarAndMenuOrder()
        }
        toolbarOrder = Prefs.customizeToolbarOrder.toMutableList()
        menuOrder = Prefs.customizeToolbarMenuOrder.toMutableList()
    }

    private fun preProcessList() {
        fullList.clear()
        // Description
        fullList.add(CustomizeToolbarFragment.VIEW_TYPE_DESCRIPTION to "")
        // Toolbar
        fullList.add(headerPair(true))
        fullList.addAll(addItemsOrEmptyPlaceholder(toolbarOrder, true))
        // Menu
        fullList.add(headerPair(false))
        fullList.addAll(addItemsOrEmptyPlaceholder(menuOrder, false))
        // Set to default
        fullList.add(CustomizeToolbarFragment.VIEW_TYPE_SET_TO_DEFAULT to "")
    }

    private fun addItemsOrEmptyPlaceholder(list: List<Int>, toolbar: Boolean): List<Pair<Int, Any>> {
        return if (list.isEmpty()) {
            listOf(emptyPlaceholderPair(toolbar))
        } else {
            list.map { CustomizeToolbarFragment.VIEW_TYPE_ITEM to PageActionItem.find(it) }
        }
    }

    private fun headerPair(isToolbar: Boolean): Pair<Int, Any> {
        return CustomizeToolbarFragment.VIEW_TYPE_HEADER to
                if (isToolbar) R.string.customize_toolbar_category_toolbar else R.string.customize_toolbar_category_menu
    }

    private fun emptyPlaceholderPair(isToolbar: Boolean): Pair<Int, Any> {
        return CustomizeToolbarFragment.VIEW_TYPE_EMPTY_PLACEHOLDER to isToolbar
    }

    private fun collectCategoriesItems(): Pair<MutableList<Int>, MutableList<Int>> {
        var saveIntoToolbar = true
        val toolbarItems = mutableListOf<Int>()
        val menuItems = mutableListOf<Int>()
        fullList.filterNot { it.first == CustomizeToolbarFragment.VIEW_TYPE_EMPTY_PLACEHOLDER }.forEach {
            if (it == headerPair(false)) {
                saveIntoToolbar = false
            }
            if (it.first == CustomizeToolbarFragment.VIEW_TYPE_ITEM) {
                if (saveIntoToolbar) {
                    toolbarItems.add((it.second as PageActionItem).id)
                } else {
                    menuItems.add((it.second as PageActionItem).id)
                }
            }
        }
        return toolbarItems to menuItems
    }

    // First: toolbar; Second: Menu
    private fun handleCategoryLimitation(pair: Pair<MutableList<Int>, MutableList<Int>>): List<Int> {
        val list = mutableListOf<Int>()
        // To avoid seeing the bug from the library, we have to use a while loop to manually swap items.
        while (pair.first.size > CustomizeToolbarFragment.TOOLBAR_ITEMS_LIMIT) {
            // Last item swap with "Menu" header
            swapList(pair.first.size, pair.first.size + 1)
            // Add swapped item to list
            list.add(pair.first.size)
            // Add to "Menu" order list and remove the last item of toolbar
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

        toolbarOrder = pair.first
        menuOrder = pair.second
        Prefs.customizeToolbarOrder = toolbarOrder
        Prefs.customizeToolbarMenuOrder = menuOrder

        return rearrangedItems
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(fullList, oldPosition, newPosition)
    }

    fun addEmptyPlaceholder(): Int {
        if (toolbarOrder.isEmpty() && !fullList.contains(emptyPlaceholderPair(true))) {
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
        if (toolbarOrder.isNotEmpty() && fullList.contains(emptyPlaceholderPair(true))) {
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
