package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.*

class CustomizeFavoritesViewModel : ViewModel() {

    private var quickActionsOrder = mutableListOf<Int>()
    private var menuOrder = mutableListOf<Int>()

    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Pair<Int, Any>>()

    init {
        setupDefaultOrder()
        quickActionsOrder = Prefs.customizeFavoritesQuickActionsOrder.toMutableList()
        menuOrder = Prefs.customizeFavoritesMenuOrder.toMutableList()
        preProcessList()
    }

    private fun setupDefaultOrder() {
        if (Prefs.customizeFavoritesMenuOrder.isEmpty() && Prefs.customizeFavoritesQuickActionsOrder.isEmpty()) {
            Prefs.customizeFavoritesQuickActionsOrder = listOf(0, 1, 2, 3, 4)
            Prefs.customizeFavoritesMenuOrder = listOf(5, 6, 7, 8, 9, 10)
        }
    }

    private fun preProcessList() {
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

    fun saveChanges() {
        var saveIntoQuickActions = true
        quickActionsOrder.clear()
        menuOrder.clear()
        fullList.filterNot { it.first == CustomizeFavoritesFragment.VIEW_TYPE_EMPTY_PLACEHOLDER }.forEach {
            if (it == headerPair(false)) {
                saveIntoQuickActions = false
            }
            if (it.first == CustomizeFavoritesFragment.VIEW_TYPE_ITEM) {
                if (saveIntoQuickActions) {
                    quickActionsOrder.add((it.second as PageMenuItem).id)
                } else {
                    menuOrder.add((it.second as PageMenuItem).id)
                }
            }
        }

    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(fullList, oldPosition, newPosition)
        L.d("fullList " + fullList)
    }

    fun addEmptyPlaceholder(): Int {
        if (quickActionsOrder.isEmpty() && !fullList.contains(emptyPlaceholderPair(true))) {
            fullList.add(1, emptyPlaceholderPair(true))
            L.d("fullList " + fullList)
            return 1
        }
        if (menuOrder.isEmpty() && !fullList.contains(emptyPlaceholderPair(false))) {
            fullList.add(emptyPlaceholderPair(false))
            L.d("fullList " + fullList)
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
