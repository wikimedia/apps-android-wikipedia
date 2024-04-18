package org.wikipedia.commons

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.language.LanguageUtil

object ImageTagsProvider {
    fun getImageTagsObservable(pageId: Int, langCode: String): Observable<Map<String, List<String>>> {
        return ServiceFactory.get(Constants.commonsWikiSite).getClaims("M$pageId", "P180")
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(Claims())
                .flatMap { claims ->
                    val ids = getDepictsClaims(claims.claims)
                    if (ids.isNullOrEmpty()) {
                        Observable.just(MwQueryResponse())
                    } else {
                        ServiceFactory.get(Constants.wikidataWikiSite).getWikidataEntityTerms(ids.joinToString(separator = "|"), LanguageUtil.convertToUselangIfNeeded(langCode))
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

    suspend fun getImageTags(pageId: Int, langCode: String): Map<String, List<String>> {
        val claims = ServiceFactory.get(Constants.commonsWikiSite).getClaimsSuspend("M$pageId", "P180")
        val ids = getDepictsClaims(claims.claims)
        return if (ids.isEmpty()) {
            emptyMap()
        } else {
            val response = ServiceFactory.get(Constants.wikidataWikiSite)
                .getWikidataEntityTermsSuspend(ids.joinToString(separator = "|"),LanguageUtil.convertToUselangIfNeeded(langCode))
            val labelList = response.query?.pages?.mapNotNull {
                it.entityTerms?.label?.firstOrNull()
            }
            if (labelList.isNullOrEmpty()) emptyMap() else mapOf(langCode to labelList)
        }
    }

    fun getDepictsClaims(claims: Map<String, List<Claims.Claim>>): List<String> {
        return claims["P180"]?.mapNotNull { it.mainSnak?.dataValue?.value() }.orEmpty()
    }
}
