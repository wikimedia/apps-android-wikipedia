package org.wikipedia.places

import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry

class PlacesFragmentViewModel(bundle: Bundle) : ViewModel() {

    val wikiSite: WikiSite get() = WikiSite.forLanguageCode(Prefs.placesWikiCode)
    var location: Location? = bundle.parcelable(PlacesActivity.EXTRA_LOCATION)
    var pageTitle: PageTitle? = bundle.parcelable(Constants.ARG_TITLE)

    var watchlistExpiryChanged = false
    var isWatched = false
    var hasWatchlistExpiry = false
    var lastWatchExpiry = WatchlistExpiry.NEVER
    var currentMarkerPageTitle: PageTitle? = null

    val watchStatus = MutableLiveData<Resource<Boolean>>()
    val nearbyPages = MutableLiveData<Resource<List<NearbyPage>>>()

    fun fetchNearbyPages(latitude: Double, longitude: Double, radius: Int, maxResults: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            nearbyPages.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(wikiSite).getGeoSearch("$latitude|$longitude", radius, maxResults, maxResults)
            val pages = response.query?.pages.orEmpty()
                .filter { it.coordinates != null }
                .map {
                    NearbyPage(it.pageId, PageTitle(it.title, wikiSite,
                        if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, PlacesFragment.THUMB_SIZE),
                        it.description,
                        it.displayTitle(wikiSite.languageCode)),
                        it.coordinates!![0].lat, it.coordinates[0].lon)
                }

            nearbyPages.postValue(Resource.Success(pages))
        }
    }

    fun watchOrUnwatch(expiry: WatchlistExpiry, unwatch: Boolean) {
        currentMarkerPageTitle?.let { title ->
            if (isWatched) {
                WatchlistAnalyticsHelper.logRemovedFromWatchlist(title)
            } else {
                WatchlistAnalyticsHelper.logAddedToWatchlist(title)
            }
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.w("Failed to fetch watch status.", throwable)
            }) {
                val token = ServiceFactory.get(title.wikiSite).getWatchToken().query?.watchToken()
                val response = ServiceFactory.get(title.wikiSite)
                    .watch(if (unwatch) 1 else null, null, title.prefixedText, expiry.expiry, token!!)

                lastWatchExpiry = expiry
                if (watchlistExpiryChanged && unwatch) {
                    watchlistExpiryChanged = false
                }
                if (unwatch) {
                    WatchlistAnalyticsHelper.logRemovedFromWatchlistSuccess(title)
                } else {
                    WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(title)
                }
                response.getFirst()?.let {
                    isWatched = it.watched
                    hasWatchlistExpiry = lastWatchExpiry != WatchlistExpiry.NEVER
                    watchStatus.postValue(Resource.Success(isWatched))
                }
            }
        }
    }

    class NearbyPage(
        val pageId: Int,
        val pageTitle: PageTitle,
        val latitude: Double,
        val longitude: Double,
        var annotation: Symbol? = null,
        var bitmap: Bitmap? = null
    )

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlacesFragmentViewModel(bundle) as T
        }
    }
}
