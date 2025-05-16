package org.wikipedia.recurring

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.games.onthisday.OnThisDayGameABCTest
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import java.util.Date
import java.util.concurrent.TimeUnit

class DailyEventTask(private val app: WikipediaApp) : RecurringTask() {
    override val name = app.getString(R.string.preference_key_daily_event_time_task_name)

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) > TimeUnit.DAYS.toMillis(1)
    }

    override suspend fun run(lastRun: Date) {
        DailyStatsEvent.log(app)
        EventPlatformClient.refreshStreamConfigs()

        if (OnThisDayGameViewModel.isLangABTested(app.appOrSystemLanguageCode)) {
            WikiGamesEvent.submit("group_assign", OnThisDayGameABCTest().getGroupName())
        }
    }
}
