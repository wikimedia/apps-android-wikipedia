package org.wikipedia.suggestededits.provider

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsRecentEditsViewModel
import java.time.Instant
import java.util.Stack
import java.util.concurrent.Semaphore
import kotlin.math.abs

object EditingSuggestionsProvider {
    private val mutex: Semaphore = Semaphore(1)

    private val articlesWithMissingDescriptionCache: Stack<String> = Stack()
    private var articlesWithMissingDescriptionCacheLang: String = ""
    private val articlesWithTranslatableDescriptionCache: Stack<Pair<PageTitle, PageTitle>> =
        Stack()
    private var articlesWithTranslatableDescriptionCacheFromLang: String = ""
    private var articlesWithTranslatableDescriptionCacheToLang: String = ""

    private val imagesWithMissingCaptionsCache: Stack<String> = Stack()
    private var imagesWithMissingCaptionsCacheLang: String = ""
    private val imagesWithTranslatableCaptionCache: Stack<Pair<String, String>> = Stack()
    private var imagesWithTranslatableCaptionCacheFromLang: String = ""
    private var imagesWithTranslatableCaptionCacheToLang: String = ""

    private val imagesWithMissingTagsCache: Stack<MwQueryPage> = Stack()

    private val articlesWithImageRecommendationsCache: Stack<MwQueryPage> = Stack()
    private var articlesWithImageRecommendationsCacheLang: String = ""
    private var articlesWithImageRecommendationsLastMillis: Long = 0

    private var revertCandidateLang: String = ""
    private val revertCandidateCache: ArrayDeque<MwQueryResult.RecentChange> = ArrayDeque()
    private var revertCandidateLastRevId = 0L
    private var revertCandidateLastTimeStamp = Instant.now()

    private const val MAX_RETRY_LIMIT: Long = 50

    fun getNextArticleWithMissingDescription(
        wiki: WikiSite,
        retryLimit: Long = MAX_RETRY_LIMIT
    ): Observable<PageSummary> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedTitle = ""
            if (articlesWithMissingDescriptionCacheLang != wiki.languageCode) {
                // evict the cache if the language has changed.
                articlesWithMissingDescriptionCache.clear()
            }
            if (!articlesWithMissingDescriptionCache.empty()) {
                cachedTitle = articlesWithMissingDescriptionCache.pop()
            }

            if (cachedTitle.isNotEmpty()) {
                Observable.just(cachedTitle)
            } else {
                ServiceFactory.getRest(Constants.wikidataWikiSite)
                    .getArticlesWithoutDescriptions(WikiSite.normalizeLanguageCode(wiki.languageCode))
                    .flatMap { pages ->
                        ServiceFactory.get(wiki)
                            .getDescription(pages.joinToString("|") { it.title() })
                            .subscribeOn(Schedulers.io())
                    }
                    .map { pages ->
                        var title: String? = null
                        articlesWithMissingDescriptionCacheLang = wiki.languageCode
                        pages.query?.pages?.forEach {
                            if (it.description.isNullOrEmpty()) {
                                articlesWithMissingDescriptionCache.push(it.title)
                            }
                        }
                        if (!articlesWithMissingDescriptionCache.empty()) {
                            title = articlesWithMissingDescriptionCache.pop()
                        }
                        if (title == null) {
                            throw ListEmptyException()
                        }
                        title
                    }
                    .retry(retryLimit) { it is ListEmptyException }
            }
        }.flatMap { title -> ServiceFactory.getRest(wiki).getSummary(null, title) }
            .doFinally { mutex.release() }
    }

    fun getNextArticleWithMissingDescription(
        sourceWiki: WikiSite,
        targetLang: String,
        sourceLangMustExist: Boolean,
        retryLimit: Long = MAX_RETRY_LIMIT
    ): Observable<Pair<PageSummary, PageSummary>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            val targetWiki = WikiSite.forLanguageCode(targetLang)
            var cachedPair: Pair<PageTitle, PageTitle>? = null
            if (articlesWithTranslatableDescriptionCacheFromLang != sourceWiki.languageCode ||
                articlesWithTranslatableDescriptionCacheToLang != targetLang
            ) {
                // evict the cache if the language has changed.
                articlesWithTranslatableDescriptionCache.clear()
            }
            if (!articlesWithTranslatableDescriptionCache.empty()) {
                cachedPair = articlesWithTranslatableDescriptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(Constants.wikidataWikiSite)
                    .getArticlesWithTranslatableDescriptions(
                        WikiSite.normalizeLanguageCode(sourceWiki.languageCode),
                        WikiSite.normalizeLanguageCode(targetLang)
                    )
                    .flatMap({ pages ->
                        if (pages.isEmpty()) {
                            throw ListEmptyException()
                        }
                        val titleList = pages.map { it.title() }
                        ServiceFactory.get(WikiSite.forLanguageCode(targetLang))
                            .getDescription(titleList.joinToString("|"))
                    }, { pages, response -> Pair(pages, response) })
                    .map { pair ->
                        val pages = pair.first
                        val mwPages = pair.second.query?.pages!!
                        var targetAndSourcePageTitles: Pair<PageTitle, PageTitle>? = null
                        articlesWithTranslatableDescriptionCacheFromLang = sourceWiki.languageCode
                        articlesWithTranslatableDescriptionCacheToLang = targetLang
                        for (page in pages) {
                            val mwPage = mwPages.find { it.title == page.title() }
                            if (mwPage != null && !mwPage.description.isNullOrEmpty()) {
                                continue
                            }
                            val entity = page.entity
                            if (entity == null || entity.descriptions.containsKey(targetLang) ||
                                sourceLangMustExist && !entity.descriptions.containsKey(sourceWiki.languageCode) ||
                                !entity.sitelinks.containsKey(sourceWiki.dbName()) ||
                                !entity.sitelinks.containsKey(targetWiki.dbName())
                            ) {
                                continue
                            }
                            val sourceTitle =
                                PageTitle(entity.sitelinks[sourceWiki.dbName()]!!.title, sourceWiki)
                            sourceTitle.description =
                                entity.descriptions[sourceWiki.languageCode]?.value
                            articlesWithTranslatableDescriptionCache.push(
                                PageTitle(
                                    entity.sitelinks[targetWiki.dbName()]!!.title,
                                    targetWiki
                                ) to sourceTitle
                            )
                        }
                        if (!articlesWithTranslatableDescriptionCache.empty()) {
                            targetAndSourcePageTitles =
                                articlesWithTranslatableDescriptionCache.pop()
                        }
                        if (targetAndSourcePageTitles == null) {
                            throw ListEmptyException()
                        }
                        targetAndSourcePageTitles
                    }
                    .retry(retryLimit) { it is ListEmptyException }
            }
        }.flatMap { getSummary(it) }
            .doFinally { mutex.release() }
    }

    private fun getSummary(targetAndSourcePageTitles: Pair<PageTitle, PageTitle>): Observable<Pair<PageSummary, PageSummary>> {
        return Observable.zip(ServiceFactory.getRest(targetAndSourcePageTitles.first.wikiSite)
            .getSummary(null, targetAndSourcePageTitles.first.prefixedText),
            ServiceFactory.getRest(targetAndSourcePageTitles.second.wikiSite)
                .getSummary(null, targetAndSourcePageTitles.second.prefixedText),
            { target, source ->
                if (target.description.isNullOrEmpty()) {
                    target.description = targetAndSourcePageTitles.first.description
                }
                Pair(source, target)
            })
    }

    fun getNextImageWithMissingCaption(
        lang: String,
        retryLimit: Long = MAX_RETRY_LIMIT
    ): Observable<String> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedTitle: String? = null
            if (imagesWithMissingCaptionsCacheLang != lang) {
                // evict the cache if the language has changed.
                imagesWithMissingCaptionsCache.clear()
            }
            if (!imagesWithMissingCaptionsCache.empty()) {
                cachedTitle = imagesWithMissingCaptionsCache.pop()
            }

            if (cachedTitle != null) {
                Observable.just(cachedTitle)
            } else {
                ServiceFactory.getRest(Constants.commonsWikiSite)
                    .getImagesWithoutCaptions(WikiSite.normalizeLanguageCode(lang))
                    .map { pages ->
                        imagesWithMissingCaptionsCacheLang = lang
                        pages.forEach { imagesWithMissingCaptionsCache.push(it.title()) }
                        var item: String? = null
                        if (!imagesWithMissingCaptionsCache.empty()) {
                            item = imagesWithMissingCaptionsCache.pop()
                        }
                        if (item == null) {
                            throw ListEmptyException()
                        }
                        item
                    }
                    .retry(retryLimit) { it is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    fun getNextImageWithMissingCaption(
        sourceLang: String,
        targetLang: String,
        retryLimit: Long = MAX_RETRY_LIMIT
    ): Observable<Pair<String, String>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedPair: Pair<String, String>? = null
            if (imagesWithTranslatableCaptionCacheFromLang != sourceLang ||
                imagesWithTranslatableCaptionCacheToLang != targetLang
            ) {
                // evict the cache if the language has changed.
                imagesWithTranslatableCaptionCache.clear()
            }
            if (!imagesWithTranslatableCaptionCache.empty()) {
                cachedPair = imagesWithTranslatableCaptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(Constants.commonsWikiSite).getImagesWithTranslatableCaptions(
                    WikiSite.normalizeLanguageCode(sourceLang),
                    WikiSite.normalizeLanguageCode(targetLang)
                )
                    .map { pages ->
                        imagesWithTranslatableCaptionCacheFromLang = sourceLang
                        imagesWithTranslatableCaptionCacheToLang = targetLang

                        var item: Pair<String, String>? = null
                        for (page in pages) {
                            if (!page.captions.containsKey(sourceLang) || page.captions.containsKey(
                                    targetLang
                                )
                            ) {
                                continue
                            }
                            imagesWithTranslatableCaptionCache.push(
                                (page.captions[sourceLang]
                                    ?: error("")) to page.title()
                            )
                        }
                        if (!imagesWithTranslatableCaptionCache.empty()) {
                            item = imagesWithTranslatableCaptionCache.pop()
                        }
                        if (item == null) {
                            throw ListEmptyException()
                        }
                        item
                    }
                    .retry(retryLimit) { it is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    fun getNextImageWithMissingTags(retryLimit: Long = MAX_RETRY_LIMIT): Observable<MwQueryPage> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedItem: MwQueryPage? = null
            if (!imagesWithMissingTagsCache.empty()) {
                cachedItem = imagesWithMissingTagsCache.pop()
            }

            if (cachedItem != null) {
                Observable.just(cachedItem)
            } else {
                ServiceFactory.get(Constants.commonsWikiSite).randomWithImageInfo
                    .map { response ->
                        response.query?.pages?.filter { it.imageInfo()?.mime == "image/jpeg" }
                            ?.forEach { page ->
                                if (page.revisions.none { "P180" in it.getContentFromSlot("mediainfo") }) {
                                    imagesWithMissingTagsCache.push(page)
                                }
                            }
                        var item: MwQueryPage? = null
                        if (!imagesWithMissingTagsCache.empty()) {
                            item = imagesWithMissingTagsCache.pop()
                        }
                        if (item == null) {
                            throw ListEmptyException()
                        }
                        item
                    }
                    .retry(retryLimit) { it is ListEmptyException }
            }
        }.doFinally { mutex.release() }
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
                    if (articlesWithImageRecommendationsCache.empty()) {
                        val response = ServiceFactory.get(WikiSite.forLanguageCode(articlesWithImageRecommendationsCacheLang))
                            .getPagesWithImageRecommendations(10)
                        // TODO: make use of continuation parameter?
                        response.query?.pages?.forEach { page ->
                            if (page.thumbUrl().isNullOrEmpty() && page.growthimagesuggestiondata?.get(0)?.images?.get(0) != null) {
                                articlesWithImageRecommendationsCache.push(page)
                            }
                        }
                    }
                } while (tries++ < retryLimit && articlesWithImageRecommendationsCache.empty())

                page = articlesWithImageRecommendationsCache.pop()
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
