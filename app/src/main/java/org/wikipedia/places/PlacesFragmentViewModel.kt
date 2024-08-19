package org.wikipedia.places

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource

class PlacesFragmentViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val wikiSite: WikiSite get() = WikiSite.forLanguageCode(Prefs.placesWikiCode)
    var location: Location? = savedStateHandle[PlacesActivity.EXTRA_LOCATION]
    var highlightedPageTitle: PageTitle? = savedStateHandle[Constants.ARG_TITLE]

    var lastKnownLocation: Location? = null
    val nearbyPagesLiveData = MutableLiveData<Resource<List<NearbyPage>>>()

    fun fetchNearbyPages(latitude: Double, longitude: Double, radius: Int, maxResults: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            nearbyPagesLiveData.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(wikiSite).getGeoSearch("$latitude|$longitude", radius, maxResults, maxResults)
            val pages = response.query?.pages.orEmpty()
                .filter { it.coordinates != null }
                .map {
                    NearbyPage(it.pageId, PageTitle(it.title, wikiSite,
                        if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, PlacesFragment.THUMB_SIZE),
                        it.description, it.displayTitle(wikiSite.languageCode)), it.coordinates!![0].lat, it.coordinates[0].lon)
                }
                .sortedBy {
                    lastKnownLocation?.run {
                        it.location.distanceTo(this)
                    }
                }
            nearbyPagesLiveData.postValue(Resource.Success(pages))
        }
    }
}
