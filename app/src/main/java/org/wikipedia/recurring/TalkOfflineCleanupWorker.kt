package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.log.L
import java.io.File
import java.util.concurrent.TimeUnit

class TalkOfflineCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            AppDatabase.instance.offlineObjectDao().searchForOfflineObjects(CLEANUP_URL_SEARCH_KEY)
                .filter {
                    (System.currentTimeMillis() - File(it.path + ".0").lastModified()) >
                            TimeUnit.DAYS.toMillis(CLEANUP_MAX_AGE_DAYS)
                }.forEach {
                    AppDatabase.instance.offlineObjectDao().deleteOfflineObject(it)
                    AppDatabase.instance.offlineObjectDao().deleteFilesForObject(it)
                }
            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.failure()
        }
    }

    companion object {
        private const val CLEANUP_URL_SEARCH_KEY = "action=discussiontoolspageinfo"
        private const val CLEANUP_MAX_AGE_DAYS = 7L
    }
}
