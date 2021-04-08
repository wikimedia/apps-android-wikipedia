package org.wikipedia.suggestededits.provider

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.ImageRecommendationResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList
import kotlin.random.Random

object EditingSuggestionsProvider {
    private val mutex: Semaphore = Semaphore(1)

    private val articlesWithMissingDescriptionCache: Stack<String> = Stack()
    private var articlesWithMissingDescriptionCacheLang: String = ""
    private val articlesWithTranslatableDescriptionCache: Stack<Pair<PageTitle, PageTitle>> = Stack()
    private var articlesWithTranslatableDescriptionCacheFromLang: String = ""
    private var articlesWithTranslatableDescriptionCacheToLang: String = ""

    private val imagesWithMissingCaptionsCache: Stack<String> = Stack()
    private var imagesWithMissingCaptionsCacheLang: String = ""
    private val imagesWithTranslatableCaptionCache: Stack<Pair<String, String>> = Stack()
    private var imagesWithTranslatableCaptionCacheFromLang: String = ""
    private var imagesWithTranslatableCaptionCacheToLang: String = ""

    private val imagesWithMissingTagsCache: Stack<MwQueryPage> = Stack()

    private val articlesWithMissingImagesCache = mutableListOf<String>()
    private var articlesWithMissingImagesCacheLang: String = ""

    private const val MAX_RETRY_LIMIT: Long = 50

    fun getNextArticleWithMissingDescription(wiki: WikiSite, retryLimit: Long = MAX_RETRY_LIMIT): Observable<PageSummary> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedTitle = ""
            if (articlesWithMissingDescriptionCacheLang != wiki.languageCode()) {
                // evict the cache if the language has changed.
                articlesWithMissingDescriptionCache.clear()
            }
            if (!articlesWithMissingDescriptionCache.empty()) {
                cachedTitle = articlesWithMissingDescriptionCache.pop()
            }

            if (cachedTitle.isNotEmpty()) {
                Observable.just(cachedTitle)
            } else {
                ServiceFactory.getRest(WikiSite(Service.WIKIDATA_URL)).getArticlesWithoutDescriptions(WikiSite.normalizeLanguageCode(wiki.languageCode()))
                        .flatMap { pages ->
                            val titleList = ArrayList<String>()
                            pages.forEach { titleList.add(it.title()) }
                            ServiceFactory.get(wiki).getDescription(titleList.joinToString("|")).subscribeOn(Schedulers.io())
                        }
                        .map { pages ->
                            var title: String? = null
                            articlesWithMissingDescriptionCacheLang = wiki.languageCode()
                            pages.query()!!.pages()!!.forEach {
                                if (it.description().isNullOrEmpty()) {
                                    articlesWithMissingDescriptionCache.push(it.title())
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
                        .retry(retryLimit) { t: Throwable -> t is ListEmptyException }
            }
        }.flatMap { title -> ServiceFactory.getRest(wiki).getSummary(null, title) }
                .doFinally { mutex.release() }
    }

    fun getNextArticleWithMissingDescription(sourceWiki: WikiSite, targetLang: String, sourceLangMustExist: Boolean, retryLimit: Long = MAX_RETRY_LIMIT): Observable<Pair<PageSummary, PageSummary>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            val targetWiki = WikiSite.forLanguageCode(targetLang)
            var cachedPair: Pair<PageTitle, PageTitle>? = null
            if (articlesWithTranslatableDescriptionCacheFromLang != sourceWiki.languageCode() ||
                    articlesWithTranslatableDescriptionCacheToLang != targetLang) {
                // evict the cache if the language has changed.
                articlesWithTranslatableDescriptionCache.clear()
            }
            if (!articlesWithTranslatableDescriptionCache.empty()) {
                cachedPair = articlesWithTranslatableDescriptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(WikiSite(Service.WIKIDATA_URL)).getArticlesWithTranslatableDescriptions(WikiSite.normalizeLanguageCode(sourceWiki.languageCode()), WikiSite.normalizeLanguageCode(targetLang))
                        .map { pages ->
                            var targetAndSourcePageTitles: Pair<PageTitle, PageTitle>? = null
                            articlesWithTranslatableDescriptionCacheFromLang = sourceWiki.languageCode()
                            articlesWithTranslatableDescriptionCacheToLang = targetLang
                            for (page in pages) {
                                val entity = page.entity
                                if (entity == null ||
                                        entity.descriptions().containsKey(targetLang) ||
                                        sourceLangMustExist && !entity.descriptions().containsKey(sourceWiki.languageCode()) ||
                                        !entity.sitelinks().containsKey(sourceWiki.dbName()) ||
                                        !entity.sitelinks().containsKey(targetWiki.dbName())) {
                                    continue
                                }
                                val sourceTitle = PageTitle(entity.sitelinks()[sourceWiki.dbName()]!!.title, sourceWiki)
                                sourceTitle.description = entity.descriptions()[sourceWiki.languageCode()]?.value()
                                articlesWithTranslatableDescriptionCache.push(PageTitle(entity.sitelinks()[targetWiki.dbName()]!!.title, targetWiki) to sourceTitle)
                            }
                            if (!articlesWithTranslatableDescriptionCache.empty()) {
                                targetAndSourcePageTitles = articlesWithTranslatableDescriptionCache.pop()
                            }
                            if (targetAndSourcePageTitles == null) {
                                throw ListEmptyException()
                            }
                            targetAndSourcePageTitles
                        }
                        .retry(retryLimit) { t: Throwable ->
                            t is ListEmptyException
                        }
            }
        }.flatMap { getSummary(it) }
                .doFinally { mutex.release() }
    }

    private fun getSummary(targetAndSourcePageTitles: Pair<PageTitle, PageTitle>): Observable<Pair<PageSummary, PageSummary>> {
        return Observable.zip(ServiceFactory.getRest(targetAndSourcePageTitles.first.wikiSite).getSummary(null, targetAndSourcePageTitles.first.prefixedText),
                ServiceFactory.getRest(targetAndSourcePageTitles.second.wikiSite).getSummary(null, targetAndSourcePageTitles.second.prefixedText), { target, source ->
            if (target.description.isNullOrEmpty()) {
                target.description = targetAndSourcePageTitles.first.description
            }
            Pair(source, target)
        })
    }

    fun getNextImageWithMissingCaption(lang: String, retryLimit: Long = MAX_RETRY_LIMIT): Observable<String> {
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
                ServiceFactory.getRest(WikiSite(Service.COMMONS_URL)).getImagesWithoutCaptions(WikiSite.normalizeLanguageCode(lang))
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
                        .retry(retryLimit) { t: Throwable -> t is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    fun getNextImageWithMissingCaption(sourceLang: String, targetLang: String, retryLimit: Long = MAX_RETRY_LIMIT): Observable<Pair<String, String>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedPair: Pair<String, String>? = null
            if (imagesWithTranslatableCaptionCacheFromLang != sourceLang ||
                    imagesWithTranslatableCaptionCacheToLang != targetLang) {
                // evict the cache if the language has changed.
                imagesWithTranslatableCaptionCache.clear()
            }
            if (!imagesWithTranslatableCaptionCache.empty()) {
                cachedPair = imagesWithTranslatableCaptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(WikiSite(Service.COMMONS_URL)).getImagesWithTranslatableCaptions(WikiSite.normalizeLanguageCode(sourceLang), WikiSite.normalizeLanguageCode(targetLang))
                        .map { pages ->
                            imagesWithTranslatableCaptionCacheFromLang = sourceLang
                            imagesWithTranslatableCaptionCacheToLang = targetLang

                            var item: Pair<String, String>? = null
                            for (page in pages) {
                                if (!page.captions.containsKey(sourceLang) || page.captions.containsKey(targetLang)) {
                                    continue
                                }
                                imagesWithTranslatableCaptionCache.push((page.captions[sourceLang] ?: error("")) to page.title())
                            }
                            if (!imagesWithTranslatableCaptionCache.empty()) {
                                item = imagesWithTranslatableCaptionCache.pop()
                            }
                            if (item == null) {
                                throw ListEmptyException()
                            }
                            item
                        }
                        .retry(retryLimit) { t: Throwable -> t is ListEmptyException }
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
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).randomWithImageInfo
                        .map { response ->
                            for (page in response.query()!!.pages()!!) {
                                if (page.imageInfo()!!.mimeType != "image/jpeg") {
                                    continue
                                }
                                if (page.revisions().none { "P180" in it.getContentFromSlot("mediainfo") }) {
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
                        .retry(retryLimit) { t: Throwable -> t is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    fun getNextArticleWithMissingImage(lang: String, sequence: Int): Observable<ImageRecommendationResponse> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedItem: ImageRecommendationResponse? = null
            if (articlesWithMissingImagesCacheLang != lang) {
                // evict the cache if the language has changed.
                articlesWithMissingImagesCache.clear()
            }
            val installIdMod = 1000
            val installId = Prefs.getAppInstallId().orEmpty()
            // Initialize the random number generator based on the user's install ID, modulo the
            // total size of the expected test group.
            val random = Random(installId.substring(installId.length - 4).toInt(16) % installIdMod)
            var seqOffset = 0
            // and seek to the appropriate position in the random sequence.
            for (i in 0..sequence) {
                seqOffset = random.nextInt(Int.MAX_VALUE)
            }

            articlesWithMissingImagesCacheLang = lang
            if (articlesWithMissingImagesCache.isNotEmpty()) {
                cachedItem = buildImageRecommendation(articlesWithMissingImagesCache[seqOffset % articlesWithMissingImagesCache.size])
            }

            if (cachedItem != null) {
                Observable.just(cachedItem)
            } else {
                val stream = WikipediaApp.getInstance().assets.open(lang + "wiki_wd_image_candidates.tsv")
                val reader = BufferedReader(InputStreamReader(stream))
                while (true) {
                    val line = reader.readLine()
                    if (line.isNullOrEmpty()) {
                        break
                    }
                    articlesWithMissingImagesCache.add(line)
                }

                var item: ImageRecommendationResponse? = null
                if (articlesWithMissingImagesCache.isNotEmpty()) {
                    item = buildImageRecommendation(articlesWithMissingImagesCache[seqOffset % articlesWithMissingImagesCache.size])
                }
                Observable.just(item!!)
            }
        }.doFinally { mutex.release() }
    }

    private fun buildImageRecommendation(str: String): ImageRecommendationResponse {
        val arr = str.split('\t')
        return ImageRecommendationResponse(arr[0].toInt(), arr[1], arr[2].split(",").toList())
    }

    class ListEmptyException : RuntimeException()
}
