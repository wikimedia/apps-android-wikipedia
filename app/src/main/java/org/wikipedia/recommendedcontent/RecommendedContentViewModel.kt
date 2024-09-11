package org.wikipedia.recommendedcontent

import android.location.Location
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.parcelable
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.util.Date

class RecommendedContentViewModel(bundle: Bundle) : ViewModel() {

    var wikiSite = bundle.parcelable<WikiSite>(Constants.ARG_WIKISITE)!!
    private val isGeneralized = bundle.getBoolean(Constants.ARG_BOOLEAN)

    private var moreLikeTerm: String? = null
    private var feedContent = mutableMapOf<String, AggregatedFeedContent>()

    private val _recentSearchesState = MutableStateFlow(Resource<List<PageTitle>>())
    val recentSearchesState = _recentSearchesState.asStateFlow()

    private val _actionState = MutableStateFlow(Resource<Pair<Int, List<PageTitle>>>())
    val actionState = _actionState.asStateFlow()

    private val _recommendedContentState = MutableStateFlow(Resource<List<PageSummary>>())
    val recommendedContentState = _recommendedContentState.asStateFlow()

    private var recommendedContentFetchJob: Job? = null

    init {
        reload(wikiSite)
    }

    fun reload(wikiSite: WikiSite) {
        this.wikiSite = wikiSite
        loadSearchHistory()
        loadRecommendedContent(isGeneralized)
    }

    fun removeRecentSearchItem(title: PageTitle, position: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _actionState.value = Resource.Error(throwable)
        }) {
            withContext(Dispatchers.IO) {
                AppDatabase.instance.recentSearchDao().deleteBy(title.displayText, Date(title.description.orEmpty().toLong()))
            }
            _actionState.value = Resource.Success(position to loadRecentSearches())
        }
    }

    fun loadSearchHistory() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _recentSearchesState.value = Resource.Error(throwable)
        }) {
            _recentSearchesState.value = Resource.Success(loadRecentSearches())
        }
    }

    private suspend fun getMoreLikeSearchTerm(): String? {
        return withContext(Dispatchers.IO) {
            // Get term from last opened article
            var term = WikipediaApp.instance.tabList.lastOrNull {
                it.backStackPositionTitle?.wikiSite == wikiSite
            }?.backStackPositionTitle?.displayText

            // Get term from last history entry if no article is opened
            if (term.isNullOrEmpty()) {
                term = AppDatabase.instance.historyEntryWithImageDao().filterHistoryItemsWithoutTime().firstOrNull {
                    it.title.wikiSite == wikiSite
                }?.apiTitle
            }

            // Ger term from Because you read if no history entry is found
            if (term.isNullOrEmpty()) {
                term = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(0, WikipediaApp.instance.resources.getInteger(
                    R.integer.article_engagement_threshold_sec)).lastOrNull {
                    it.title.wikiSite == wikiSite
                }?.title?.displayText
            }

            // Get term from last recent search if no because you read is found
            if (term.isNullOrEmpty()) {
                term = AppDatabase.instance.recentSearchDao().getRecentSearches().firstOrNull()?.text
            }

            term?.let {
                StringUtil.addUnderscores(StringUtil.removeHTMLTags(it))
            }
        }
    }

    private fun loadRecommendedContent(isGeneralized: Boolean) {
        recommendedContentFetchJob?.cancel()
        recommendedContentFetchJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _recommendedContentState.value = Resource.Error(throwable)
        }) {
            _recommendedContentState.value = Resource.Loading()
            delay(200)
            val recommendedContent = if (isGeneralized) {
                loadGeneralizedContent()
            } else {
                loadPersonalizedContent()
            }
            _recommendedContentState.value = Resource.Success(recommendedContent)
        }
    }

    private suspend fun loadRecentSearches(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            AppDatabase.instance.recentSearchDao().getRecentSearches().map {
                PageTitle(it.text, wikiSite).apply {
                    // Put timestamp in description for the delete action.
                    description = it.timestamp.time.toString()
                }
            }.take(RECENT_SEARCHES_ITEMS)
        }
    }

    private suspend fun loadFeed(): AggregatedFeedContent {
        return withContext(Dispatchers.IO) {

            feedContent[wikiSite.languageCode]?.let {
                return@withContext it
            }

            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.languageCode).isNullOrEmpty()
            val date = DateUtil.getUtcRequestDateFor(0)
            var feedContentResponse = ServiceFactory.getRest(wikiSite).getFeedFeatured(date.year, date.month, date.day)
            if (hasParentLanguageCode) {
                feedContentResponse.topRead?.let {
                    val topReadResponse = L10nUtil.getPagesForLanguageVariant(it.articles, wikiSite)
                    feedContentResponse = AggregatedFeedContent(
                        tfa = feedContentResponse.tfa,
                        news = feedContentResponse.news,
                        topRead = TopRead(it.date, topReadResponse),
                        potd = feedContentResponse.potd,
                        onthisday = feedContentResponse.onthisday
                    )
                }
            }
            // set map to feedContent
            feedContent[wikiSite.languageCode] = feedContentResponse
            feedContentResponse
        }
    }

    private suspend fun loadGeneralizedContent(): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            val content = loadFeed()
            val news = content.news?.mapNotNull { it.links.firstOrNull() } ?: emptyList()
            val topRead = content.topRead?.articles ?: emptyList()
            // Take most news items and fill the rest with top read items
            (news + topRead).distinct().take(RECOMMENDED_CONTENT_ITEMS).shuffled()
        }
    }

    private suspend fun loadPersonalizedContent(): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            val places = async { loadPlaces() }
            val moreLike = async {
                moreLikeTerm = getMoreLikeSearchTerm()
                moreLikeTerm?.let {
                    loadMoreLike(it)
                } ?: emptyList()
            }
            // Take at most 5 places and fill the rest with moreLike items
            (places.await().take(5) + moreLike.await()).distinct().take(RECOMMENDED_CONTENT_ITEMS).shuffled()
        }
    }

    private suspend fun loadMoreLike(searchTerm: String): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            val moreLikeResponse = ServiceFactory.get(wikiSite).searchMoreLike("morelike:$searchTerm", Constants.SUGGESTION_REQUEST_ITEMS, Constants.SUGGESTION_REQUEST_ITEMS)
            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.languageCode).isNullOrEmpty()

            val list = moreLikeResponse.query?.pages?.map {
                PageSummary(it.displayTitle(wikiSite.languageCode), it.title, it.description, it.extract, it.thumbUrl(), wikiSite.languageCode)
            } ?: emptyList()

            if (hasParentLanguageCode) {
                L10nUtil.getPagesForLanguageVariant(list, wikiSite)
            } else {
                list
            }
        }
    }

    private suspend fun loadPlaces(): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            Prefs.placesLastLocationAndZoomLevel?.let { pair ->
                val location = pair.first
                val response = ServiceFactory.get(wikiSite).getGeoSearch("${location.latitude}|${location.longitude}", 10000, 10, 10)
                val pages = response.query?.pages.orEmpty()
                    .filter { it.coordinates != null }
                    .map {
                        val thumbUrl = if (it.thumbUrl().isNullOrEmpty()) null else ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl()!!, PlacesFragment.THUMB_SIZE)
                        PageSummary(it.displayTitle(wikiSite.languageCode), it.title, it.description, it.extract, thumbUrl, wikiSite.languageCode).apply {
                            this.coordinates = Location("").apply {
                                latitude = it.coordinates!![0].lat
                                longitude = it.coordinates[0].lon
                            }
                        }
                    }
                pages
            } ?: emptyList()
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecommendedContentViewModel(bundle) as T
        }
    }

    companion object {
        const val RECOMMENDED_CONTENT_ITEMS = 10
        const val RECENT_SEARCHES_ITEMS = 3
    }
}
