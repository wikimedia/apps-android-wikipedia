package org.wikipedia.suggestededits.provider

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.page.PageTitle
import java.util.*
import java.util.concurrent.Semaphore

object MissingDescriptionProvider {
    private val mutex : Semaphore = Semaphore(1)

    private val articlesWithMissingDescriptionCache : Stack<String> = Stack()
    private var articlesWithMissingDescriptionCacheLang : String = ""
    private val articlesWithTranslatableDescriptionCache : Stack<Pair<PageTitle, PageTitle>> = Stack()
    private var articlesWithTranslatableDescriptionCacheFromLang : String = ""
    private var articlesWithTranslatableDescriptionCacheToLang : String = ""

    private val imagesWithMissingCaptionsCache : Stack<String> = Stack()
    private var imagesWithMissingCaptionsCacheLang : String = ""
    private val imagesWithTranslatableCaptionCache : Stack<Pair<String, String>> = Stack()
    private var imagesWithTranslatableCaptionCacheFromLang : String = ""
    private var imagesWithTranslatableCaptionCacheToLang : String = ""

    // TODO: add a maximum-retry limit -- it's currently infinite, or until disposed.

    fun getNextArticleWithMissingDescription(wiki: WikiSite): Observable<RbPageSummary> {
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
                ServiceFactory.getRest(WikiSite(Service.WIKIDATA_URL)).getArticlesWithoutDescriptions(wiki.languageCode())
                        .map { pages ->
                            var title: String? = null
                            articlesWithMissingDescriptionCacheLang = wiki.languageCode()
                            for (page in pages) {
                                articlesWithMissingDescriptionCache.push(page.title)
                            }
                            if (!articlesWithMissingDescriptionCache.empty()) {
                                title = articlesWithMissingDescriptionCache.pop()
                            }
                            if (title == null) {
                                throw ListEmptyException()
                            }
                            title
                        }
                        .retry { t: Throwable -> t is ListEmptyException }
            }
        }.flatMap { title -> ServiceFactory.getRest(wiki).getSummary(null, title) }
                .doFinally { mutex.release() }
    }

    fun getNextArticleWithMissingDescription(sourceWiki: WikiSite, targetLang: String, sourceLangMustExist: Boolean): Observable<Pair<RbPageSummary, RbPageSummary>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            val targetWiki = WikiSite.forLanguageCode(targetLang)
            var cachedPair: Pair<PageTitle, PageTitle>? = null
            if (articlesWithTranslatableDescriptionCacheFromLang != sourceWiki.languageCode()
                    || articlesWithTranslatableDescriptionCacheToLang != targetLang) {
                // evict the cache if the language has changed.
                articlesWithTranslatableDescriptionCache.clear()
            }
            if (!articlesWithTranslatableDescriptionCache.empty()) {
                cachedPair = articlesWithTranslatableDescriptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(WikiSite(Service.WIKIDATA_URL)).getArticlesWithTranslatableDescriptions(sourceWiki.languageCode(), targetLang)
                        .map { pages ->
                            var sourceAndTargetPageTitles: Pair<PageTitle, PageTitle>? = null
                            articlesWithTranslatableDescriptionCacheFromLang = sourceWiki.languageCode()
                            articlesWithTranslatableDescriptionCacheToLang = targetLang
                            for (page in pages) {
                                val entity = page.entity
                                if (entity == null
                                        || entity.descriptions().containsKey(targetLang)
                                        || sourceLangMustExist && !entity.descriptions().containsKey(sourceWiki.languageCode())
                                        || !entity.sitelinks().containsKey(sourceWiki.dbName())
                                        || !entity.sitelinks().containsKey(targetWiki.dbName())) {
                                    continue
                                }
                                articlesWithTranslatableDescriptionCache.push(Pair(PageTitle(entity.sitelinks()[targetWiki.dbName()]!!.title, targetWiki),
                                        PageTitle(entity.sitelinks()[sourceWiki.dbName()]!!.title, sourceWiki)))
                            }
                            if (!articlesWithTranslatableDescriptionCache.empty()) {
                                sourceAndTargetPageTitles = articlesWithTranslatableDescriptionCache.pop()
                            }
                            if (sourceAndTargetPageTitles == null) {
                                throw ListEmptyException()
                            }
                            sourceAndTargetPageTitles
                        }
                        .retry { t: Throwable -> t is ListEmptyException }
            }
        }.flatMap { sourceAndTargetPageTitles: Pair<PageTitle, PageTitle> -> getSummary(sourceAndTargetPageTitles) }
                .doFinally { mutex.release() }
    }

    private fun getSummary(titles: Pair<PageTitle, PageTitle>): Observable<Pair<RbPageSummary, RbPageSummary>> {
        return Observable.zip(ServiceFactory.getRest(titles.first.wikiSite).getSummary(null, titles.first.prefixedText),
                ServiceFactory.getRest(titles.second.wikiSite).getSummary(null, titles.second.prefixedText),
                BiFunction<RbPageSummary, RbPageSummary, Pair<RbPageSummary, RbPageSummary>> { source, target -> Pair(source, target) })
    }

    fun getNextImageWithMissingCaption(lang: String): Observable<String> {
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
                ServiceFactory.getRest(WikiSite(Service.COMMONS_URL)).getImagesWithoutCaptions(lang)
                        .map { pages ->
                            imagesWithMissingCaptionsCacheLang = lang
                            for (page in pages) {
                                imagesWithMissingCaptionsCache.push(page.title)
                            }
                            var item: String? = null
                            if (!imagesWithMissingCaptionsCache.empty()) {
                                item = imagesWithMissingCaptionsCache.pop()
                            }
                            if (item == null) {
                                throw ListEmptyException()
                            }
                            item
                        }
                        .retry { t: Throwable -> t is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    fun getNextImageWithMissingCaption(sourceLang: String, targetLang: String): Observable<Pair<String, String>> {
        return Observable.fromCallable { mutex.acquire() }.flatMap {
            var cachedPair: Pair<String, String>? = null
            if (imagesWithTranslatableCaptionCacheFromLang != sourceLang
                    || imagesWithTranslatableCaptionCacheToLang != targetLang) {
                // evict the cache if the language has changed.
                imagesWithTranslatableCaptionCache.clear()
            }
            if (!imagesWithTranslatableCaptionCache.empty()) {
                cachedPair = imagesWithTranslatableCaptionCache.pop()
            }

            if (cachedPair != null) {
                Observable.just(cachedPair)
            } else {
                ServiceFactory.getRest(WikiSite(Service.COMMONS_URL)).getImagesWithTranslatableCaptions(sourceLang, targetLang)
                        .map { pages ->
                            imagesWithTranslatableCaptionCacheFromLang = sourceLang
                            imagesWithTranslatableCaptionCacheToLang = targetLang

                            var item: Pair<String, String>? = null
                            for (page in pages) {
                                if (!page.captions.containsKey(sourceLang) || page.captions.containsKey(targetLang)) {
                                    continue
                                }
                                imagesWithTranslatableCaptionCache.push(Pair(page.captions[sourceLang]!!, page.title))
                            }
                            if (!imagesWithTranslatableCaptionCache.empty()) {
                                item = imagesWithTranslatableCaptionCache.pop()
                            }
                            if (item == null) {
                                throw ListEmptyException()
                            }
                            item
                        }
                        .retry { t: Throwable -> t is ListEmptyException }
            }
        }.doFinally { mutex.release() }
    }

    private class ListEmptyException : RuntimeException()
}
