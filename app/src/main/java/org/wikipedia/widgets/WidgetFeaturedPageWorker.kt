package org.wikipedia.widgets

import android.content.Context
import android.text.style.URLSpan
import androidx.core.text.getSpans
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

class WidgetFeaturedPageWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = WikipediaApp.instance
            val mainPageTitle = PageTitle(MainPageNameData.valueFor(app.appOrSystemLanguageCode), app.wikiSite)
            val date = DateUtil.getUtcRequestDateFor(0)

            val result = ServiceFactory.getRest(WikipediaApp.instance.wikiSite)
                .getFeedFeatured(date.year, date.month, date.day)

            // TODO: don't use PageSummary.
            val summary = if (result.tfa != null) {
                result.tfa
            } else {
                val response = ServiceFactory.get(mainPageTitle.wikiSite).parseTextForMainPage(mainPageTitle.prefixedText)
                ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getPageSummary(null, findFeaturedArticleTitle(response.text))
            }

            val pageTitle = summary.getPageTitle(app.wikiSite)
            pageTitle.displayText = summary.displayTitle

            WidgetProviderFeaturedPage.forceUpdateWidget(applicationContext, pageTitle)

            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.retry()
        }
    }

    private fun findFeaturedArticleTitle(pageLeadContent: String): String {
        // Extract the actual link to the featured page in a hacky way (until we
        // have the correct API for it):
        // Parse the HTML, and look for the first link, which should be the
        // article of the day.
        val text = StringUtil.fromHtml(pageLeadContent)
        val spans = text.getSpans<URLSpan>()
        var titleText = ""
        for (span in spans) {
            if (!span.url.startsWith("/wiki/") ||
                    text.getSpanEnd(span) - text.getSpanStart(span) <= 1) {
                continue
            }
            val title = PageTitle.titleForInternalLink(UriUtil.decodeURL(span.url), WikiSite(span.url))
            if (!title.isFilePage && !title.isSpecial) {
                titleText = title.displayText
                break
            }
        }
        return titleText
    }
}
