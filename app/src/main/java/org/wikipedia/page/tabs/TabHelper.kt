package org.wikipedia.page.tabs

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

object TabHelper {

    const val MAX_TABS = 100

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    var count: Int = 0

    init {
        coroutineScope.launch {
            updateTabCount()
        }
    }

    suspend fun updateTabCount() {
        val tabs = AppDatabase.instance.tabDao().getTabs().filter { it.getBackStackIds().isNotEmpty() }
        count = tabs.size
    }

    fun getCurrentTab(): Tab {
        // TODO: handle this with coroutines if we have viewModel
        return runBlocking {
            val foregroundTab = AppDatabase.instance.tabDao().getForegroundTab()
            if (foregroundTab == null) {
                return@runBlocking Tab()
            }
            // Use the backStackIds to get the full backStack items from the database
            val backStackItems = AppDatabase.instance.pageBackStackItemDao()
                .getPageBackStackItems(foregroundTab.getBackStackIds())
            foregroundTab.backStack = backStackItems.toMutableList()
            foregroundTab
        }
    }

    fun removeTab(tab: Tab) {
        coroutineScope.launch(coroutineExceptionHandler) {
            deleteTabs(listOf(tab))
        }
    }

    fun findTabByTitle(title: PageTitle): Tab? {
        // TODO: handle this with coroutines if we have viewModel
        return runBlocking {
            val tabs = AppDatabase.instance.tabDao().getTabs()
            tabs.firstOrNull { tab ->
                val backStackItems = AppDatabase.instance.pageBackStackItemDao()
                    .getPageBackStackItems(tab.getBackStackIds())
                backStackItems.any { it.getPageTitle() == title }
            }
        }
    }

    suspend fun trimTabCount() {
        withContext(Dispatchers.IO) {
            val tabs = AppDatabase.instance.tabDao().getTabs().filter { it.getBackStackIds().isNotEmpty() }
            if (tabs.size > MAX_TABS) {
                // Sort tabs by order and remove the oldest ones
                val sortedTabs = tabs.sortedBy { it.order }
                val tabsToDelete = sortedTabs.take(sortedTabs.size - MAX_TABS)
                deleteTabs(tabsToDelete)
            }
        }
    }

    fun openInNewBackgroundTab(coroutineScope: CoroutineScope = TabHelper.coroutineScope, entry: HistoryEntry, action: () -> Unit = {}) {
        coroutineScope.launch(coroutineExceptionHandler) {
            val tab = Tab()
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
            insertTabs(listOf(tab))
            action()
        }
    }

    suspend fun insertTabs(tabs: List<Tab>, toForeground: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (tabs.isEmpty()) return@withContext
            val allTabs = AppDatabase.instance.tabDao().getTabs()
            // get the last order from the table
            var lastOrder = if (toForeground) 0 else allTabs.maxOfOrNull { it.order } ?: 0
            tabs.forEach { tab ->
                val ids = AppDatabase.instance.pageBackStackItemDao().insertPageBackStackItems(tab.backStack)
                tab.setBackStackIds(ids)
                if (tab.order == 0) {
                    // If the order is not set, assign a new order
                    tab.order = ++lastOrder
                }
            }
            var finalTabs = tabs
            if (toForeground) {
                // Re-arrange the existing tabs' order
                allTabs.forEachIndexed { index, existingTab ->
                    existingTab.order = ++lastOrder
                }
                finalTabs = finalTabs + allTabs
            }

            AppDatabase.instance.tabDao().insertTabs(finalTabs)
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

     suspend fun updateTab(tab: Tab) {
        withContext(Dispatchers.IO) {
            // Find the existing tab in the database
            val existingTab = AppDatabase.instance.tabDao().getTabById(tab.id)
            if (existingTab != null) {
                // First, find removed backstack items
                val removedBackStacks =
                    existingTab.backStack.subtract(tab.backStack).filter { it.id != -1L }
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
                tab.setBackStackIds(backStackIds)
                AppDatabase.instance.tabDao().updateTab(tab)
                updateTabCount()
            }
        }
    }

    suspend fun moveTabToForeground(tab: Tab): List<Tab> {
        return withContext(Dispatchers.IO) {
            val list = AppDatabase.instance.tabDao().getTabs().toMutableList()
            // Change the tab's order to 1 and update the rest of the tabs
            tab.order = 1
            list.remove(tab)
            list.add(0, tab)
            list.forEachIndexed { index, t ->
                t.order = index + 1
            }
            AppDatabase.instance.tabDao().updateTabs(list)
            updateTabCount()
            return@withContext list
        }
    }
}
