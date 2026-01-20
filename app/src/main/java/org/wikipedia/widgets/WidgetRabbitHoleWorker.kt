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

            // For prototype purposes, use hardcoded articles "Dog" and "Cat"
            val startArticle = "Dog"
            val endArticle = "Cat"

            val wikiSite = WikiSite.forLanguageCode(app.appOrSystemLanguageCode)

            // Fetch page summaries for both articles
            val startSummary = ServiceFactory.getRest(wikiSite).getPageSummary(startArticle)
            val endSummary = ServiceFactory.getRest(wikiSite).getPageSummary(endArticle)

            val startPageTitle = startSummary.getPageTitle(wikiSite)
            startPageTitle.displayText = startSummary.displayTitle

            val endPageTitle = endSummary.getPageTitle(wikiSite)
            endPageTitle.displayText = endSummary.displayTitle

            WidgetProviderRabbitHole.forceUpdateWidget(applicationContext, startPageTitle, endPageTitle)

            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.retry()
        }
    }
}
