package org.wikipedia.suggestededits.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsRecentEditsViewModel
import java.time.Instant
import java.util.concurrent.Semaphore
import kotlin.math.abs

object EditingSuggestionsProvider {
    private val mutex: Semaphore = Semaphore(1)

    private val articlesWithMissingDescriptionCache = ArrayDeque<String>()
    private var articlesWithMissingDescriptionCacheLang: String = ""
    private val articlesWithTranslatableDescriptionCache = ArrayDeque<Pair<PageTitle, PageTitle>>()
    private var articlesWithTranslatableDescriptionCacheFromLang: String = ""
    private var articlesWithTranslatableDescriptionCacheToLang: String = ""

    private val imagesWithMissingCaptionsCache = ArrayDeque<String>()
    private var imagesWithMissingCaptionsCacheLang: String = ""
    private val imagesWithTranslatableCaptionCache = ArrayDeque<Pair<String, String>>()
    private var imagesWithTranslatableCaptionCacheFromLang: String = ""
    private var imagesWithTranslatableCaptionCacheToLang: String = ""

    private val imagesWithMissingTagsCache = ArrayDeque<MwQueryPage>()

    private val articlesWithImageRecommendationsCache = ArrayDeque<MwQueryPage>()
    private var articlesWithImageRecommendationsCacheLang: String = ""
    private var articlesWithImageRecommendationsLastMillis: Long = 0

    private var revertCandidateLang: String = ""
    private val revertCandidateCache: ArrayDeque<MwQueryResult.RecentChange> = ArrayDeque()
    private var revertCandidateLastRevId = 0L
    private var revertCandidateLastTimeStamp = Instant.now()

    private const val MAX_RETRY_LIMIT: Long = 50

    suspend fun getNextArticleWithMissingDescription(wiki: WikiSite, retryLimit: Long = MAX_RETRY_LIMIT): PageSummary {
        var pageSummary: PageSummary
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                var title = ""
                if (articlesWithMissingDescriptionCacheLang != wiki.languageCode) {
                    // evict the cache if the language has changed.
                    articlesWithMissingDescriptionCache.clear()
                }
                if (!articlesWithMissingDescriptionCache.isEmpty()) {
                    title = articlesWithMissingDescriptionCache.removeFirst()
                }

                var tries = 0
                do {
                    // Fetch a batch of random articles, and check if any of them have missing descriptions.
                    val resultsWithNoDescription = ServiceFactory.get(wiki).getRandomPages().query?.pages?.filter {
                        it.description.isNullOrEmpty()
                    }.orEmpty()

                    articlesWithMissingDescriptionCacheLang = wiki.languageCode



                    if (DescriptionEditUtil.wikiUsesLocalDescriptions(wiki.languageCode)) {
                        resultsWithNoDescription.forEach {
                            articlesWithMissingDescriptionCache.addFirst(it.title)
                        }
                    } else {
                        // If the wiki uses Wikidata descriptions, check protection status of the Wikidata items.
                        val qNums = resultsWithNoDescription.mapNotNull { it.pageProps?.wikiBaseItem }

                        val wdResponse = ServiceFactory.get(Constants.wikidataWikiSite).getProtection(qNums.joinToString("|") ?: "")


                            .getEntitiesByPageTitles(resultsWithNoDescription?.joinToString("|") { it.title })


                        val randomItems = ServiceFactory.getRest(Constants.wikidataWikiSite)
                            .getEntitiesByPageTitles(resultsWithNoDescription?.joinToString("|") { it.title })
                    }



                    if (!articlesWithMissingDescriptionCache.isEmpty()) {
                        title = articlesWithMissingDescriptionCache.removeFirst()
                    }
                } while (tries++ < retryLimit && title.isEmpty())

                pageSummary = ServiceFactory.getRest(wiki).getPageSummary(null, title)
            } finally {
                mutex.release()
            }
        }
        return pageSummary
    }

    suspend fun getNextArticleWithMissingDescription(sourceWiki: WikiSite, targetLang: String, sourceLangMustExist: Boolean,
                                                     retryLimit: Long = MAX_RETRY_LIMIT): Pair<PageSummary, PageSummary> {
        var pair = Pair(PageSummary(), PageSummary())
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                val targetWiki = WikiSite.forLanguageCode(targetLang)
                var titles: Pair<PageTitle, PageTitle>? = null
                if (articlesWithTranslatableDescriptionCacheFromLang != sourceWiki.languageCode ||
                    articlesWithTranslatableDescriptionCacheToLang != targetLang) {
                    // evict the cache if the language has changed.
                    articlesWithTranslatableDescriptionCache.clear()
                }
                if (!articlesWithTranslatableDescriptionCache.isEmpty()) {
                    titles = articlesWithTranslatableDescriptionCache.removeFirst()
                }
                var tries = 0
                do {
                    val listOfSuggestedEditItem = ServiceFactory.getRest(Constants.wikidataWikiSite)
                        .getArticlesWithTranslatableDescriptions(WikiSite.normalizeLanguageCode(sourceWiki.languageCode),
                            WikiSite.normalizeLanguageCode(targetLang))
                    val mwQueryPages = ServiceFactory.get(targetWiki)
                        .getDescription(listOfSuggestedEditItem.joinToString("|") { it.title() }).query?.pages

                    articlesWithTranslatableDescriptionCacheFromLang = sourceWiki.languageCode
                    articlesWithTranslatableDescriptionCacheToLang = targetLang

                    listOfSuggestedEditItem.forEach { item ->
                        val page = mwQueryPages?.find { it.title == item.title() }
                        if (page != null && !page.description.isNullOrEmpty()) {
                            return@forEach
                        }
                        val entity = item.entity
                        if (entity == null || entity.descriptions.containsKey(targetLang) ||
                            sourceLangMustExist && !entity.descriptions.containsKey(sourceWiki.languageCode) ||
                            !entity.sitelinks.containsKey(sourceWiki.dbName()) ||
                            !entity.sitelinks.containsKey(targetWiki.dbName())
                        ) {
                            return@forEach
                        }
                        val sourceTitle = PageTitle(entity.sitelinks[sourceWiki.dbName()]!!.title, sourceWiki).apply {
                            description = entity.descriptions[sourceWiki.languageCode]?.value
                        }
                        val targetTitle = PageTitle(entity.sitelinks[targetWiki.dbName()]!!.title, targetWiki)
                        articlesWithTranslatableDescriptionCache.addFirst(sourceTitle to targetTitle)
                    }

                    if (!articlesWithTranslatableDescriptionCache.isEmpty()) {
                        titles = articlesWithTranslatableDescriptionCache.removeFirst()
                    }
                } while (tries++ < retryLimit && titles == null)

                titles?.let {
                    val sourcePageSummary = async {
                        ServiceFactory.getRest(it.first.wikiSite).getPageSummary(null, it.first.prefixedText).apply {
                            if (description.isNullOrEmpty()) {
                                description = it.first.description
                            }
                        }
                    }
                    val targetPageSummary = async {
                        ServiceFactory.getRest(it.second.wikiSite).getPageSummary(null, it.second.prefixedText)
                    }
                    pair = sourcePageSummary.await() to targetPageSummary.await()
                }
            } finally {
                mutex.release()
            }
        }
        return pair
    }

    suspend fun getNextImageWithMissingCaption(lang: String, retryLimit: Long = MAX_RETRY_LIMIT): String {
        var title = ""
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                if (imagesWithMissingCaptionsCacheLang != lang) {
                    // evict the cache if the language has changed.
                    imagesWithMissingCaptionsCache.clear()
                }
                if (!imagesWithMissingCaptionsCache.isEmpty()) {
                    title = imagesWithMissingCaptionsCache.removeFirst()
                }
                imagesWithMissingCaptionsCacheLang = lang
                var tries = 0
                do {
                    val listOfSuggestedEditItem = ServiceFactory.getRest(Constants.commonsWikiSite)
                        .getImagesWithoutCaptions(WikiSite.normalizeLanguageCode(lang))
                    listOfSuggestedEditItem.forEach {
                        imagesWithMissingCaptionsCache.addFirst(it.title())
                    }
                    if (!imagesWithMissingCaptionsCache.isEmpty()) {
                        title = imagesWithMissingCaptionsCache.removeFirst()
                    }
                } while (tries++ < retryLimit && title.isEmpty())
            } finally {
                mutex.release()
            }
        }
        return title
    }

    suspend fun getNextImageWithMissingCaption(sourceLang: String, targetLang: String,
                                               retryLimit: Long = MAX_RETRY_LIMIT): Pair<String, String> {
        var pair = Pair("", "")
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                if (imagesWithTranslatableCaptionCacheFromLang != sourceLang ||
                    imagesWithTranslatableCaptionCacheToLang != targetLang
                ) {
                    // evict the cache if the language has changed.
                    imagesWithTranslatableCaptionCache.clear()
                }
                if (!imagesWithTranslatableCaptionCache.isEmpty()) {
                    pair = imagesWithTranslatableCaptionCache.removeFirst()
                }
                imagesWithTranslatableCaptionCacheFromLang = sourceLang
                imagesWithTranslatableCaptionCacheToLang = targetLang
                var tries = 0
                do {
                    val listOfSuggestedEditItem = ServiceFactory.getRest(Constants.commonsWikiSite).getImagesWithTranslatableCaptions(
                        WikiSite.normalizeLanguageCode(sourceLang),
                        WikiSite.normalizeLanguageCode(targetLang)
                    )
                    listOfSuggestedEditItem.forEach {
                        if (!it.captions.containsKey(sourceLang) || it.captions.containsKey(targetLang)) {
                            return@forEach
                        }
                        imagesWithTranslatableCaptionCache.addFirst((it.captions[sourceLang] ?: error("")) to it.title())
                    }
                    if (!imagesWithTranslatableCaptionCache.isEmpty()) {
                        pair = imagesWithTranslatableCaptionCache.removeFirst()
                    }
                } while (tries++ < retryLimit && (pair.first.isEmpty() || pair.second.isEmpty()))
            } finally {
                mutex.release()
            }
        }
        return pair
    }

    suspend fun getNextImageWithMissingTags(retryLimit: Long = MAX_RETRY_LIMIT): MwQueryPage {
        var page: MwQueryPage
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                if (!imagesWithMissingTagsCache.isEmpty()) {
                    page = imagesWithMissingTagsCache.removeFirst()
                }
                var tries = 0
                do {
                    val response = ServiceFactory.get(Constants.commonsWikiSite).getRandomImages()
                    response.query?.pages?.filter { it.imageInfo()?.mime == "image/jpeg" }?.forEach { page ->
                        if (page.revisions.none { "P180" in it.getContentFromSlot("mediainfo") }) {
                            imagesWithMissingTagsCache.addFirst(page)
                        }
                    }
                } while (tries++ < retryLimit && imagesWithMissingTagsCache.isEmpty())
                page = imagesWithMissingTagsCache.removeFirst()
            } finally {
                mutex.release()
            }
        }
        return page
    }

    suspend fun getNextArticleWithImageRecommendation(lang: String, retryLimit: Long = MAX_RETRY_LIMIT): MwQueryPage {
        var page: MwQueryPage
        withContext(Dispatchers.IO) {
            mutex.acquire()
            try {
                if (articlesWithImageRecommendationsCacheLang != lang) {
                    // evict the cache if the language has changed.
                    articlesWithImageRecommendationsCache.clear()
                }
                articlesWithImageRecommendationsCacheLang = lang

                // Wait at least 5 seconds before serving up the next recommendation.
                while (abs(System.currentTimeMillis() - articlesWithImageRecommendationsLastMillis) < 5000) {
                    Thread.sleep(1000)
                }
                articlesWithImageRecommendationsLastMillis = System.currentTimeMillis()

                var tries = 0
                do {
                    if (articlesWithImageRecommendationsCache.isEmpty()) {
                        val response = ServiceFactory.get(WikiSite.forLanguageCode(articlesWithImageRecommendationsCacheLang))
                            .getPagesWithImageRecommendations(10)
                        // TODO: make use of continuation parameter?
                        response.query?.pages?.forEach { page ->
                            if (page.thumbUrl().isNullOrEmpty() && page.growthimagesuggestiondata?.get(0)?.images?.get(0) != null) {
                                articlesWithImageRecommendationsCache.addFirst(page)
                            }
                        }
                    }
                } while (tries++ < retryLimit && articlesWithImageRecommendationsCache.isEmpty())

                page = articlesWithImageRecommendationsCache.removeFirst()
            } finally {
                mutex.release()
            }
        }
        return page
    }

    fun populateRevertCandidateCache(lang: String, recentChanges: List<MwQueryResult.RecentChange>) {
        revertCandidateLang = lang
        revertCandidateCache.clear()
        revertCandidateLastRevId = 0L
        recentChanges.forEach {
            revertCandidateCache.addFirst(it)
            if (it.curRev > revertCandidateLastRevId) {
                revertCandidateLastRevId = it.curRev
                revertCandidateLastTimeStamp = it.parsedInstant
            }
        }
    }

    @Suppress("KotlinConstantConditions")
    suspend fun getNextRevertCandidate(lang: String): MwQueryResult.RecentChange {
        return withContext(Dispatchers.IO) {
            try {
                mutex.acquire()
                var cachedItem: MwQueryResult.RecentChange? = null
                if (revertCandidateLang != lang) {
                    // evict the cache if the language has changed.
                    revertCandidateCache.clear()
                    revertCandidateLastRevId = 0L
                }
                revertCandidateLang = lang
                if (!revertCandidateCache.isEmpty()) {
                    cachedItem = revertCandidateCache.removeFirst()
                }

                if (cachedItem == null) {
                    val wikiSite = WikiSite.forLanguageCode(lang)
                    while (this.coroutineContext.isActive) {
                        try {
                            // If we have been reset, then fetch a few *older* changes, so that the user
                            // has a few changes to flip through. Otherwise, start fetching *newer* changes,
                            // starting from the last recorded timestamp.
                            val triple = if (revertCandidateLastRevId == 0L)
                                SuggestedEditsRecentEditsViewModel.getRecentEditsCall(wikiSite)
                            else
                                SuggestedEditsRecentEditsViewModel.getRecentEditsCall(wikiSite,
                                    startTimeStamp = revertCandidateLastTimeStamp, direction = "newer")

                            // Retrieve the list of filtered changes from our filter, but *also* get
                            // the list of total changes so that we can update our maxRevId and latest
                            // timestamp, to ensure that our next call will start at the correct point.
                            val filteredChanges = triple.first.sortedBy { it.curRev }
                            val allChanges = triple.second

                            var maxRevId = 0L
                            for (candidate in allChanges) {
                                if (candidate.curRev > maxRevId) {
                                    maxRevId = candidate.curRev
                                }
                                if (candidate.parsedInstant > revertCandidateLastTimeStamp) {
                                    revertCandidateLastTimeStamp = candidate.parsedInstant
                                }
                            }
                            for (candidate in filteredChanges) {
                                if (candidate.curRev > revertCandidateLastRevId) {
                                    revertCandidateCache.addLast(candidate)
                                }
                            }
                            if (maxRevId > revertCandidateLastRevId) {
                                revertCandidateLastRevId = maxRevId
                            }
                            if (!revertCandidateCache.isEmpty()) {
                                cachedItem = revertCandidateCache.removeFirst()
                            }
                            if (cachedItem == null) {
                                throw ListEmptyException()
                            }
                            break
                        } catch (e: ListEmptyException) {
                            // continue indefinitely until new data comes in.
                            Thread.sleep(3000)
                        }
                    }
                }
                cachedItem!!
            } finally {
                mutex.release()
            }
        }
    }

    class ListEmptyException : RuntimeException()
}
