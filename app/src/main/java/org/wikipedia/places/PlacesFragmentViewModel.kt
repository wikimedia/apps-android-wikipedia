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
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource

class PlacesFragmentViewModel(bundle: Bundle) : ViewModel() {

    val wikiSite: WikiSite get() = WikiSite.forLanguageCode(Prefs.placesWikiCode)
    var location: Location? = bundle.parcelable(PlacesActivity.EXTRA_LOCATION)
    var highlightedPageTitle: PageTitle? = bundle.parcelable(Constants.ARG_TITLE)

    var lastViewportLocation: Location? = null
    var lastViewportLocationQueried: Location? = null
    var lastViewportZoom = 15.0
    var lastViewportZoomQueried = 0.0
    var lastKnownUserLocation: Location? = null

    val nearbyPagesLiveData = MutableLiveData<Resource<List<NearbyPage>>>()

    fun fetchNearbyPages(latitude: Double, longitude: Double, radius: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            nearbyPagesLiveData.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(wikiSite).getGeoSearch("$latitude|$longitude", radius, ITEMS_PER_REQUEST, ITEMS_PER_REQUEST)
            val pages = response.query?.pages.orEmpty()
                .filter { it.coordinates != null }
                .map {
                    NearbyPage(it.pageId, PageTitle(it.title, wikiSite,
                        if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, THUMB_SIZE),
                        it.description, it.displayTitle(wikiSite.languageCode)), it.coordinates!![0].lat, it.coordinates[0].lon)
                }
            nearbyPagesLiveData.postValue(Resource.Success(pages))
        }
    }

    class NearbyPage(
        val pageId: Int,
        val pageTitle: PageTitle,
        val latitude: Double,
        val longitude: Double,
        var annotation: Symbol? = null,
        var bitmap: Bitmap? = null
    ) {

        private val lat = latitude
        private val lng = longitude
        val location get() = Location("").apply {
            latitude = lat
            longitude = lng
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlacesFragmentViewModel(bundle) as T
        }
    }

    companion object {
        const val ITEMS_PER_REQUEST = 75
        const val THUMB_SIZE = 160
    }
}
