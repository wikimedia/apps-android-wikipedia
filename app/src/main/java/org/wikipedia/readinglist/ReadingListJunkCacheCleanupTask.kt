package org.wikipedia.readinglist

import kotlinx.coroutines.*
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mainly to clean up junk offline data when saving/removing articles from reading lists.
 */
class ReadingListJunkCacheCleanupTask : RecurringTask() {

    override fun shouldRun(lastRun: Date): Boolean {
        return System.currentTimeMillis() - lastRun.time >= RUN_INTERVAL_MILLI
    }

    override fun run(lastRun: Date) {
        CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { _, exception -> L.w(exception) }) {
            val pages = withContext(Dispatchers.IO) { ReadingListDbHelper.instance().allPages }
            SavedPageSyncService().cleanUpJunkFiles(pages)
        }
    }

    override fun getName(): String {
        return "reading-list-cache-cleanup"
    }

    companion object {
        private val RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(30)
    }


}
