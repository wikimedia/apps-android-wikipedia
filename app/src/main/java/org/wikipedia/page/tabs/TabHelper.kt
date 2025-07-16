package org.wikipedia.page.tabs

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.log.L

object TabHelper {

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    val list = mutableListOf<Tab>()

    var count: Int = 0

    init {
        updateTabCount()
    }

    fun updateTabCount() {
        MainScope().launch(coroutineExceptionHandler) {
            val tabs = AppDatabase.instance.tabDao().getTabs().filter { it.getBackStackIds().isNotEmpty() }
            count = tabs.size
        }
    }

    fun getCurrentTab(): Tab {
        if (list.isEmpty()) {
            list.add(Tab())
        }
        return list.last()
    }

    suspend fun initTabs() {
        val tab = AppDatabase.instance.tabDao().getTabs()
        if (tab.isNotEmpty()) {
            tab.forEach {
                // Use the backStackIds to get the full backStack items from the database
                val backStackItems = AppDatabase.instance.pageBackStackItemDao().getPageBackStackItems(it.getBackStackIds())
                it.backStack = backStackItems.toMutableList()
            }
            list.addAll(tab)
        }
    }

    fun hasTabs(): Boolean {
        return list.isNotEmpty() && (list.size > 1 || list[0].backStack.isNotEmpty())
    }

    fun removeTabAt(position: Int): Tab {
        return list.removeAt(position)
    }

    fun trimTabCount() {
        while (list.size > Constants.MAX_TABS) {
            list.removeAt(0)
        }
    }

    fun commitTabState() {
        coroutineScope.launch(coroutineExceptionHandler) {
            if (list.isEmpty()) {
                AppDatabase.instance.tabDao().deleteAll()
                AppDatabase.instance.pageBackStackItemDao().deleteAll()
                initTabs()
            } else {
                val existingTabs = AppDatabase.instance.tabDao().getTabs()
                val removedTabsFromList = existingTabs.subtract(list.toSet())
                val newTabsFromList = list.subtract(existingTabs.toSet())

                // First, delete tabs that are no longer in the list
                if (removedTabsFromList.isNotEmpty()) {
                    deleteTabs(removedTabsFromList.toList())
                }

                // Then, insert new tabs that are not in the existing tabs
                if (newTabsFromList.isNotEmpty()) {
                    insertTabs(newTabsFromList.toList())
                }

                // Finally, update the existing tabs with new backStack or order
                val tabsToUpdate = existingTabs - newTabsFromList - removedTabsFromList
                if (tabsToUpdate.isNotEmpty()) {
                    updateTabs(tabsToUpdate.toList())
                }
            }
        }
    }

    fun openInNewBackgroundTab(coroutineScope: CoroutineScope = TabHelper.coroutineScope, entry: HistoryEntry) {
        coroutineScope.launch(coroutineExceptionHandler) {
            val tab = if (count == 0) list[0] else Tab()
            if (count > 0) {
                list.add(0, tab)
                trimTabCount()
            }
            // Add a new PageBackStackItem to database
            val pageBackStackItem = PageBackStackItem(
                apiTitle = entry.title.prefixedText,
                displayTitle = entry.title.displayText,
                langCode = entry.title.wikiSite.languageCode,
                namespace = entry.title.namespace,
                thumbUrl = entry.title.thumbUrl,
                description = entry.title.description,
                extract = entry.title.extract,
                source = entry.source
            )
            tab.backStack.add(pageBackStackItem)
            commitTabState()
        }
    }

    suspend fun insertTabs(tabs: List<Tab>) {
        withContext(Dispatchers.IO) {
            if (tabs.isEmpty()) return@withContext
            // get the last order from the table
            var lastOrder = AppDatabase.instance.tabDao().getTabs().maxOfOrNull { it.order } ?: 0
            tabs.forEach { tab ->
                val ids = AppDatabase.instance.pageBackStackItemDao().insertPageBackStackItems(tab.backStack)
                tab.setBackStackIds(ids)
                tab.order = ++lastOrder
            }
            AppDatabase.instance.tabDao().insertTabs(tabs)
            updateTabCount()
        }
    }

    suspend fun deleteTabs(tabs: List<Tab>) {
        withContext(Dispatchers.IO) {
            if (tabs.isEmpty()) return@withContext
            val backStackIdsToDelete = tabs.flatMap { it.getBackStackIds() }.distinct()
            // Delete all backStack items associated with the tabs to be deleted
            AppDatabase.instance.pageBackStackItemDao().deletePageBackStackItemsById(backStackIdsToDelete)
            AppDatabase.instance.tabDao().deleteTabs(tabs)
            // Reset the order of the remaining tabs
            val remainingTabs = AppDatabase.instance.tabDao().getTabs()
            remainingTabs.forEachIndexed { index, tab ->
                tab.order = index + 1
            }
            AppDatabase.instance.tabDao().updateTabs(remainingTabs)
            updateTabCount()
        }
    }

     suspend fun updateTabs(tabs: List<Tab>) {
        withContext(Dispatchers.IO) {
            if (tabs.isEmpty()) return@withContext
            tabs.forEachIndexed { index, tab ->
                // Find the existing tab in the database
                val existingTab = AppDatabase.instance.tabDao().getTabById(tab.id)
                if (existingTab != null) {
                    // First, find removed backstack items
                    val removedBackStacks = existingTab.backStack.subtract(tab.backStack).filter { it.id != -1L }
                    if (removedBackStacks.isNotEmpty()) {
                        // Delete removed backstack items from the database
                        AppDatabase.instance.pageBackStackItemDao()
                            .deletePageBackStackItemsById(removedBackStacks.map { it.id }.toList())
                    }

                    // Second, find new backstack items if the id is -1, insert them to the database
                    val backStackIds = mutableListOf<Long>()
                    tab.backStack.forEach { item ->
                        var backStackId = item.id
                        if (item.id == -1L) {
                            val newId = AppDatabase.instance.pageBackStackItemDao()
                                .insertPageBackStackItem(item)
                            backStackId = newId
                        }
                        backStackIds.add(backStackId)
                    }

                    // Finally, update the tab with the new backStackIds and order
                    tab.order = index
                    tab.setBackStackIds(backStackIds)
                    AppDatabase.instance.tabDao().updateTab(tab)
                } else {
                    insertTabs(listOf(tab))
                }
            }
            updateTabCount()
        }
    }
}
