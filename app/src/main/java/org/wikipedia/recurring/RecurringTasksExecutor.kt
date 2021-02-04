package org.wikipedia.recurring

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.alphaupdater.AlphaUpdateChecker
import org.wikipedia.settings.RemoteConfigRefreshTask
import org.wikipedia.util.ReleaseUtil

class RecurringTasksExecutor(private val app: WikipediaApp) {
    fun run() {
        Completable.fromAction {
            val allTasks = arrayOf( // Has list of all rotating tasks that need to be run
                    RemoteConfigRefreshTask(),
                    DailyEventTask(app)
            )
            for (task in allTasks) {
                task.runIfNecessary()
            }
            if (ReleaseUtil.isAlphaRelease) {
                AlphaUpdateChecker(app).runIfNecessary()
            }
        }.subscribeOn(Schedulers.io()).subscribe()
    }
}
