package org.wikipedia.page

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.log.L

object PageAvailableOfflineHandler {
    fun interface Callback {
        fun onFinish(available: Boolean)
    }

    fun check(page: ReadingListPage, callback: Callback) {
        callback.onFinish(WikipediaApp.instance.isOnline || (page.offline && !page.saving))
    }

    fun check(lifeCycleScope: CoroutineScope, pageTitle: PageTitle, callback: Callback) {
        if (WikipediaApp.instance.isOnline) {
            callback.onFinish(true)
            return
        }
        lifeCycleScope.launch(CoroutineExceptionHandler { _, exception ->
            callback.onFinish(false)
            L.w(exception)
        }) {
            val readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(pageTitle)
            callback.onFinish(readingListPage != null && readingListPage.offline && !readingListPage.saving)
        }
    }
}
