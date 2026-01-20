package org.wikipedia.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

class WidgetRabbitHoleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = WikipediaApp.instance

            // For prototype purposes, use hardcoded list of articles representing the rabbit hole path
            val articleTitles = listOf("Moth", "Diurnal animal", "Sleeping", "Memory", "Telephone number", "Telephone call", "Ringtone", "Landline")

                //"Dog", "Species", "Eukaryote", "Greek language", "New Testament")

            val wikiSite = WikiSite.forLanguageCode(app.appOrSystemLanguageCode)

            // Fetch page summaries for the first and last articles in the list
            val startSummary = ServiceFactory.getRest(wikiSite).getPageSummary(articleTitles.first())
            val endSummary = ServiceFactory.getRest(wikiSite).getPageSummary(articleTitles.last())

            val startPageTitle = startSummary.getPageTitle(wikiSite)
            startPageTitle.displayText = startSummary.displayTitle

            val endPageTitle = endSummary.getPageTitle(wikiSite)
            endPageTitle.displayText = endSummary.displayTitle

            WidgetProviderRabbitHole.forceUpdateWidget(applicationContext, startPageTitle, endPageTitle, articleTitles)

            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.retry()
        }
    }

    companion object {
        val redirectedTitles = listOf("Moth", "Diurnality", "Sleep", "Memory", "Telephone number", "Telephone call", "Ringtone", "Landline")
    }
}
