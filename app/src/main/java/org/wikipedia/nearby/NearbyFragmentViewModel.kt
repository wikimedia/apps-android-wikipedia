package org.wikipedia.nearby

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class NearbyFragmentViewModel(bundle: Bundle) : ViewModel() {

    var wikiSite: WikiSite = bundle.getParcelable(NearbyActivity.EXTRA_WIKI)!!

    val nearbyPages = MutableLiveData<Resource<List<NearbyPage>>>()

    fun fetchNearbyPages(latitude: Double, longitude: Double) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            nearbyPages.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(wikiSite).getGeoSearch("$latitude|$longitude", 50, 50)
            val pages = response.query?.pages.orEmpty()
                .filter { it.coordinates != null }
                .map {
                    NearbyPage(it.pageId, PageTitle(it.title, wikiSite, it.thumbUrl(), it.description, it.displayTitle(wikiSite.languageCode)),
                        it.coordinates!![0].lat, it.coordinates[0].lon)
                }

            nearbyPages.postValue(Resource.Success(pages))
        }
    }

    class NearbyPage(
        val pageId: Int,
        val pageTitle: PageTitle,
        val latitude: Double,
        val longitude: Double,
        var annotation: Symbol? = null
    )

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NearbyFragmentViewModel(bundle) as T
        }
    }
}
