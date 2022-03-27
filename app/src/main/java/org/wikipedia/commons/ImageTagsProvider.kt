package org.wikipedia.commons

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.wikidata.Claims

object ImageTagsProvider {
    fun getImageTagsObservable(pageId: Int, langCode: String): Observable<Map<String, List<String>>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getClaims("M$pageId", "P180")
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(Claims())
                .flatMap { claims ->
                    val ids = claims.claims["P180"]?.map { it.mainSnak?.dataValue?.value() }
                    if (ids.isNullOrEmpty()) {
                        Observable.just(MwQueryResponse())
                    } else {
                        ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataEntityTerms(ids.joinToString(separator = "|"), langCode)
                    }
                }
                .subscribeOn(Schedulers.io())
                .map { response ->
                    val labelList = response.query?.pages?.mapNotNull {
                        it.entityTerms?.label?.firstOrNull()
                    }
                    if (labelList.isNullOrEmpty()) emptyMap() else mapOf(langCode to labelList)
                }
    }
}
