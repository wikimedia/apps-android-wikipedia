package org.wikipedia.recommendedcontent

import android.location.Location
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesFragment
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.util.Date

class RecommendedContentViewModel(bundle: Bundle) : ViewModel() {

    val wikiSite = WikipediaApp.instance.wikiSite
    val inHistory = bundle.getBoolean(RecommendedContentFragment.ARG_IN_HISTORY)
    private val sectionIds: List<Int> = bundle.getIntegerArrayList(RecommendedContentFragment.ARG_SECTION_IDS)!!
    val sections = sectionIds.map { RecommendedContentSection.find(it) }

    private var exploreTerm: String? = null
    private var feedContent: AggregatedFeedContent? = null

    private val _historyState = MutableStateFlow(Resource<List<PageTitle>>())
    val historyState = _historyState.asStateFlow()

    private val _actionState = MutableStateFlow(Resource<Pair<Int, List<PageTitle>>>())
    val actionState = _actionState.asStateFlow()

    private val _recommendedContentState = MutableStateFlow(Resource<List<PageSummary>>())
    val recommendedContentState = _recommendedContentState.asStateFlow()

    init {
        loadSearchHistory()
        loadRecommendedContent(sections)
    }

    fun removeHistoryItem(title: PageTitle, position: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _actionState.value = Resource.Error(throwable)
        }) {
            val entry = HistoryEntry(title, HistoryEntry.SOURCE_RECOMMENDED_CONTENT)
            AppDatabase.instance.historyEntryDao().delete(entry)
            _actionState.value = Resource.Success(position to loadHistoryItems())
        }
    }

    fun removeRecentSearchItem(title: PageTitle, position: Int) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _actionState.value = Resource.Error(throwable)
        }) {
            val recentSearch = RecentSearch(title.displayText, Date(title.description.orEmpty().toLong()))
            AppDatabase.instance.recentSearchDao().delete(recentSearch)
            _actionState.value = Resource.Success(position to loadRecentSearches())
        }
    }

    fun loadSearchHistory() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _historyState.value = Resource.Error(throwable)
        }) {
            if (inHistory) {
                _historyState.value = Resource.Success(loadHistoryItems())
            } else {
                _historyState.value = Resource.Success(loadRecentSearches())
            }
        }
    }

    private suspend fun getExploreSearchTerm(): String {
        return withContext(Dispatchers.IO) {
            // Get term from last opened article
            var term = WikipediaApp.instance.tabList.lastOrNull()?.backStackPositionTitle?.displayText ?: ""

            // Get term from last history entry if no article is opened
            if (term.isEmpty()) {
                term = AppDatabase.instance.historyEntryWithImageDao().findEntriesBySearchTerm("").firstOrNull()?.apiTitle ?: ""
            }

            // Get term from last recent search if no history entry is found
            if (term.isEmpty()) {
                term = AppDatabase.instance.recentSearchDao().getRecentSearches().firstOrNull()?.text ?: ""
            }

            // Ger term from Because you read if no recent search is found
            if (term.isEmpty()) {
                term = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(0, WikipediaApp.instance.resources.getInteger(
                    R.integer.article_engagement_threshold_sec)).lastOrNull()?.title?.displayText ?: ""
            }

            StringUtil.addUnderscores(StringUtil.removeHTMLTags(term))
        }
    }

    private fun loadRecommendedContent(sections: List<RecommendedContentSection>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _recommendedContentState.value = Resource.Error(throwable)
        }) {
            _recommendedContentState.value = Resource.Loading()
            val recommendedContent = mutableListOf<Deferred<List<PageSummary>>>()
            sections.forEach { section ->
                val content = when (section) {
                    RecommendedContentSection.TOP_READ -> async { loadTopRead() }
                    RecommendedContentSection.EXPLORE -> async {
                        exploreTerm = getExploreSearchTerm()
                        exploreTerm?.let {
                            loadExplore(it)
                        } ?: emptyList()
                    }
                    RecommendedContentSection.ON_THIS_DAY -> async { loadOnThisDay() }
                    RecommendedContentSection.IN_THE_NEWS -> async { loadInTheNews() }
                    RecommendedContentSection.PLACES_NEAR_YOU -> async { loadPlaces() }
                    RecommendedContentSection.BECAUSE_YOU_READ -> async { loadBecauseYouRead(0) }
                    RecommendedContentSection.CONTINUE_READING -> async { loadContinueReading() }
                }
                recommendedContent.add(content)
            }

            // merge into one list and shuffle
            val contentList = recommendedContent.map { it.await() }.flatten().shuffled()

            _recommendedContentState.value = Resource.Success(contentList)
        }
    }

    private suspend fun loadHistoryItems(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            AppDatabase.instance.historyEntryWithImageDao().filterHistoryItemsWithoutTime("").map {
                it.title
            }.take(3)
        }
    }

    private suspend fun loadRecentSearches(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            AppDatabase.instance.recentSearchDao().getRecentSearches().map {
                PageTitle(it.text, WikipediaApp.instance.wikiSite).apply {
                    // Put timestamp in description for the delete action.
                    description = it.timestamp.time.toString()
                }
            }.take(3)
        }
    }

    private suspend fun loadFeed(): AggregatedFeedContent {
        return withContext(Dispatchers.IO) {

            feedContent?.let {
                return@withContext it
            }

            val wikiSite = WikipediaApp.instance.wikiSite
            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.languageCode).isNullOrEmpty()
            val date = DateUtil.getUtcRequestDateFor(0)
            var feedContentResponse = ServiceFactory.getRest(wikiSite).getFeedFeatured(date.year, date.month, date.day)
            if (hasParentLanguageCode) {
                feedContentResponse.topRead?.let {
                    val topReadResponse = getPagesForLanguageVariant(it.articles, wikiSite)
                    feedContentResponse = AggregatedFeedContent(
                        tfa = feedContentResponse.tfa,
                        news = feedContentResponse.news,
                        topRead = TopRead(it.date, topReadResponse),
                        potd = feedContentResponse.potd,
                        onthisday = feedContentResponse.onthisday
                    )
                }
            }
            feedContent = feedContentResponse
            feedContentResponse
        }
    }

    private suspend fun loadTopRead(): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            loadFeed().topRead?.articles?.take(5) ?: emptyList()
        }
    }

    private suspend fun loadOnThisDay(): List<PageSummary> {
        // TODO: determine which event to load.
        return withContext(Dispatchers.IO) {
            loadFeed().onthisday?.first()?.pages() ?: emptyList()
        }
    }

    private suspend fun loadInTheNews(): List<PageSummary> {
        // TODO: determine which news to load.
        return withContext(Dispatchers.IO) {
            loadFeed().news?.first()?.links?.filterNotNull() ?: emptyList()
        }
    }

    private suspend fun loadExplore(searchTerm: String): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            val wikiSite = WikipediaApp.instance.wikiSite
            val moreLikeResponse = ServiceFactory.get(wikiSite).searchMoreLike("morelike:$searchTerm", Constants.SUGGESTION_REQUEST_ITEMS, Constants.SUGGESTION_REQUEST_ITEMS)
            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.languageCode).isNullOrEmpty()

            val list = moreLikeResponse.query?.pages?.map {
                PageSummary(it.displayTitle(wikiSite.languageCode), it.title, it.description, it.extract, it.thumbUrl(), wikiSite.languageCode)
            }?.take(5) ?: emptyList()

            if (hasParentLanguageCode) {
                getPagesForLanguageVariant(list, wikiSite)
            } else {
                list
            }
        }
    }

    private suspend fun loadBecauseYouRead(age: Int): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            val entry = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age, WikipediaApp.instance.resources.getInteger(
                R.integer.article_engagement_threshold_sec)).lastOrNull()
            if (entry == null) {
                emptyList()
            } else {
                loadExplore(entry.title.prefixedText)
            }
        }
    }

    private suspend fun loadContinueReading(): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            WikipediaApp.instance.tabList.mapNotNull { tab ->
                tab.backStackPositionTitle?.let {
                    PageSummary(it.displayText, it.prefixedText, it.description, null, it.thumbUrl, it.wikiSite.languageCode)
                }
            }.take(5)
        }
    }

    private suspend fun loadPlaces(): List<PageSummary> {
        val wikiSite = WikipediaApp.instance.wikiSite
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
            }?.take(5) ?: emptyList()
        }
    }

    // TODO: borrowed from FeedClient. Refactor this method to be more generic.
    private suspend fun getPagesForLanguageVariant(list: List<PageSummary>, wikiSite: WikiSite): List<PageSummary> {
        val newList = mutableListOf<PageSummary>()
        withContext(Dispatchers.IO) {
            val titles = list.joinToString(separator = "|") { it.apiTitle }
            // First, get the correct description from Wikidata directly.
            val wikiDataResponse = async {
                ServiceFactory.get(Constants.wikidataWikiSite)
                    .getWikidataDescription(titles = titles, sites = wikiSite.dbName(), langCode = wikiSite.languageCode)
            }
            // Second, fetch varianttitles from prop=info endpoint.
            val mwQueryResponse = async {
                ServiceFactory.get(wikiSite).getVariantTitlesByTitles(titles)
            }

            list.forEach { pageSummary ->
                // Find the correct display title from the varianttitles map, and insert the new page summary to the list.
                val displayTitle = mwQueryResponse.await().query?.pages?.find { StringUtil.addUnderscores(it.title) == pageSummary.apiTitle }?.varianttitles?.get(wikiSite.languageCode)
                val newPageSummary = pageSummary.apply {
                    val newDisplayTitle = displayTitle ?: pageSummary.displayTitle
                    this.titles = PageSummary.Titles(
                        canonical = pageSummary.apiTitle,
                        display = newDisplayTitle
                    )
                    this.description = wikiDataResponse.await().entities.values.firstOrNull {
                        it.labels[wikiSite.languageCode]?.value == newDisplayTitle
                    }?.descriptions?.get(wikiSite.languageCode)?.value ?: pageSummary.description
                }
                newList.add(newPageSummary)
            }
        }
        return newList
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecommendedContentViewModel(bundle) as T
        }
    }
}
