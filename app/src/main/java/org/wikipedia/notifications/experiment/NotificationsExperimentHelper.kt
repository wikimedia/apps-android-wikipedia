package org.wikipedia.notifications.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.TopPageViews
import org.wikipedia.page.Namespace
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil

object NotificationsExperimentHelper {
    private const val DAYS_IN_WEEK = 7
    private const val MAIN_PAGE = "Main_Page"

    val wikiSite = WikiSite.forLanguageCode("en")

    suspend fun getWeeklyTopRead(): List<TopPageViews.Article> = coroutineScope {
        val nonArticleNamespacesDeferred = async {
            ServiceFactory.get(wikiSite).getPageNamespaceWithSiteInfo(null).query?.namespaces.orEmpty().values
                .filter { it.id != Namespace.MAIN.code() }
                .map { StringUtil.addUnderscores(it.name) }
                .toSet()
        }
        val dailyArticlesDeferred = (1..DAYS_IN_WEEK).map { age ->
            async {
                val date = DateUtil.getUtcRequestDateFor(age)
                runCatching {
                    ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                        .getTopPageViews(wikiSite.authority(), date.year, date.month, date.day)
                        .items.flatMap { it.articles }
                }.getOrDefault(emptyList())
            }
        }
        val nonArticleNamespaces = nonArticleNamespacesDeferred.await()

        dailyArticlesDeferred.awaitAll()
            .asSequence()
            .flatten()
            .filter { it.article != MAIN_PAGE && it.article.substringBefore(':', "") !in nonArticleNamespaces }
            .groupBy { it.article }
            .map { (article, dailyEntries) -> article to dailyEntries.sumOf { it.views } }
            .sortedByDescending { it.second }
            .mapIndexed { index, (article, views) -> TopPageViews.Article(article, views, index + 1) }
            .toList()
    }
}
