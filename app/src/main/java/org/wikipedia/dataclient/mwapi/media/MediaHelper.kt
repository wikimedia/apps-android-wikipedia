package org.wikipedia.dataclient.mwapi.media

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite

object MediaHelper {
    private const val COMMONS_DB_NAME = "commonswiki"

    /**
     * Returns a map of "language":"caption" combinations for a particular file on Commons.
     */
    fun getImageCaptions(fileName: String): Observable<Map<String, String>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getEntitiesByTitle(fileName, COMMONS_DB_NAME)
                .map { entities ->
                    entities.first?.labels?.values?.associate { it.language to it.value }.orEmpty()
                }
    }
}
