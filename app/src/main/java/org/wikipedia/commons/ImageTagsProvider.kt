package org.wikipedia.commons

import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.language.LanguageUtil

object ImageTagsProvider {
    suspend fun getImageTags(pageId: Int, langCode: String): Map<String, List<String>> {
        try {
            val claims = ServiceFactory.get(Constants.commonsWikiSite).getClaimsSuspend("M$pageId", "P180")
            val ids = getDepictsClaims(claims.claims)
            return if (ids.isEmpty()) {
                emptyMap()
            } else {
                val response = ServiceFactory.get(Constants.wikidataWikiSite).getWikidataEntityTerms(ids.joinToString(separator = "|"),
                    LanguageUtil.convertToUselangIfNeeded(langCode))
                val labelList = response.query?.pages?.mapNotNull {
                    it.entityTerms?.label?.firstOrNull()
                }
                if (labelList.isNullOrEmpty()) emptyMap() else mapOf(langCode to labelList)
            }
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    fun getDepictsClaims(claims: Map<String, List<Claims.Claim>>): List<String> {
        return claims["P180"]?.mapNotNull { it.mainSnak?.dataValue?.value() }.orEmpty()
    }
}
