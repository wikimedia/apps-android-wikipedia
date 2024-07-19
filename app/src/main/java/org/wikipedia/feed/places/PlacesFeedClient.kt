package org.wikipedia.feed.places

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil

class PlacesFeedClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private lateinit var cb: FeedClient.Callback
    private var age: Int = 0
    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        this.cb = cb

        Prefs.placesLastLocationAndZoomLevel?.let {
            coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
                cb.error(throwable)
            }) {
                val list = mutableListOf<PlacesCard>()
                FeedContentType.aggregatedLanguages.forEach { lang ->
                    val wikiSite = WikiSite.forLanguageCode(lang)
                    val location = it.first
                    // TODO: tune the radius and decide which markers to pick
                    val response = ServiceFactory.get(wikiSite).getGeoSearch("${location.latitude}|${location.longitude}", 10000, age, age)
                    val lastPage = response.query?.pages.orEmpty()
                        .filter { it.coordinates != null }
                        .map {
                            NearbyPage(it.pageId, PageTitle(it.title, wikiSite,
                                if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, PlacesFragment.THUMB_SIZE),
                                it.description, it.displayTitle(wikiSite.languageCode)), it.coordinates!![0].lat, it.coordinates[0].lon)
                        }.first()
                    list.add(PlacesCard(wikiSite, age, lastPage))
                }
                cb.success(list)
            }
        } ?: run {
            cb.success(listOf(PlacesCard(wiki, age)))
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
