package org.wikipedia.page

import android.annotation.SuppressLint
import kotlinx.coroutines.*
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.log.L

object PageAvailableOfflineHandler {

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(Dispatchers.Main)

    fun interface Callback {
        fun onFinish(available: Boolean)
    }

    fun check(page: ReadingListPage, callback: Callback) {
        callback.onFinish(WikipediaApp.getInstance().isOnline || (page.offline && !page.saving))
    }

    @SuppressLint("CheckResult")
    fun check(pageTitle: PageTitle, callback: Callback) {
        if (WikipediaApp.getInstance().isOnline) {
            callback.onFinish(true)
            return
        }
        scope.launch(CoroutineExceptionHandler { _, exception ->
            run {
                callback.onFinish(false)
                L.w(exception)
            }
        }) {
            val readingListPage = withContext(dispatcher) { AppDatabase.getAppDatabase().readingListPageDao().findPageInAnyList(pageTitle) }
            callback.onFinish(readingListPage != null && readingListPage.offline && !readingListPage.saving)
        }
    }
}
