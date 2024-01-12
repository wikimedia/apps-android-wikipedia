package org.wikipedia.widgets

import android.content.Context
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

            WidgetProviderFeaturedPage.forceUpdateWidget(applicationContext, pageTitle, false)

            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.retry()
        }
    }

    private fun findFeaturedArticleTitle(pageContent: String): String {
        // In lieu of a proper API for getting the featured article, we'll just parse it out of the
        // main page HTML. The idea is to find the first subheading (h2), then search for the first
        // anchor (link) after that, which satisfies a few conditions.

        var parsePos = pageContent.indexOf("</h2>")
        if (parsePos == -1) {
            parsePos = 0
        }

        // Yes, I know you're not supposed to parse HTML with regexes. But this is a very specific
        // isolated situation and it's a lot easier and more efficient than a full-blown HTML parser.
        // Is that ok with you? Good.
        var match = """<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)(["'])\s*>(.*?)<\/a>""".toRegex()
            .find(pageContent, parsePos)

        while (match != null) {
            val href = match.groupValues[2].trim()
            val text = StringUtil.fromHtml(match.groupValues[4]).toString().trim()
            val title = PageTitle.titleForInternalLink(UriUtil.decodeURL(href), WikiSite(href))
            if (!title.isFilePage && !title.isSpecial && href.startsWith("/wiki/") && text.isNotEmpty()) {
                return title.prefixedText
            }
            match = match.next()
        }
        return ""
    }
}
