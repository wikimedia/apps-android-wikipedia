package org.wikipedia.suggestededits.provider

import android.text.TextUtils
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.wikidata.Entities
import java.util.*

object MissingDescriptionProvider {
    private val articlesWithMissingDescriptionCache : Stack<String> = Stack()
    private var articlesWithMissingDescriptionCacheLang : String = ""
    private val articlesWithTranslatableDescriptionCache : Stack<Pair<PageTitle, PageTitle>> = Stack()
    private var articlesWithTranslatableDescriptionCacheFromLang : String = ""
    private var articlesWithTranslatableDescriptionCacheToLang : String = ""

    // TODO: add a maximum-retry limit -- it's currently infinite, or until disposed.

    @Deprecated("Remove when the new API is deployed to production.")
    fun getNextArticleWithMissingDescription(wiki: WikiSite): Observable<RbPageSummary> {
        var cachedTitle = ""
        synchronized(articlesWithMissingDescriptionCache) {
            if (articlesWithMissingDescriptionCacheLang != wiki.languageCode()) {
                // evict the cache if the language has changed.
                articlesWithMissingDescriptionCache.clear()
            }
            if (!articlesWithMissingDescriptionCache.empty()) {
                cachedTitle = articlesWithMissingDescriptionCache.pop()
            }
        }

        return (if (!TextUtils.isEmpty(cachedTitle)) Observable.just(cachedTitle) else
            ServiceFactory.get(wiki).randomWithPageProps
                    .map<String> { response ->
                        var title : String? = null
                        synchronized(articlesWithMissingDescriptionCache) {
                            articlesWithMissingDescriptionCacheLang = wiki.languageCode()
                            for (page in response.query()!!.pages()!!) {
                                if (page.pageProps() == null || page.pageProps()!!.isDisambiguation || !TextUtils.isEmpty(page.description())) {
                                    continue
                                }
                                articlesWithMissingDescriptionCache.push(page.title())
                            }
                            if (!articlesWithMissingDescriptionCache.empty()) {
                                title = articlesWithMissingDescriptionCache.pop()
                            }
                        }
                        if (title == null) {
                            throw ListEmptyException()
                        }
                        title
                    }
                )
                .flatMap { title -> ServiceFactory.getRest(wiki).getSummary(null, title) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    fun getNextArticleWithMissingDescriptionNew(wiki: WikiSite): Observable<RbPageSummary> {
        return ServiceFactory.get(wiki).getEditorTaskMissingDescriptions(wiki.languageCode())
                .map<MwQueryPage> { response ->
                    response.query()!!.pages()!![0]
                }
                .flatMap { page: MwQueryPage -> ServiceFactory.getRest(wiki).getSummary(null, page.title()) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    @Deprecated("Remove when the new API is deployed to production.")
    fun getNextArticleWithMissingDescription(sourceWiki: WikiSite, targetLang: String, sourceLangMustExist: Boolean): Observable<Pair<RbPageSummary, RbPageSummary>> {
        val targetWiki = WikiSite.forLanguageCode(targetLang)
        var cachedPair: Pair<PageTitle, PageTitle>? = null
        synchronized(articlesWithTranslatableDescriptionCache) {
            if (articlesWithTranslatableDescriptionCacheFromLang != sourceWiki.languageCode()
                    || articlesWithTranslatableDescriptionCacheToLang != targetLang) {
                // evict the cache if the language has changed.
                articlesWithTranslatableDescriptionCache.clear()
            }
            if (!articlesWithTranslatableDescriptionCache.empty()) {
                cachedPair = articlesWithTranslatableDescriptionCache.pop()
            }
        }

        return (if (cachedPair != null) Observable.just(cachedPair) else
            ServiceFactory.get(sourceWiki).randomWithPageProps
                    .flatMap { response: MwQueryResponse ->
                        val qNumbers = ArrayList<String>()
                        for (page in response.query()!!.pages()!!) {
                            if (page.pageProps() == null || page.pageProps()!!.isDisambiguation || TextUtils.isEmpty(page.pageProps()!!.wikiBaseItem)) {
                                continue
                            }
                            qNumbers.add(page.pageProps()!!.wikiBaseItem)
                        }
                        ServiceFactory.get(WikiSite(Service.WIKIDATA_URL))
                                .getWikidataLabelsAndDescriptions(StringUtils.join(qNumbers, '|'))
                    }
                    .map<Pair<PageTitle, PageTitle>> { response ->
                        var sourceAndTargetPageTitles: Pair<PageTitle, PageTitle>? = null
                        synchronized(articlesWithTranslatableDescriptionCache) {
                            articlesWithTranslatableDescriptionCacheFromLang = sourceWiki.languageCode()
                            articlesWithTranslatableDescriptionCacheToLang = targetLang
                            for (q in response.entities()!!.keys) {
                                val entity = response.entities()!![q]
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
                        }
                        if (sourceAndTargetPageTitles == null) {
                            throw ListEmptyException()
                        }
                        sourceAndTargetPageTitles
                    })
                .flatMap { sourceAndTargetPageTitles: Pair<PageTitle, PageTitle> -> getSummary(sourceAndTargetPageTitles) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    fun getNextArticleWithMissingDescriptionNew(sourceWiki: WikiSite, targetLang: String): Observable<Pair<RbPageSummary, RbPageSummary>> {
        val targetWiki = WikiSite.forLanguageCode(targetLang)
        return ServiceFactory.get(sourceWiki).getEditorTaskTranslatableDescriptions(sourceWiki.languageCode(), targetLang)
                .flatMap { response: MwQueryResponse ->
                    val qNumbers = ArrayList<String>()
                    for (page in response.query()!!.pages()!!) {
                        qNumbers.add(page.title())
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL))
                            .getWikidataLabelsAndDescriptions(StringUtils.join(qNumbers, '|'))
                }
                .map<Pair<PageTitle, PageTitle>> { response ->
                    var sourceAndTargetPageTitles: Pair<PageTitle, PageTitle>? = null
                    for (q in response.entities()!!.keys) {
                        val entity = response.entities()!![q]
                        if (entity == null
                                || entity.descriptions().containsKey(targetLang)
                                || !entity.descriptions().containsKey(sourceWiki.languageCode())
                                || !entity.sitelinks().containsKey(sourceWiki.dbName())
                                || !entity.sitelinks().containsKey(targetWiki.dbName())) {
                            continue
                        }
                        sourceAndTargetPageTitles = Pair(PageTitle(entity.sitelinks()[sourceWiki.dbName()]!!.title, sourceWiki),
                                PageTitle(entity.sitelinks()[targetWiki.dbName()]!!.title, targetWiki))
                        break
                    }
                    if (sourceAndTargetPageTitles == null) {
                        throw ListEmptyException()
                    }
                    sourceAndTargetPageTitles
                }
                .flatMap { sourceAndTargetPageTitles: Pair<PageTitle, PageTitle> -> getSummary(sourceAndTargetPageTitles) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    private fun getSummary(titles: Pair<PageTitle, PageTitle>): Observable<Pair<RbPageSummary, RbPageSummary>> {
        return Observable.zip(ServiceFactory.getRest(titles.first.wikiSite).getSummary(null, titles.first.prefixedText),
                ServiceFactory.getRest(titles.second.wikiSite).getSummary(null, titles.second.prefixedText),
                BiFunction<RbPageSummary, RbPageSummary, Pair<RbPageSummary, RbPageSummary>> { source, target -> Pair(source, target) })
    }

    fun getNextImageWithMissingCaption(lang: String): Observable<MwQueryPage> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).randomWithImageInfo
                .flatMap<Entities, MwQueryPage>({ result: MwQueryResponse ->
                    val pages = result.query()!!.pages()
                    val mNumbers = ArrayList<String>()
                    for (page in pages!!) {
                        mNumbers.add("M" + page.pageId())
                    }
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getWikidataLabelsAndDescriptions(StringUtils.join(mNumbers, '|'))
                }, { mwQueryResponse, entities ->
                    var item: MwQueryPage? = null
                    for (m in entities.entities()!!.keys) {
                        if (entities.entities()!![m]?.labels() != null && entities.entities()!![m]?.labels()!!.containsKey(lang)) {
                            continue
                        }
                        for (page in mwQueryResponse.query()!!.pages()!!) {
                            if (m == "M" + page.pageId()) {
                                item = page
                                break
                            }
                        }
                    }
                    if (item == null) {
                        throw ListEmptyException()
                    }
                    item
                })
                .retry { t: Throwable -> t is ListEmptyException }
    }

    fun getNextImageWithMissingCaption(sourceLang: String, targetLang: String): Observable<Pair<String, MwQueryPage>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).randomWithImageInfo
                .flatMap<Entities, Pair<String, MwQueryPage>>({ result: MwQueryResponse ->
                    val pages = result.query()!!.pages()
                    val mNumbers = ArrayList<String>()
                    for (page in pages!!) {
                        mNumbers.add("M" + page.pageId())
                    }
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getWikidataLabelsAndDescriptions(StringUtils.join(mNumbers, '|'))
                }, { mwQueryResponse, entities ->
                    var item: Pair<String, MwQueryPage>? = null
                    for (m in entities.entities()!!.keys) {
                        if (entities.entities()!![m]?.labels() == null || !entities.entities()!![m]?.labels()!!.containsKey(sourceLang)
                                || entities.entities()!![m]?.labels()!!.containsKey(targetLang)) {
                            continue
                        }
                        for (page in mwQueryResponse.query()!!.pages()!!) {
                            if (m == "M" + page.pageId()) {
                                item = Pair(entities.entities()!![m]?.labels()!![sourceLang]!!.value(), page)
                                break
                            }
                        }
                    }
                    if (item == null) {
                        throw ListEmptyException()
                    }
                    item
                })
                .retry { t: Throwable -> t is ListEmptyException }
    }

    private class ListEmptyException : RuntimeException()
}
