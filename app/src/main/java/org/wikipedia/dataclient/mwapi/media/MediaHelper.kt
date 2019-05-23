package org.wikipedia.dataclient.mwapi.media

import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite

import java.util.HashMap

import io.reactivex.Observable

object MediaHelper {
    private const val COMMONS_DB_NAME = "commonswiki"

    /**
     * Returns a map of "language":"caption" combinations for a particular file on Commons.
     */
    fun getImageCaptions(fileName: String): Observable<Map<String, String>> {
        return ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getEntitiesByTitle(fileName, COMMONS_DB_NAME)
                .map { entities ->
                    val captions = HashMap<String, String>()
                    for (label in entities.first!!.labels().values) {
                        captions[label.language()] = label.value()
                    }
                    captions
                }
    }

}
