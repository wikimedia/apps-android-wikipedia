package org.wikipedia.recurring

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.alphaupdater.AlphaUpdateChecker
import org.wikipedia.settings.RemoteConfigRefreshTask
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L

class RecurringTasksExecutor(private val lifecycleScope: LifecycleCoroutineScope) {
    fun run() {
        val app = WikipediaApp.instance
        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val allTasks = arrayOf( // Has list of all rotating tasks that need to be run
                RemoteConfigRefreshTask(),
                DailyEventTask(app),
                TalkOfflineCleanupTask(app)
            )
            for (task in allTasks) {
                task.runIfNecessary()
            }
            if (ReleaseUtil.isAlphaRelease) {
                AlphaUpdateChecker(app).runIfNecessary()
            }
        }
    }
}
