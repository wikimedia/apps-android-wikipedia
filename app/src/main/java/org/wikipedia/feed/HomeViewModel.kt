package org.wikipedia.feed

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.ui.util.fastJoinToString
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.NotificationBellState
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.events.NewRecommendedReadingListEvent
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.didyouknow.DidYouKnowCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.BasedOnInterestCard
import org.wikipedia.feed.model.BecauseYouReadCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ContinueReadingCard
import org.wikipedia.feed.model.DiscoverCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.PlacesOfInterestCard
import org.wikipedia.feed.model.RandomCard
import org.wikipedia.feed.model.SeeAllRecommendationCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopReadCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.json.LocalDateTimeSerializer
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsRepository
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.settings.homefeed.ForYouModuleType
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

enum class HomeTab { COMMUNITY, FOR_YOU }
private const val MAX_STOP_TIMEOUT_MILLIS = 5000L
private const val MAX_DISCOVER_ARTICLE_CARDS = 4
private const val PLACES_ARTICLES_REQUEST_LIMIT = 10
private const val PLACES_SEARCH_RADIUS_METERS = 10000

@Serializable
sealed class ForYouModule {
    abstract val age: Int
    abstract val index: Int
    abstract val cards: List<ForYouCard>

    abstract fun withCards(cards: List<ForYouCard>): ForYouModule
    abstract fun moduleKey(): String

    @Serializable
    data class BasedOnInterest(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.BASED_ON_INTEREST.name
    }

    @Serializable
    data class ContinueReading(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.CONTINUE_READING.name
    }

    @Serializable
    data class BecauseYouRead(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.BECAUSE_YOU_READ.name
    }

    @Serializable
    data class PlacesOfInterest(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>,
        val hasLocationPermission: Boolean,
        val isLoading: Boolean = false
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.PLACES_OF_INTEREST.name
    }

    @Serializable
    data class Discover(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>,
        val isEnabled: Boolean,
        val isLoading: Boolean = false,
        val updateFrequency: RecommendedReadingListUpdateFrequency = RecommendedReadingListUpdateFrequency.DAILY
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.DISCOVER.name
    }

    @Serializable
    data class Random(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.RANDOM.name
    }
}

@Serializable
class ForYouCollectionSaved(
    @Serializable(with = LocalDateTimeSerializer::class) val dateTime: LocalDateTime? = null,
    val modulesPerLanguage: Map<String, List<ForYouModule>> = emptyMap()
)

data class CommunityContentState(
    val cards: List<Card> = emptyList(),
    val wikiSite: WikiSite = WikiSite.forLanguageCode(Prefs.homeLanguageCode),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = true,
    val emptyState: FeedEmptyState? = null
)

data class ForYouContentState(
    val modules: List<ForYouModule> = emptyList(),
    val wikiSite: WikiSite = WikiSite.forLanguageCode(Prefs.homeLanguageCode),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = false,
    val isInterestModuleHidden: Boolean = false,
    val emptyState: FeedEmptyState? = null
)

enum class FeedEmptyState { ALL_MODULES_HIDDEN, NO_DATA }

data class TabsState(val count: Int, val pulse: Boolean)

val noImageCardBackgroundColors = listOf(R.color.maroon800, R.color.purple800, R.color.pink800)
val noImageCardForegroundColors = listOf(R.color.maroon300, R.color.purple300, R.color.pink300)

class HomeViewModel : ViewModel() {
    private val _wikiSite = MutableStateFlow(WikiSite.forLanguageCode(Prefs.homeLanguageCode))
    val wikiSite = _wikiSite.asStateFlow()

    private val _selectedTab = MutableStateFlow(
        if (Prefs.homePreferenceSelection == HomePreferenceType.PERSONALIZED) HomeTab.FOR_YOU else HomeTab.COMMUNITY
    )
    val selectedTab = _selectedTab.asStateFlow()

    private val _communityState = MutableStateFlow(CommunityContentState())
    val communityState = combine(
        _communityState,
        SettingsRepository.hiddenModules,
        SettingsRepository.hiddenCards
    ) { state, hiddenModules, hiddenCards ->
        val visibleItems = state.cards
            .filterNot { hiddenModules.contains(it.moduleKey()) }
            .filterNot { hiddenCards.contains(it.hideKey) }
        val hasContent = visibleItems.any { it !is DayHeaderCard }
        val areAllModulesHidden = CommunityModuleType.entries.all { hiddenModules.contains(it.name) }
        val emptyState = when {
            areAllModulesHidden -> FeedEmptyState.ALL_MODULES_HIDDEN
            !state.isInitialLoading && state.error == null && !hasContent -> FeedEmptyState.NO_DATA
            else -> null
        }
        state.copy(cards = visibleItems, emptyState = emptyState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS), CommunityContentState())

    /**
     * The Places of Interest module is loaded differently from the other "For you" modules, because it depends on location permission
     * and the user's saved location from Places. So it lives in its own flow that reacts to those changes directly.
     * Whenever the saved location or the feed language changes, we rebuild just this module (emitting a loading
     * placeholder first when permission is granted) rather than reloading the entire tab. The result is merged into
     * forYouState and sorted into its usual position by using ForYouModuleType enum ordinal.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val placesModule: StateFlow<ForYouModule.PlacesOfInterest?> =
        combine(Prefs.placesLastLocationFlow, _wikiSite) { _, _ -> }
        .transformLatest {
            if (hasLocationPermission()) {
                emit(ForYouModule.PlacesOfInterest(age = 0, index = 0, cards = emptyList(), isLoading = true, hasLocationPermission = true))
            }
            emit(buildPlacesModule())
        }
        .catch {
            L.e(it)
            emit(null)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS),
            null
        )

    /**
     * The Discover module (recommended reading list) loads independently of the batched "For you"
     * modules, like Places. It reacts to the Discover settings (enabled state, article count, source, update frequency)
     * and to NewRecommendedReadingListEvent posted when a new list is generated by RecommendedReadingListTask or the settings screen.
     * On each trigger it emits a loading placeholder, then the module read from the local
     * recommended pages. The result is merged into forYouState and sorted into its usual position by
     * ForYouModuleType enum ordinal.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val discoverModule: StateFlow<ForYouModule.Discover?> =
        merge(
            Prefs.observeKeys(
                R.string.preference_key_recommended_reading_list_enabled,
                R.string.preference_key_recommended_reading_list_articles_number,
                R.string.preference_key_recommended_reading_list_source,
                R.string.preference_key_recommended_reading_list_update_frequency
            ),
            FlowEventBus.events.filterIsInstance<NewRecommendedReadingListEvent>().map { }
        )
        .transformLatest {
            if (Prefs.isRecommendedReadingListEnabled) {
                emit(ForYouModule.Discover(age = 0, index = 0, cards = emptyList(), isEnabled = true, isLoading = true))
            }
            emit(buildDiscoverModule())
        }
        .catch {
            L.e(it)
            emit(null)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS),
            null
        )

    private val _forYouState = MutableStateFlow(ForYouContentState())
    val forYouState = combine(
        _forYouState,
        SettingsRepository.hiddenModules,
        SettingsRepository.hiddenCards,
        placesModule,
        discoverModule
    ) { state, hiddenModules, hiddenCards, placesModule, discoverModule ->
        val visibleItems = (state.modules + listOfNotNull(placesModule, discoverModule))
            .sortedBy { ForYouModuleType.valueOf(it.moduleKey()).ordinal }
            .filterNot { hiddenModules.contains(it.moduleKey()) }
            .mapNotNull { module ->
                val visibleCards = module.cards.filterNot { hiddenCards.contains(it.hideKey) }
                // only drop module when it has cards, and they are all hidden, not when it is empty to begin with.
                if (module.cards.isNotEmpty() && visibleCards.isEmpty()) null else module.withCards(visibleCards)
            }
        val areAllModulesHidden = ForYouModuleType.entries.all { hiddenModules.contains(it.name) }
        val isInterestModuleHidden = hiddenModules.contains(ForYouModuleType.BASED_ON_INTEREST.name)
        val emptyState = when {
            areAllModulesHidden -> FeedEmptyState.ALL_MODULES_HIDDEN
            !state.isInitialLoading && state.error == null && visibleItems.isEmpty() -> FeedEmptyState.NO_DATA
            else -> null
        }
        state.copy(
            modules = visibleItems,
            emptyState = emptyState,
            isInterestModuleHidden = isInterestModuleHidden
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS), ForYouContentState())

    // "age" in days from today. 0 = today, 1 = yesterday, etc.
    private var nextCommunityAge = 0

    // Batch counter for "For you" recommendations.
    private var forYouBatchIndex = 0

    private val _tabsState = MutableStateFlow(TabsState(WikipediaApp.instance.tabCount, pulse = false))
    val tabsState = _tabsState.asStateFlow()

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

    private val _unreadCount = MutableStateFlow(NotificationBellState())
    val unreadCount = _unreadCount.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsRepository.migrateLegacyHiddenCards()
        }
        viewModelScope.launch {
            combine(_selectedTab, _wikiSite) { tab, site -> tab to site }
            .distinctUntilChanged()
            .collect { (tab, site) ->
                ensureContentLoaded(tab, site)
            }
        }
    }

    fun refreshCommunityContent() {
        nextCommunityAge = 0
        _communityState.update { CommunityContentState() }
        loadCommunityContent()
    }

    fun refreshForYouContent() {
        forYouBatchIndex = 0
        _forYouState.update { ForYouContentState() }
        loadForYouContent()
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    private fun ensureContentLoaded(tab: HomeTab, site: WikiSite) {
        when (tab) {
            HomeTab.COMMUNITY -> {
                if (_communityState.value.wikiSite != site) {
                    refreshCommunityContent()
                } else if (_communityState.value.cards.isEmpty() && !_communityState.value.isInitialLoading) {
                    loadCommunityContent()
                }
            }
            HomeTab.FOR_YOU -> {
                if (_forYouState.value.wikiSite != site) {
                    refreshForYouContent()
                } else if (_forYouState.value.modules.isEmpty() && !_forYouState.value.isInitialLoading) {
                    loadForYouContent()
                }
            }
        }
    }

    fun updateLanguage(langCode: String) {
        Prefs.homeLanguageCode = langCode
        _wikiSite.value = WikiSite.forLanguageCode(langCode)
    }

    fun updateTabCount(pulse: Boolean = false) {
        _tabsState.value = TabsState(WikipediaApp.instance.tabCount, pulse)
    }

    fun updateSelectedLanguageIfNeeded() {
        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(wikiSite.value.languageCode)) {
            updateLanguage(WikipediaApp.instance.languageState.appLanguageCode)
        }
    }

    /**
     * Loads the next day's community content (today on first call, then progressively older).
     * Safe to call as a retry — the age only advances after a successful fetch.
     */
    fun loadCommunityContent() {
        if (_communityState.value.isInitialLoading || _communityState.value.isLoadingMore) return

        viewModelScope.launch(communityHandler) {
            val isInitial = _communityState.value.cards.isEmpty()
            _communityState.value = _communityState.value.copy(
                wikiSite = wikiSite.value,
                isInitialLoading = isInitial,
                isLoadingMore = !isInitial,
                error = null
            )

            val age = nextCommunityAge
            val date = LocalDate.now().minusDays(nextCommunityAge.toLong())
            val content = ServiceFactory.getRest(wikiSite.value)
                .getFeedFeatured(date.year.toString(), "%02d".format(date.monthValue), "%02d".format(date.dayOfMonth), wikiSite.value.languageCode)

            // Construct Card objects based on the day's content
            val cardsForDay = buildList<Card> {
                content.tfa?.let {
                    add(FeaturedArticleCard(it, age, wikiSite.value))
                }
                content.topRead?.let {
                    add(TopReadCard(it, age, wikiSite.value))
                }
                content.dyk?.let {
                    add(DidYouKnowCard(it, date.toString(), wikiSite.value))
                }
                if (!content.news.isNullOrEmpty()) {
                    add(NewsCard(content.news, age, wikiSite.value))
                }
                if (!content.onthisday.isNullOrEmpty()) {
                    add(OnThisDayCard(content.onthisday.take(2), age, wikiSite.value))
                }
                content.potd?.let {
                    add(FeaturedImageCard(it, age, wikiSite.value))
                }
            }.toMutableList()
            if (cardsForDay.isNotEmpty()) {
                cardsForDay.add(0, DayHeaderCard(age))
            }

            // Advance age only after success, so retry on failure re-fetches the same day.
            nextCommunityAge = age + 1

            _communityState.value = _communityState.value.copy(
                cards = _communityState.value.cards + cardsForDay,
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
                wikiSite = wikiSite.value,
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
                error = null
            )
        }
    }

    fun hideCommunityCard(card: Card) {
        viewModelScope.launch {
            SettingsRepository.addHiddenCard(card.hideKey)
        }
    }

    fun restoreCommunityCard(card: Card) {
        viewModelScope.launch {
            SettingsRepository.removeHiddenCard(card.hideKey)
        }
    }

    fun hideForYouCard(card: ForYouCard) {
        viewModelScope.launch {
            SettingsRepository.addHiddenCard(card.hideKey)
        }
    }

    fun restoreForYouCard(card: ForYouCard) {
        viewModelScope.launch {
            SettingsRepository.removeHiddenCard(card.hideKey)
        }
    }

    fun hideModule(moduleKey: String) {
        viewModelScope.launch {
            SettingsRepository.addHiddenModule(moduleKey)
        }
    }

    fun restoreModule(moduleKey: String) {
        viewModelScope.launch {
            SettingsRepository.removeHiddenModule(moduleKey)
        }
    }

    private suspend fun fetchForYouModules(age: Int): List<ForYouModule> {
        val modules = mutableListOf<ForYouModule>()
        val hiddenCards = SettingsRepository.hiddenCards.first()
        var forYouCollectionSaved = ForYouCollectionSaved()

        try {
            withContext(Dispatchers.Default) {
                forYouCollectionSaved = JsonUtil.decodeFromString<ForYouCollectionSaved>(Prefs.homeForYouModulesToday)!!
            }
        } catch (e: Exception) {
            L.e("Failed to load modules from cache.")
        }

        if (forYouCollectionSaved.dateTime != null &&
            forYouCollectionSaved.dateTime.toLocalDate() == LocalDate.now() &&
            forYouCollectionSaved.modulesPerLanguage.containsKey(wikiSite.value.languageCode)
        ) {
            L.d("Loading modules from cache...")
            val modules = forYouCollectionSaved.modulesPerLanguage[wikiSite.value.languageCode].orEmpty()
            val newModules = mutableListOf<ForYouModule>()
            modules.forEach { module ->
                val filteredCards = module.cards.filterNot { hiddenCards.contains(it.hideKey) }
                if (filteredCards.isNotEmpty()) {
                    newModules.add(module.withCards(filteredCards))
                }
            }
            return newModules
        }
        L.d("Loading modules from network...")

        // --- Interests ---

        val interestTopics = AppDatabase.instance.topicInterestDao().getAllRandom().distinctBy { it.topicId }.take(5)
        interestTopics.forEachIndexed { index, topic ->
            val articleTopic = ArticleTopics.all.find { it.topicId == topic.topicId }
            val entries = ServiceFactory.get(wikiSite.value).getArticlesByTopic(
                "articletopic:" + (articleTopic?.queryTopicId ?: topic.topicId) + "^95",
                limit = 20,
                profile = "classic_noboostlinks",
                sort = "random"
            )
                .query?.pages
                ?.filter { it.pageProps?.disambiguation == null } // Filter out disambiguation pages
                ?.sortedBy { it.index } // Sort by index, as reported by the API
                ?.sortedBy { it.thumbUrl().isNullOrEmpty() } // Sort by whether it has a thumbnail
                ?.map { page ->
                    PageTitle(
                        text = page.title,
                        wiki = wikiSite.value,
                        thumbUrl = page.thumbUrl(),
                        description = page.description,
                        displayText = page.displayTitle(wikiSite.value.languageCode),
                    ).also {
                        if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                        it.extract = page.extract
                    }
                }.orEmpty().map {
                    // TODO: filter items that have already been suggested.
                    BasedOnInterestCard(it, interestTopic = topic)
                }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)

            if (entries.isNotEmpty()) {
                modules.add(ForYouModule.BasedOnInterest(age, index, entries))
            }
        }
        val interestArticles = AppDatabase.instance.articleInterestDao().getAllRandom(wikiSite.value.languageCode).take(5)
        interestArticles.forEachIndexed { index, article ->
            val searchTerm = StringUtil.removeUnderscores(article.apiTitle)
            val entries = ServiceFactory.get(wikiSite.value).searchMoreLike("morelike:$searchTerm", 10, 10)
                .query?.pages?.filter { it.title != searchTerm && it.title != MainPageNameData.valueFor(wikiSite.value.languageCode) }?.map { page ->
                    PageTitle(
                        text = page.title,
                        wiki = wikiSite.value,
                        thumbUrl = page.thumbUrl(),
                        description = page.description,
                        displayText = page.displayTitle(wikiSite.value.languageCode),
                    ).also {
                        if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                        it.extract = page.extract
                    }
                }.orEmpty().map {
                    // TODO: filter items that have already been suggested.
                    BasedOnInterestCard(it, interestArticle = article)
                }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)

            if (entries.isNotEmpty()) {
                modules.add(ForYouModule.BasedOnInterest(age, index, entries))
            }
        }

        // --- Because you read ---

        val becauseYouReadCards = buildList {
            val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, wikiSite.value.languageCode)
            if (lastReadEntries.size > age) {
                val entry = lastReadEntries[age]
                val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.value.languageCode).isNullOrEmpty()
                val searchTerm = StringUtil.removeUnderscores(entry.title.prefixedText)

                var moreLikeMaxAge = 86400
                if (hasParentLanguageCode) {
                    moreLikeMaxAge = 0
                }
                val moreLikeResponse = ServiceFactory.get(entry.title.wikiSite).searchMoreLike("morelike:$searchTerm",
                    Constants.SUGGESTION_REQUEST_ITEMS * 2, Constants.SUGGESTION_REQUEST_ITEMS * 2, sMaxAge = moreLikeMaxAge, maxAge = moreLikeMaxAge)

                val relatedPages = moreLikeResponse.query?.pages?.filter { it.title != searchTerm && it.title != MainPageNameData.valueFor(entry.title.wikiSite.languageCode) }?.map {
                    PageSummary(it.displayTitle(wikiSite.value.languageCode), it.title, it.description, it.extract, it.thumbUrl(), wikiSite.value.languageCode)
                }?.take(Constants.SUGGESTION_REQUEST_ITEMS)

                addAll(relatedPages?.map {
                    BecauseYouReadCard(it.getPageTitle(wikiSite.value), entry.title.displayText)
                } ?: emptyList())
            }
        }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)

        if (becauseYouReadCards.isNotEmpty()) {
            // The index for this module is always 0 because there is always a single instance of this module, per age.
            modules.add(ForYouModule.BecauseYouRead(age, 0, becauseYouReadCards))
        }

        // --- Continue reading ---

        val continueReadingCards = buildList {
            val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, wikiSite.value.languageCode)
            if (lastReadEntries.size > age) {
                add(ContinueReadingCard(lastReadEntries[age].title, HistoryEntry.SOURCE_HISTORY))
            }
            AppDatabase.instance.readingListPageDao().getMostRecentSavedPagesByLang(wikiSite.value.languageCode, 10).take(2)
                .forEach {
                    add(ContinueReadingCard(ReadingListPage.toPageTitle(it), HistoryEntry.SOURCE_READING_LIST))
                }
        }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)
        if (continueReadingCards.isNotEmpty()) {
            ServiceFactory.get(wikiSite.value).getInfoWithExtractsByPageTitles(continueReadingCards.map { it.title.prefixedText }.fastJoinToString("|"))
                .query?.pages?.forEach { page ->
                    continueReadingCards.find { it.title.prefixedText == StringUtil.addUnderscores(page.title) }?.let {
                        it.title.description = page.description
                        it.title.thumbUrl = page.thumbUrl()
                        it.title.displayText = page.displayTitle(wikiSite.value.languageCode)
                        it.title.extract = page.extract
                    }
                }
        }
        if (continueReadingCards.isNotEmpty()) {
            // The index for this module is always 0 because there is always a single instance of this module, per age.
            modules.add(ForYouModule.ContinueReading(age, 0, continueReadingCards))
        }

        // --- Random article ---

        val random = ServiceFactory.getRest(wikiSite.value).getRandomSummary()
        val randomCard = RandomCard(random.getPageTitle(wikiSite.value))
        if (!hiddenCards.contains(randomCard.hideKey)) {
            // The index for this module is always 0 because there is always a single instance of this module, per age.
            modules.add(ForYouModule.Random(age, 0, listOf(randomCard)))
        }

        forYouCollectionSaved = ForYouCollectionSaved(
            dateTime = LocalDateTime.now(),
            modulesPerLanguage = forYouCollectionSaved.modulesPerLanguage + (wikiSite.value.languageCode to modules)
        )
        withContext(Dispatchers.Default) {
            Prefs.homeForYouModulesToday = JsonUtil.encodeToString(forYouCollectionSaved).orEmpty()
        }
        return modules
    }

    private suspend fun buildDiscoverModule(): ForYouModule.Discover? {
        if (!Prefs.isRecommendedReadingListEnabled) {
            return ForYouModule.Discover(age = 0, index = 0, cards = emptyList(), isEnabled = false)
        }
        val pages = RecommendedReadingListHelper.generateRecommendedReadingList(Prefs.resetRecommendedReadingList)

        val displayPages = pages.take(MAX_DISCOVER_ARTICLE_CARDS)
        if (displayPages.isEmpty()) {
            return null
        }
        updateMissingExtracts(displayPages)
        val cards = displayPages.map { page ->
            DiscoverCard(
                PageTitle(
                    text = page.apiTitle,
                    wiki = page.wiki,
                    thumbUrl = page.thumbUrl,
                    description = page.description,
                    displayText = page.displayTitle,
                    extract = page.extract
                )
            )
        }
        val displayCards = if (pages.size > MAX_DISCOVER_ARTICLE_CARDS) cards + SeeAllRecommendationCard() else cards
        return ForYouModule.Discover(
            age = 0,
            index = 0,
            cards = displayCards,
            isEnabled = true,
            updateFrequency = Prefs.recommendedReadingListUpdateFrequency
        )
    }

    /**
     * Pages cached before the [RecommendedPage.extract] column existed have a null extract.
     * This function fetches those null extracts in a single batched request and saves them to DB.
     */
    private suspend fun updateMissingExtracts(pages: List<RecommendedPage>) {
        val missing = pages.filter { it.extract == null }
        if (missing.isEmpty()) {
            return
        }
        runCatching {
            ServiceFactory.get(wikiSite.value)
                .getInfoWithExtractsByPageTitles(missing.fastJoinToString("|") { it.apiTitle })
                .query?.pages?.forEach { page ->
                    missing.find { it.apiTitle == StringUtil.addUnderscores(page.title) }?.extract = page.extract
                }
        }.onFailure { L.e(it) }
        AppDatabase.instance.recommendedPageDao().updateAll(missing.filter { it.extract != null })
    }

    private suspend fun buildPlacesModule(): ForYouModule.PlacesOfInterest? {
        if (!hasLocationPermission()) {
            return ForYouModule.PlacesOfInterest(age = 0, index = 0, cards = emptyList(), hasLocationPermission = false)
        }
        val location = Prefs.placesLastLocationAndZoomLevel?.first
            ?: return ForYouModule.PlacesOfInterest(age = 0, index = 0, cards = emptyList(), hasLocationPermission = false)

        val cards = getPlacesCards(location)
        if (cards.isEmpty()) {
            return null
        }
        return ForYouModule.PlacesOfInterest(age = 0, index = 0, cards = cards, hasLocationPermission = true)
    }

    private suspend fun getPlacesCards(savedLocation: Location): List<PlacesOfInterestCard> {
        val coordinates = "${savedLocation.latitude}|${savedLocation.longitude}"
        return ServiceFactory.get(wikiSite.value)
            .getGeoSearchWithExtracts(coordinates, PLACES_SEARCH_RADIUS_METERS, PLACES_ARTICLES_REQUEST_LIMIT, PLACES_ARTICLES_REQUEST_LIMIT)
            .query?.pages.orEmpty()
            .filter { it.coordinates != null }
            .sortedBy {
                savedLocation.distanceTo(Location("").apply {
                    latitude = it.coordinates!![0].lat
                    longitude = it.coordinates[0].lon
                })
            }
            .map { page ->
                val title = PageTitle(
                    text = page.title,
                    wiki = wikiSite.value,
                    thumbUrl = page.thumbUrl(),
                    description = page.description,
                    displayText = page.displayTitle(wikiSite.value.languageCode),
                    extract = page.extract
                )
                val articleLocation = Location("").apply {
                    latitude = page.coordinates!![0].lat
                    longitude = page.coordinates[0].lon
                }
                val distance = GeoUtil.getDistanceWithUnit(savedLocation, articleLocation, Locale.getDefault())
                PlacesOfInterestCard(title, distance)
            }
    }

    private fun hasLocationPermission(): Boolean {
        val context = WikipediaApp.instance
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshUnreadNotificationCount() {
        _unreadCount.update { it.copy(unreadCount = Prefs.notificationUnreadCount, canShow = AccountUtil.isLoggedIn) }
    }
}
