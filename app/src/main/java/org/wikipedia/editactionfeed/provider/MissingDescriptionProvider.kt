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
                .map<List<PageTitle>> { response ->
                    val sourceAndTargetPageTitles = ArrayList<PageTitle>()

                    for (q in response.entities()!!.keys) {
                        val entity = response.entities()!![q]
                        if (entity == null || !entity.labels().containsKey(sourceWiki.languageCode())
                                || entity.descriptions().containsKey(targetLang)
                                || sourceLangMustExist && !entity.descriptions().containsKey(sourceWiki.languageCode())
                                || !entity.sitelinks().containsKey(sourceWiki.dbName())
                                || !entity.sitelinks().containsKey(targetWiki.dbName())) {
                            continue
                        }
                        sourceAndTargetPageTitles.add(PageTitle(entity.sitelinks()[sourceWiki.dbName()]!!.title, sourceWiki))
                        sourceAndTargetPageTitles.add(PageTitle(entity.sitelinks()[targetWiki.dbName()]!!.title, targetWiki))

                    }
                    if (sourceAndTargetPageTitles.isEmpty()) {
                        throw ListEmptyException()
                    }
                    sourceAndTargetPageTitles
                }
                .flatMap { sourceAndTargetPageTitles: List<PageTitle> -> getSummary(sourceAndTargetPageTitles[0], sourceAndTargetPageTitles[1]) }
                .retry { t: Throwable -> t is ListEmptyException }
    }

    private fun getSummary(sourceTitle: PageTitle, targetTitle: PageTitle): Observable<Pair<RbPageSummary, RbPageSummary>> {
        val sourceCall = ServiceFactory.getRest(sourceTitle.wikiSite).getSummary(null, sourceTitle.prefixedText)
        val targetCall = ServiceFactory.getRest(targetTitle.wikiSite).getSummary(null, targetTitle.prefixedText)
        return Observable.zip(sourceCall, targetCall, BiFunction<RbPageSummary, RbPageSummary, Pair<RbPageSummary, RbPageSummary>> { source, target -> Pair(source, target) })
    }


    private class ListEmptyException : RuntimeException()
}
