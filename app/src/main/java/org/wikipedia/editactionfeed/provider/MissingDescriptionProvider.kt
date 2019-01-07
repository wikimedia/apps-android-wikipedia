package org.wikipedia.editactionfeed.provider

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
import java.util.*

object MissingDescriptionProvider {
    // TODO: add a maximum-retry limit -- it's currently infinite, or until disposed.

    fun getNextArticleWithMissingDescription(wiki: WikiSite): Observable<RbPageSummary> {
        return ServiceFactory.get(wiki).randomWithPageProps
                .map<List<MwQueryPage>> { response ->
                    val pages = ArrayList<MwQueryPage>()
                    for (page in response.query()!!.pages()!!) {
                        if (page.pageProps() == null || page.pageProps()!!.isDisambiguation || !TextUtils.isEmpty(page.description())) {
                            continue
                        }
                        pages.add(page)
                    }
                    if (pages.isEmpty()) {
                        throw ListEmptyException()
                    }
                    pages
                }
                .flatMap { pages: List<MwQueryPage> -> ServiceFactory.getRest(wiki).getSummary(null, pages[0].title()) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    fun getNextArticleWithMissingDescription(sourceWiki: WikiSite, targetLang: String, sourceLangMustExist: Boolean): Observable<Pair<RbPageSummary, RbPageSummary>> {
        val targetWiki = WikiSite.forLanguageCode(targetLang)

        return ServiceFactory.get(sourceWiki).randomWithPageProps
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

                    for (q in response.entities()!!.keys) {
                        val entity = response.entities()!![q]
                        if (entity == null || !entity.labels().containsKey(sourceWiki.languageCode())
                                || entity.descriptions().containsKey(targetLang)
                                || sourceLangMustExist && !entity.descriptions().containsKey(sourceWiki.languageCode())
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

    private class ListEmptyException : RuntimeException()
}
