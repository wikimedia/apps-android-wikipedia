package org.wikipedia.commons

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.dataclient.wikidata.Entities

object ImageTagsProvider {
    @JvmStatic
    fun getImageTagsObservable(pageId: Int, langCode: String): Observable<Map<String, List<String>>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getClaims("M$pageId", "P180")
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(Claims())
                .flatMap { claims ->
                    val ids = claims.claims()["P180"]?.map { it.mainSnak?.dataValue?.value }
                    if (ids.isNullOrEmpty()) {
                        Observable.just(Entities())
                    } else {
                        ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabels(ids.joinToString(separator = "|"), langCode)
                    }
                }
                .subscribeOn(Schedulers.io())
                .map { entities ->
                    val tags = HashMap<String, MutableList<String>>()
                    entities.entities?.flatMap { it.value.labels?.values!! }!!
                        .forEach { label ->
                            tags.getOrPut(label.language!!, { ArrayList() }).add(label.value!!)
                        }
                    tags
                }
    }
}
