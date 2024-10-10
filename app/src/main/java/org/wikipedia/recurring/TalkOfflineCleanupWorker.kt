package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class TalkOfflineCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            AppDatabase.instance.offlineObjectDao()
                .searchForOfflineObjects(CLEANUP_URL_SEARCH_KEY)
                .filter {
                    val lastModified = Instant.ofEpochMilli(File("${it.path}.0").lastModified())
                    ChronoUnit.DAYS.between(lastModified, Instant.now()) > CLEANUP_MAX_AGE_DAYS
                }.forEach {
                    AppDatabase.instance.offlineObjectDao().deleteOfflineObject(it)
                    AppDatabase.instance.offlineObjectDao().deleteFilesForObject(it)
                }
        }
        return Result.success()
    }

    companion object {
        private const val CLEANUP_URL_SEARCH_KEY = "action=discussiontoolspageinfo"
        private const val CLEANUP_MAX_AGE_DAYS = 7L
    }
}
