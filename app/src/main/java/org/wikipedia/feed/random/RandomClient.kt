package org.wikipedia.feed.random

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import kotlin.math.max

class RandomClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        clientJob = coroutineScope.launch(
            CoroutineExceptionHandler { _, caught ->
                L.v(caught)
                cb.error(caught)
            }
        ) {
            val deferredSummaries = WikipediaApp.instance.languageState.appLanguageCodes.take(5)
                .filter { !FeedContentType.RANDOM.langCodesDisabled.contains(it) }
                .map { lang ->
                    async {
                        val wikiSite = WikiSite.forLanguageCode(lang)
                        val randomSummary = try {
                            ServiceFactory.getRest(wikiSite).getRandomSummary()
                        } catch (e: Exception) {
                            AppDatabase.instance.readingListPageDao().getRandomPage(lang)?.let {
                                ReadingListPage.toPageSummary(it)
                            }
                        }
                        randomSummary?.let {
                            RandomCard(it, age, wikiSite)
                        }
                    }
                }

            cb.success(deferredSummaries.awaitAll().filterNotNull())
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }

    companion object {
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
}
