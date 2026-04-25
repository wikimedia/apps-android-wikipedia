package org.wikipedia.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.util.Collections

enum class HomeTab { COMMUNITY, FOR_YOU }

data class DayContent(
    val age: Int,
    val date: LocalDate,
    val featuredArticle: Pair<String, PageSummary?>,
    val news: Pair<String, List<NewsItem>?>,
    val topRead: Pair<String, TopRead?>,
    val featuredImage: Pair<String, FeaturedImage?>,
    val onThisDay: Pair<String, List<OnThisDay.Event>?>
)

sealed class ForYouModule {
    abstract val pages: List<PageSummary>

    data class BasedOnReadingHistory(override val pages: List<PageSummary>) : ForYouModule()
    data class BasedOnLocation(override val pages: List<PageSummary>) : ForYouModule()
    data class BasedOnInterests(val interest: String, override val pages: List<PageSummary>) : ForYouModule()
}

data class CommunityContentState(
    val days: List<DayContent> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = true
)

data class ForYouContentState(
    val modules: List<ForYouModule> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = true
)

class HomeViewModel : ViewModel() {
    private val _wikiSite = MutableStateFlow(WikiSite.forLanguageCode(Prefs.homeLanguageCode))
    val wikiSite = _wikiSite.asStateFlow()

    private val _selectedTab = MutableStateFlow(
        if (Prefs.homePreferenceSelection == HomePreferenceType.PERSONALIZED) HomeTab.FOR_YOU else HomeTab.COMMUNITY
    )
    val selectedTab = _selectedTab.asStateFlow()

    private val _communityState = MutableStateFlow(CommunityContentState())
    val communityState = _communityState.asStateFlow()

    private val _forYouState = MutableStateFlow(ForYouContentState())
    val forYouState = _forYouState.asStateFlow()

    // "age" in days from today. 0 = today, 1 = yesterday, etc.
    private var nextCommunityAge = 0

    // Batch counter for "For you" recommendations.
    private var forYouBatchIndex = 0

    private val communityHandler = CoroutineExceptionHandler { _, throwable ->
        _communityState.value = _communityState.value.copy(
            isInitialLoading = false,
            isLoadingMore = false,
            error = throwable
        )
    }

    private val forYouHandler = CoroutineExceptionHandler { _, throwable ->
        _forYouState.value = _forYouState.value.copy(
            isInitialLoading = false,
            isLoadingMore = false,
            error = throwable
        )
    }

    private val hiddenModules =
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
            public override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>): Boolean {
                return size > MAX_HIDDEN_CARDS
            }
        })

    init {
        if (_selectedTab.value == HomeTab.COMMUNITY) {
            loadCommunityContent()
        } else {
            loadForYouContent()
        }
        updateHiddenModules()
    }

    fun updateHiddenModules() {
        hiddenModules.clear()
        hiddenModules.addAll(Prefs.hiddenCards)
    }

    fun refreshCommunityContent() {
        nextCommunityAge = 0
        _communityState.update { CommunityContentState() }
        loadCommunityContent()
        updateHiddenModules()
    }

    fun refreshForYouContent() {
        forYouBatchIndex = 0
        _forYouState.update { ForYouContentState() }
        loadForYouContent()
        updateHiddenModules()
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
        if (tab == HomeTab.FOR_YOU &&
            _forYouState.value.modules.isEmpty() &&
            !_forYouState.value.isInitialLoading
        ) {
            loadForYouContent()
        }
    }

    fun updateLanguage(langCode: String) {
        _wikiSite.value = WikiSite.forLanguageCode(langCode)
        Prefs.homeLanguageCode = langCode
        if (selectedTab.value == HomeTab.COMMUNITY) {
            refreshCommunityContent()
        } else {
            refreshForYouContent()
        }
    }

    fun hideModule(moduleKey: String) {
        hiddenModules.add(moduleKey)
        Prefs.hiddenCards = hiddenModules
        // TODO: optimize by only refreshing the affected module instead of the whole tab.
        if (selectedTab.value == HomeTab.COMMUNITY) {
            refreshCommunityContent()
        } else {
            refreshForYouContent()
        }
    }

    fun unhideModule(moduleKey: String) {
        hiddenModules.remove(moduleKey)
        Prefs.hiddenCards = hiddenModules
        // TODO: optimize by only refreshing the affected module instead of the whole tab.
        if (selectedTab.value == HomeTab.COMMUNITY) {
            refreshCommunityContent()
        } else {
            refreshForYouContent()
        }
    }

    /**
     * Loads the next day's community content (today on first call, then progressively older).
     * Safe to call as a retry — the age only advances after a successful fetch.
     */
    fun loadCommunityContent() {
        if (_communityState.value.isInitialLoading || _communityState.value.isLoadingMore) return

        viewModelScope.launch(communityHandler) {
            val isInitial = _communityState.value.days.isEmpty()
            _communityState.value = _communityState.value.copy(
                isInitialLoading = isInitial,
                isLoadingMore = !isInitial,
                error = null
            )

            val age = nextCommunityAge
            val date = LocalDate.now().minusDays(nextCommunityAge.toLong())
            val content = ServiceFactory.getRest(wikiSite.value)
                .getFeedFeatured(date.year.toString(), "%02d".format(date.monthValue), "%02d".format(date.dayOfMonth), wikiSite.value.languageCode)

            val dayContent = DayContent(
                age = age,
                date = date,
                featuredArticle = "tfa-$age" to content.tfa,
                news = "news-$age" to content.news,
                topRead = "top-read-$age" to content.topRead,
                featuredImage = "tfi-$age" to content.potd,
                onThisDay = "otd-$age" to content.onthisday
            )

            // Advance age only after success, so retry on failure re-fetches the same day.
            nextCommunityAge = age + 1

            _communityState.value = _communityState.value.copy(
                days = _communityState.value.days + dayContent,
                isInitialLoading = false,
                isLoadingMore = false,
                error = null,
                canLoadMore = true
            )
        }
    }

    /**
     * Loads the next batch of personalized recommendations for the "For you" tab.
     * Safe to call as a retry — the batch index only advances after a successful fetch.
     */
    fun loadForYouContent() {
        if (_forYouState.value.isInitialLoading || _forYouState.value.isLoadingMore) return

        viewModelScope.launch(forYouHandler) {
            val isInitial = _forYouState.value.modules.isEmpty()
            _forYouState.value = _forYouState.value.copy(
                isInitialLoading = isInitial,
                isLoadingMore = !isInitial,
                error = null
            )

            val newModules = fetchForYouModules(forYouBatchIndex)

            // Advance batch index only after success.
            forYouBatchIndex++

            _forYouState.value = _forYouState.value.copy(
                modules = _forYouState.value.modules + newModules,
                isInitialLoading = false,
                isLoadingMore = false,
                error = null,
                canLoadMore = newModules.isNotEmpty()
            )
        }
    }

    private suspend fun fetchForYouModules(batchIndex: Int): List<ForYouModule> {

        val sampleImageUrls = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg/1280px-SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/10/Color_of_Friendship.jpg/1280px-Color_of_Friendship.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg/1280px-MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg/1280px-Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg/1280px-Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg",
        )

        val modules = sampleImageUrls.map {
            ForYouModule.BasedOnInterests("Art", listOf(PageSummary("", "", "", "", thumbnail = it, "")))
        }
        return modules
    }

    companion object {
        private const val MAX_HIDDEN_CARDS = 100
    }
}
