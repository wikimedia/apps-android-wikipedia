package org.wikipedia.places

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource

enum class SortMode(val menuId: Int) {
    POPULARITY(R.id.menu_places_sort_popularity),
    DISTANCE(R.id.menu_places_sort_distance)
}

internal fun sortNearbyPages(
    pages: List<NearbyPage>,
    sortMode: SortMode,
    lastKnownLocation: Location?
): List<NearbyPage> = when (sortMode) {
    SortMode.DISTANCE -> pages.sortedBy {
        lastKnownLocation?.run { it.location.distanceTo(this) }
    }
    SortMode.POPULARITY -> pages.sortedByDescending { it.pageViews }
}

class PlacesFragmentViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val wikiSite: WikiSite get() = WikiSite.forLanguageCode(Prefs.placesWikiCode)
    var location: Location? = savedStateHandle[PlacesActivity.EXTRA_LOCATION]
    var highlightedPageTitle: PageTitle? = savedStateHandle[Constants.ARG_TITLE]

    var lastKnownLocation: Location? = null
    // Default to popularity: easier to discover interesting nearby places.
    var sortMode: SortMode
        get() = SortMode.entries.firstOrNull { it.name == Prefs.placesSortMode } ?: SortMode.POPULARITY
        set(value) { Prefs.placesSortMode = value.name }
    val nearbyPagesLiveData = MutableLiveData<Resource<List<NearbyPage>>>()

    private var lastResults: List<NearbyPage> = emptyList()

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
                        it.description, it.displayTitle(wikiSite.languageCode)), it.coordinates!![0].lat, it.coordinates[0].lon,
                        // pageViewsMap is a date→count map; sum gives the N-day total.
                        it.pageViewsMap.values.filterNotNull().sum())
                }
            lastResults = pages
            applySort()
        }
    }

    fun applySort() {
        val sorted = sortNearbyPages(lastResults, sortMode, lastKnownLocation)
        nearbyPagesLiveData.postValue(Resource.Success(sorted))
    }
}
