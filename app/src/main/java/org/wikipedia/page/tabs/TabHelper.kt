package org.wikipedia.page.tabs

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.HistoryEntry
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

object TabHelper {

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }
    val list = mutableListOf<Tab>()

    val count
        get() = if (list.size > 1) list.size else if (list.isEmpty()) 0 else if (list[0].backStack.isEmpty()) 0 else list.size

    init {
        coroutineScope.launch (coroutineExceptionHandler) {
            initTabs()
        }
    }

    // TODO: remove on 2026-07-01
    private suspend fun migrateTabsToDatabase() {
        withContext(Dispatchers.IO) {
            if (Prefs.tabs.isEmpty() || AppDatabase.instance.tabDao().hasTabs()) {
                return@withContext
            }
            AppDatabase.instance.tabDao().insertTabs(Prefs.tabs)

            // TODO: enable this on 2026-07-01
            // Prefs.clearTabs()
        }
    }

    private suspend fun initTabs() {
        migrateTabsToDatabase()
        if (AppDatabase.instance.tabDao().hasTabs()) {
            val tab = AppDatabase.instance.tabDao().getTabs()
            tab.forEach {
                // Use the backStackIds to get the full backStack items from the database
                val backStackItems = AppDatabase.instance.pageBackStackItemDao().getPageBackStackItems(it.backStackIds)
                it.backStack = backStackItems.toMutableList()
            }
            list.addAll(tab)
        }
        if (list.isEmpty()) {
            list.add(Tab())
        }
    }

    fun hasTabs(): Boolean {
        return list.isNotEmpty() && (list.size > 1 || list[0].backStack.isNotEmpty())
    }

    fun commitTabState(tab: Tab? = null) {
        coroutineScope.launch (coroutineExceptionHandler) {
            if (tab == null) {
                // Regular tab commit
                AppDatabase.instance.tabDao().deleteAll()
                if (list.isEmpty()) {
                    initTabs()
                } else {
                    AppDatabase.instance.tabDao().insertTabs(list)
                }
            } else {
                // Update the specific tab
                AppDatabase.instance.tabDao().updateTab(tab)
            }
        }
    }

    fun openInNewBackgroundTab(entry: HistoryEntry) {
        coroutineScope.launch (coroutineExceptionHandler) {
            val tab = if (count == 0) list[0] else Tab()
            if (count > 0) {
                list.add(0, tab)
                while (list.size > Constants.MAX_TABS) {
                    AppDatabase.instance.tabDao().deleteTab(list.removeAt(0))
                }
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

            // TODO: should move into a separate function?
            tab.backStackIds.clear()
            tab.backStack.forEach {
                val id = AppDatabase.instance.pageBackStackItemDao().insertPageBackStackItem(it)
                tab.backStackIds.add(id)
            }
            AppDatabase.instance.tabDao().insertTab(tab)
        }
    }
}