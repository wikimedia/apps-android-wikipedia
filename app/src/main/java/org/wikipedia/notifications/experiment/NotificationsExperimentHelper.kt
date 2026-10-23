package org.wikipedia.notifications.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.TopPageViews
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil

object NotificationsExperimentHelper {
    private const val DAYS_IN_WEEK = 7
    private const val MAX_DAYS_TO_CHECK = 14

    suspend fun getWeeklyTopRead(languageCode: String): PageTitle? = coroutineScope {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val mainPageTitle = StringUtil.addUnderscores(MainPageNameData.valueFor(languageCode))
        val nonArticleNamespacesDeferred = async {
            ServiceFactory.get(wikiSite).getPageNamespaceWithSiteInfo(null).query?.namespaces.orEmpty().values
                .filter { it.id != Namespace.MAIN.code() }
                .map { StringUtil.addUnderscores(it.name) }
                .toSet()
        }
        val weeklyArticles = (1..MAX_DAYS_TO_CHECK).asFlow()
            .map { age -> getDailyTopArticles(wikiSite, age) }
            .filter { it.isNotEmpty() }
            .take(DAYS_IN_WEEK)
            .toList()
            .flatten()
        val nonArticleNamespaces = nonArticleNamespacesDeferred.await()

        weeklyArticles
            .groupingBy { it.article }
            .fold(0L) { totalViews, entry -> totalViews + entry.views }
            .filterKeys { it != mainPageTitle && it.substringBefore(':', "") !in nonArticleNamespaces }
            .maxByOrNull { it.value }
            ?.let { PageTitle(it.key, wikiSite) }
    }

    private suspend fun getDailyTopArticles(wikiSite: WikiSite, age: Int): List<TopPageViews.Article> {
        val date = DateUtil.getUtcRequestDateFor(age)
        return runCatching {
            ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getTopPageViews(wikiSite.authority(), date.year, date.month, date.day)
                .items.flatMap { it.articles }
        }.getOrDefault(emptyList())
    }
}
