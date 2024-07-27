package org.wikipedia.feed.places

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
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
                val location = it.first
                val response = ServiceFactory.get(wiki).getGeoSearch("${location.latitude}|${location.longitude}", 10000, 10, 10)
                val lastPage = response.query?.pages.orEmpty()
                    .filter { it.coordinates != null }
                    .map {
                        NearbyPage(it.pageId, PageTitle(it.title, wiki,
                            if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, PlacesFragment.THUMB_SIZE),
                            it.description, it.displayTitle(wiki.languageCode)), it.coordinates!![0].lat, it.coordinates[0].lon)
                    }[age % 10]
                cb.success(listOf(PlacesCard(wiki, age, lastPage)))
            }
        } ?: run {
            cb.success(listOf(PlacesCard(wiki, age)))
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
