package org.wikipedia.readinglist

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_DIR_NAME
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.util.log.L
import java.io.File
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
            val cacheFolder = File(WikipediaApp.getInstance().filesDir, CACHE_DIR_NAME)
            val currentTime = System.currentTimeMillis()
            // TODO: force sync reading list with offline data downloaded
            cacheFolder.listFiles().forEach {
                if (currentTime - it.lastModified() > LAST_MODIFIED_MILLI_LIMIT) {
                    it.delete()
                }
            }
        }
    }

    override fun getName(): String {
        return "reading-list-cache-cleanup"
    }


    companion object {
        private val RUN_INTERVAL_MILLI = TimeUnit.DAYS.toMillis(30)
        private val LAST_MODIFIED_MILLI_LIMIT = TimeUnit.DAYS.toMillis(150)
    }


}
