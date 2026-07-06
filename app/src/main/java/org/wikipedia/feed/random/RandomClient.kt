package org.wikipedia.feed.random

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import kotlin.math.max

object RandomClient {
    suspend fun getRandomPages(wikiSite: WikiSite, count: Int): List<PageTitle> {
        return ServiceFactory.get(wikiSite).getRandomPagesWithExtract(max(count, 10) * 4).query?.pages.orEmpty().asSequence().filter {
            it.pageProps?.disambiguation == null
        }.filter {
            wikiSite.languageCode != "en" || (!it.description.orEmpty().contains("list article", ignoreCase = true) && !it.description.orEmpty().contains("list of", ignoreCase = true))
        }.map { page ->
            PageTitle(
                text = page.title,
                wiki = wikiSite,
                thumbUrl = page.thumbUrl(),
                description = page.description,
                displayText = page.displayTitle(wikiSite.languageCode),
            ).also {
                if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                it.extract = page.extract
            }
        }.sortedBy { it.thumbUrl == null }.take(count).toList()
    }
}
