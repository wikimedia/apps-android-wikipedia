package org.wikipedia.commons

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import java.util.*

object ImageTagsProvider {
    fun getImageTagsObservable(pageId: String, langCode: String): Observable<Map<String, List<String>>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getClaims("M$pageId")
                .subscribeOn(Schedulers.io())
                .flatMap { claims ->
                    val depicts = claims.claims()["P180"]
                    val ids = mutableListOf<String?>()
                    depicts?.forEach {
                        ids.add(it.mainSnak?.dataValue?.value?.id)
                    }
                    ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabels(ids.joinToString(separator = "|"), langCode)
                }
                .subscribeOn(Schedulers.io())
                .map { entities ->
                    val tags = HashMap<String, MutableList<String>>()
                    entities.entities().forEach {
                        it.value.labels().values.forEach { label ->
                            if (tags[label.language()].isNullOrEmpty()) {
                                tags[label.language()] = mutableListOf(label.value())
                            } else {
                                tags[label.language()]!!.add(label.value())
                            }
                        }
                    }
                    tags
                }
    }
}

