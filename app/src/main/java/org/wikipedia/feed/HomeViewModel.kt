package org.wikipedia.feed

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.ui.util.fastJoinToString
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.wikipedia.feed.interests.NewWithinInterestABTest
import org.wikipedia.feed.model.BasedOnInterestCard
import org.wikipedia.feed.model.BecauseYouReadCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ContinueReadingCard
import org.wikipedia.feed.model.DayHeaderCard
import org.wikipedia.feed.model.DidYouKnowCard
import org.wikipedia.feed.model.DiscoverCard
import org.wikipedia.feed.model.FeaturedArticleCard
import org.wikipedia.feed.model.FeaturedImageCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.GamesModulePromptCard
import org.wikipedia.feed.model.NewWithinInterestCard
import org.wikipedia.feed.model.NewsCard
import org.wikipedia.feed.model.OnThisDayCard
import org.wikipedia.feed.model.PlacesOfInterestCard
import org.wikipedia.feed.model.RandomCard
import org.wikipedia.feed.model.SeeAllRecommendationCard
import org.wikipedia.feed.model.TopReadCard
import org.wikipedia.feed.model.WikiGameCard
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.personalization.interest.InterestSelectionRepository
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.games.WikiGames
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.onthisday.OnThisDayGameProvider
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
private const val MAX_CARDS_PER_MODULE = 4
private const val MAX_INTEREST_TOPIC_MODULES = 5
private const val MAX_INTEREST_ARTICLE_MODULES = 5
private const val MAX_NEW_WITHIN_INTEREST_TOPICS = 4
private const val MORE_LIKE_REQUEST_ITEMS = 10

private fun slotKeyOf(moduleKey: String, age: Int, index: Int) = "$moduleKey-$age-$index"

@Serializable
sealed class ForYouModule {
    abstract val age: Int
    abstract val index: Int
    abstract val cards: List<ForYouCard>

    abstract fun withCards(cards: List<ForYouCard>): ForYouModule
    abstract fun moduleKey(): String

    /**
     * Identifies the slot that this module occupies in the "For you" feed, and stays the same whether the
     * slot is still an unloaded [Placeholder] or the module that eventually replaces it. Used both as the
     * list key in the UI and as the cache key of an already-loaded module.
     */
    val slotKey get() = slotKeyOf(moduleKey(), age, index)

    /**
     * Stands in for a module whose contents haven't been fetched yet. The feed is laid out entirely as
     * placeholders as soon as the day's plan of modules is known, and each one is swapped for its real
     * module (or removed, if the module turns out to be empty) when the user scrolls near it.
     */
    data class Placeholder(
        override val age: Int,
        override val index: Int,
        val forModuleKey: String,
        val error: Throwable? = null,
        override val cards: List<ForYouCard> = emptyList()
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = this
        override fun moduleKey(): String = forModuleKey
    }

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
    data class NewWithinInterest(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.NEW_WITHIN_INTEREST.name
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

    data class Games(
        override val age: Int,
        override val index: Int,
        override val cards: List<ForYouCard>,
        val isLoading: Boolean = false
    ) : ForYouModule() {
        override fun withCards(cards: List<ForYouCard>): ForYouModule = copy(cards = cards)
        override fun moduleKey(): String = ForYouModuleType.GAMES.name
    }
}

/**
 * Describes a single "For you" module that is yet to be fetched. The plan of requests for the current day
 * is built up front from local data only (no network), which lets the feed lay out a placeholder for every
 * module it will eventually contain, while the contents of each module are fetched one at a time.
 */
@Serializable
sealed class ForYouModuleRequest {
    abstract val age: Int
    abstract val index: Int
    abstract val moduleType: ForYouModuleType

    val slotKey get() = slotKeyOf(moduleType.name, age, index)

    @Serializable
    data class TopicInterest(
        override val age: Int,
        override val index: Int,
        val topic: InterestTopic
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.BASED_ON_INTEREST
    }

    @Serializable
    data class ArticleInterest(
        override val age: Int,
        override val index: Int,
        val article: InterestArticle
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.BASED_ON_INTEREST
    }

    @Serializable
    data class NewWithinInterest(
        override val age: Int,
        override val index: Int,
        val topics: List<InterestTopic>
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.NEW_WITHIN_INTEREST
    }

    @Serializable
    data class BecauseYouRead(
        override val age: Int,
        override val index: Int
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.BECAUSE_YOU_READ
    }

    @Serializable
    data class ContinueReading(
        override val age: Int,
        override val index: Int
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.CONTINUE_READING
    }

    @Serializable
    data class Random(
        override val age: Int,
        override val index: Int
    ) : ForYouModuleRequest() {
        override val moduleType get() = ForYouModuleType.RANDOM
    }
}

/**
 * The day's plan of modules for a single language, together with whatever has been fetched so far.
 * [emptySlotKeys] records the slots that resolved to no content, so that they aren't requested again
 * on the same day.
 */
@Serializable
data class ForYouLanguageCollection(
    val plan: List<ForYouModuleRequest> = emptyList(),
    val modules: List<ForYouModule> = emptyList(),
    val emptySlotKeys: Set<String> = emptySet()
)

@Serializable
class ForYouCollectionSaved(
    @Serializable(with = LocalDateTimeSerializer::class) val dateTime: LocalDateTime? = null,
    val collectionPerLanguage: Map<String, ForYouLanguageCollection> = emptyMap()
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

    /**
     * The Games module is loaded reactively, like Places, because its content should reflect live game progress.
     * We observe today's game as a Room Flow in [DailyGameHistory] DB, so every write during play re-emits and rebuilds the
     * module, moving the card through Preview -> InProgress -> Completed without a manual feed refresh.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val gameModule: StateFlow<ForYouModule.Games?> =
        _wikiSite.flatMapLatest { site ->
            val supportedGames = getSupportedGames(site.languageCode)
            // short-circuit if no games are supported for this language
            if (supportedGames.isEmpty()) {
                return@flatMapLatest flowOf(null)
            }
            val today = LocalDate.now()
            AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDateFlow(
                gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
                language = site.languageCode,
                year = today.year,
                month = today.monthValue,
                day = today.dayOfMonth
            )
            .transformLatest<DailyGameHistory?, ForYouModule.Games?> {
                emit(ForYouModule.Games(age = 0, index = 0, cards = emptyList(), isLoading = true))
                emit(buildGameModule(supportedGames))
            }
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

    // Combine the reactive modules first so the outer combine stays within the 5-flow typed overloads.
    private val reactiveModules = combine(placesModule, discoverModule, gameModule) { places, discover, game ->
        listOfNotNull(places, discover, game)
    }

    private val _forYouState = MutableStateFlow(ForYouContentState())
    val forYouState = combine(
        _forYouState,
        SettingsRepository.hiddenModules,
        SettingsRepository.hiddenCards,
        reactiveModules
    ) { state, hiddenModules, hiddenCards, reactiveModules ->
        val visibleItems = (state.modules + reactiveModules)
            .sortedBy { ForYouModuleType.valueOf(it.moduleKey()).ordinal }
            .filterNot { hiddenModules.contains(it.moduleKey()) }
            .mapNotNull { module ->
                val visibleCards = module.cards.filterNot { hiddenCards.contains(it.hideKey) }
                // only drop module when it has cards, and they are all hidden, not when it is empty to begin with.
                if (module.cards.isNotEmpty() && visibleCards.isEmpty()) null else module.withCards(visibleCards)
            }
        val areAllModulesHidden = ForYouModuleType.entries().all { hiddenModules.contains(it.key) }
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
        // The feed always starts out loading: nothing has been laid out yet at the time of subscription, and
        // a state that claims otherwise would briefly present an empty feed before the first real emission.
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS), ForYouContentState(isInitialLoading = true))

    // "age" in days from today. 0 = today, 1 = yesterday, etc.
    private var nextCommunityAge = 0

    // Batch counter for "For you" recommendations.
    private var forYouBatchIndex = 0

    // The day's plan of "For you" modules, keyed by slot key. Each entry is fetched lazily, when the user
    // scrolls to the module that occupies its slot.
    private var forYouPlan = emptyMap<String, ForYouModuleRequest>()

    // Slots that are being fetched right now, so that a slot is never requested twice concurrently.
    private val forYouSlotJobs = mutableMapOf<String, Job>()

    // In-memory mirror of the modules cached in Prefs, updated as each module resolves.
    private var forYouCache = ForYouCollectionSaved()
    private val forYouCacheMutex = Mutex()

    // Start of the current load, used to report how long it takes the first module to arrive.
    private var forYouLoadStartMillis = 0L

    private val _tabsState = MutableStateFlow(TabsState(WikipediaApp.instance.tabCount, pulse = false))
    val tabsState = _tabsState.asStateFlow()

    // Holds the API titles of Community-tab articles currently in the feed, to be queried whether they are saved in a reading list.
    // The "For you" tab does not contribute here; its cards resolve saved state lazily on overflow-menu tap (see resolveForYouSavedState in HomeFragment).
    private val _savedInReadingApiTitles = MutableStateFlow<List<String>>(emptyList())

    // Derives saved state by asking Room only about those titles that we want to monitor.
    // flatMapLatest restarts the DB observation whenever _savedInReadingApiTitles changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedInReadingListTitles: StateFlow<Set<String>> = _savedInReadingApiTitles
        .flatMapLatest { titles ->
            if (titles.isEmpty()) flowOf(emptySet())
            else AppDatabase.instance.readingListPageDao()
                .observeSavedApiTitles(
                    wikiSite.value.languageCode,
                    titles,
                    ReadingListPage.STATUS_QUEUE_FOR_DELETE
                )
                .map { it.toSet() }
        }
        .catch {
            L.e(it)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS), emptySet())

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

    private val _forYouNetworkLatency = MutableStateFlow(0L)
    val forYouNetworkLatency = _forYouNetworkLatency.asStateFlow()

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
        forYouPlan = emptyMap()
        forYouSlotJobs.values.forEach { it.cancel() }
        forYouSlotJobs.clear()
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
                .getFeedFeatured(date.year.toString(), "%02d".format(Locale.ROOT, date.monthValue), "%02d".format(Locale.ROOT, date.dayOfMonth), wikiSite.value.languageCode)

            // Construct Card objects based on the day's content
            val cardsForDay = buildList {
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

            _savedInReadingApiTitles.value = _communityState.value.cards.filterIsInstance<FeaturedArticleCard>()
                .map { it.page.apiTitle }
        }
    }

    /**
     * Lays out the next batch of personalized recommendations for the "For you" tab: it decides which
     * modules the feed will contain, without fetching any of their contents. Modules that were already
     * fetched earlier today are restored from the cache; the rest become placeholders that are filled in
     * by [loadForYouModules] as the user scrolls to them.
     * Safe to call as a retry — the batch index only advances after a successful build.
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

            val age = forYouBatchIndex
            val langCode = wikiSite.value.languageCode
            val collection = restoreForYouCache(langCode)
            val cachedPlan = collection.plan.filter { it.age == age }

            val plan: List<ForYouModuleRequest>
            if (cachedPlan.isNotEmpty()) {
                L.d("Loading modules plan from cache...")
                plan = cachedPlan
            } else {
                L.d("Building modules plan...")
                plan = buildForYouPlan(age, langCode)
                updateForYouCache(langCode) { it.copy(plan = it.plan + plan) }
                // Only a plan built from scratch measures how long the feed takes to fetch from the network.
                forYouLoadStartMillis = System.currentTimeMillis()
            }

            val cachedModules = collection.modules.associateBy { it.slotKey }
            val newModules = plan.mapNotNull { request ->
                if (collection.emptySlotKeys.contains(request.slotKey)) null
                else cachedModules[request.slotKey]
                    ?: ForYouModule.Placeholder(request.age, request.index, request.moduleType.name)
            }

            // Advance batch index only after success.
            forYouBatchIndex = age + 1
            forYouPlan = forYouPlan + plan.associateBy { it.slotKey }

            _forYouState.value = _forYouState.value.copy(
                modules = _forYouState.value.modules + newModules,
                isInitialLoading = false,
                isLoadingMore = false,
                error = null
            )
        }
    }

    /**
     * Fetches the contents of the given "For you" module slots, if they haven't been fetched already.
     * The tab calls this with the module currently in view plus the next one, so that only what the user
     * is about to see is ever requested.
     * A slot whose previous attempt failed is only retried when [retry] is set, so that the failed module
     * keeps showing its error until the user asks for it again.
     */
    fun loadForYouModules(slotKeys: List<String>, retry: Boolean = false) {
        slotKeys.forEach { slotKey ->
            val request = forYouPlan[slotKey] ?: return@forEach
            val placeholder = _forYouState.value.modules
                .find { it.slotKey == slotKey } as? ForYouModule.Placeholder ?: return@forEach
            if (placeholder.error != null && !retry) return@forEach
            if (forYouSlotJobs.containsKey(slotKey)) return@forEach

            if (placeholder.error != null) {
                replaceForYouModule(slotKey) { placeholder.copy(error = null) }
            }
            forYouSlotJobs[slotKey] = viewModelScope.launch { loadForYouModule(request) }
        }
    }

    private suspend fun loadForYouModule(request: ForYouModuleRequest) {
        val langCode = wikiSite.value.languageCode
        try {
            val module = fetchForYouModule(request)
            if (forYouLoadStartMillis > 0L) {
                _forYouNetworkLatency.value = System.currentTimeMillis() - forYouLoadStartMillis
                forYouLoadStartMillis = 0L
            }
            // A module with no content leaves the feed entirely, rather than showing an empty screen.
            replaceForYouModule(request.slotKey) { module }
            updateForYouCache(langCode) { collection ->
                if (module == null) collection.copy(emptySlotKeys = collection.emptySlotKeys + request.slotKey)
                else collection.copy(modules = collection.modules.filterNot { it.slotKey == request.slotKey } + module)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            L.e(e)
            replaceForYouModule(request.slotKey) { (it as? ForYouModule.Placeholder)?.copy(error = e) ?: it }
        } finally {
            forYouSlotJobs.remove(request.slotKey)
        }
    }

    private fun replaceForYouModule(slotKey: String, transform: (ForYouModule) -> ForYouModule?) {
        _forYouState.update { state ->
            state.copy(modules = state.modules.mapNotNull { if (it.slotKey == slotKey) transform(it) else it })
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

    /**
     * Decides which "For you" modules the given batch will contain, using only locally available data,
     * so that this is fast enough to run before anything is shown. The resulting requests are ordered the
     * same way the modules are laid out in the feed.
     */
    private suspend fun buildForYouPlan(age: Int, langCode: String): List<ForYouModuleRequest> {
        val requests = mutableListOf<ForYouModuleRequest>()

        // "Based on interest" modules come from both topics and articles, and share a single running index
        // so that every one of them ends up with a slot key of its own.
        var basedOnInterestIndex = 0
        AppDatabase.instance.topicInterestDao().getAllRandom().distinctBy { it.topicId }.take(MAX_INTEREST_TOPIC_MODULES)
            .forEach { requests.add(ForYouModuleRequest.TopicInterest(age, basedOnInterestIndex++, it)) }
        AppDatabase.instance.articleInterestDao().getAllRandom(langCode).take(MAX_INTEREST_ARTICLE_MODULES)
            .forEach { requests.add(ForYouModuleRequest.ArticleInterest(age, basedOnInterestIndex++, it)) }

        // There is always a single instance of each of the modules below, per age, so their index is always 0.
        val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, langCode)
        if (lastReadEntries.size > age) {
            requests.add(ForYouModuleRequest.BecauseYouRead(age, 0))
            requests.add(ForYouModuleRequest.ContinueReading(age, 0))
        } else if (AppDatabase.instance.readingListPageDao().getMostRecentSavedPagesByLang(langCode, 1).isNotEmpty()) {
            requests.add(ForYouModuleRequest.ContinueReading(age, 0))
        }

        requests.add(ForYouModuleRequest.Random(age, 0))

        val newWithinInterestTest = NewWithinInterestABTest()
        if (newWithinInterestTest.isTestActive()) {
            newWithinInterestTest.maybeSendExposureEvent()
            if (newWithinInterestTest.isTestGroupUser()) {
                val topics = AppDatabase.instance.topicInterestDao().getAllRandom().distinctBy { it.topicId }.take(MAX_NEW_WITHIN_INTEREST_TOPICS)
                if (topics.isNotEmpty()) {
                    requests.add(ForYouModuleRequest.NewWithinInterest(age, 0, topics))
                }
            }
        }

        return requests.sortedBy { it.moduleType.ordinal }
    }

    /**
     * Fetches the contents of a single module. Returns null when the module has nothing to show, in which
     * case it is dropped from the feed.
     */
    private suspend fun fetchForYouModule(request: ForYouModuleRequest): ForYouModule? {
        val site = wikiSite.value
        val hiddenCards = SettingsRepository.hiddenCards.first()

        return when (request) {
            is ForYouModuleRequest.TopicInterest -> {
                val articleTopic = ArticleTopics.all.find { it.topicId == request.topic.topicId }
                val cards = withContext(Dispatchers.IO) {
                    InterestSelectionRepository.getArticlesByTopic(site, articleTopic?.queryTopicId ?: request.topic.topicId)
                }.map {
                    // TODO: filter items that have already been suggested.
                    BasedOnInterestCard(it, interestTopic = request.topic)
                }.filterNot { hiddenCards.contains(it.hideKey) }.take(MAX_CARDS_PER_MODULE)
                if (cards.isEmpty()) null else ForYouModule.BasedOnInterest(request.age, request.index, cards)
            }

            is ForYouModuleRequest.ArticleInterest -> {
                val searchTerm = StringUtil.removeUnderscores(request.article.apiTitle)
                val cards = withContext(Dispatchers.IO) {
                    ServiceFactory.get(site).searchMoreLike("morelike:$searchTerm", MORE_LIKE_REQUEST_ITEMS, MORE_LIKE_REQUEST_ITEMS)
                        .query?.pages?.filter { it.title != searchTerm && it.title != MainPageNameData.valueFor(site.languageCode) }?.map { page ->
                            PageTitle(
                                text = page.title,
                                wiki = site,
                                thumbUrl = page.thumbUrl(),
                                description = page.description,
                                displayText = page.displayTitle(site.languageCode),
                            ).also {
                                if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                                it.extract = page.extract
                            }
                        }.orEmpty()
                }.map {
                    // TODO: filter items that have already been suggested.
                    BasedOnInterestCard(it, interestArticle = request.article)
                }.filterNot { hiddenCards.contains(it.hideKey) }.take(MAX_CARDS_PER_MODULE)
                if (cards.isEmpty()) null else ForYouModule.BasedOnInterest(request.age, request.index, cards)
            }

            is ForYouModuleRequest.NewWithinInterest -> {
                val cards = coroutineScope {
                    request.topics.map { topic ->
                        async(Dispatchers.IO) {
                            val articleTopic = ArticleTopics.all.find { it.topicId == topic.topicId }
                            val titles = InterestSelectionRepository.getNewArticlesWithinTopic(site, articleTopic?.queryTopicId ?: topic.topicId).take(MAX_CARDS_PER_MODULE)
                            listOf(NewWithinInterestCard(titles, interestTopic = topic))
                                .filterNot { it.titles.isEmpty() || hiddenCards.contains(it.hideKey) }
                        }
                    }.awaitAll().flatten()
                }
                if (cards.isEmpty()) null else ForYouModule.NewWithinInterest(request.age, request.index, cards)
            }

            is ForYouModuleRequest.BecauseYouRead -> {
                val age = request.age
                val cards = withContext(Dispatchers.IO) {
                    buildList {
                        val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, site.languageCode)
                        if (lastReadEntries.size > age) {
                            val entry = lastReadEntries[age]
                            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(site.languageCode).isNullOrEmpty()
                            val searchTerm = StringUtil.removeUnderscores(entry.title.prefixedText)

                            var moreLikeMaxAge = 86400
                            if (hasParentLanguageCode) {
                                moreLikeMaxAge = 0
                            }
                            val moreLikeResponse = ServiceFactory.get(entry.title.wikiSite).searchMoreLike("morelike:$searchTerm",
                                Constants.SUGGESTION_REQUEST_ITEMS * 2, Constants.SUGGESTION_REQUEST_ITEMS * 2, sMaxAge = moreLikeMaxAge, maxAge = moreLikeMaxAge)

                            val relatedPages = moreLikeResponse.query?.pages?.filter { it.title != searchTerm && it.title != MainPageNameData.valueFor(entry.title.wikiSite.languageCode) }?.map {
                                PageSummary(
                                    it.displayTitle(site.languageCode),
                                    it.title,
                                    it.description,
                                    it.extract,
                                    it.thumbUrl(),
                                    site.languageCode
                                )
                            }?.take(Constants.SUGGESTION_REQUEST_ITEMS)

                            addAll(relatedPages?.map {
                                BecauseYouReadCard(
                                    it.getPageTitle(site),
                                    entry.title.displayText
                                )
                            } ?: emptyList())
                        }
                    }.filterNot { hiddenCards.contains(it.hideKey) }.take(MAX_CARDS_PER_MODULE)
                }
                if (cards.isEmpty()) null else ForYouModule.BecauseYouRead(request.age, request.index, cards)
            }

            is ForYouModuleRequest.ContinueReading -> {
                val age = request.age
                val cards = withContext(Dispatchers.IO) {
                    val continueReadingCards = buildList {
                        val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, site.languageCode)
                        if (lastReadEntries.size > age) {
                            add(
                                ContinueReadingCard(
                                    lastReadEntries[age].title,
                                    HistoryEntry.SOURCE_HISTORY
                                )
                            )
                        }
                        AppDatabase.instance.readingListPageDao().getMostRecentSavedPagesByLang(site.languageCode, 10).take(2)
                            .forEach {
                                add(
                                    ContinueReadingCard(
                                        ReadingListPage.toPageTitle(it),
                                        HistoryEntry.SOURCE_READING_LIST
                                    )
                                )
                            }
                    }.filterNot { hiddenCards.contains(it.hideKey) }.take(MAX_CARDS_PER_MODULE)
                    if (continueReadingCards.isNotEmpty()) {
                        ServiceFactory.get(site).getInfoWithExtractsByPageTitles(continueReadingCards.map { it.title.prefixedText }.fastJoinToString("|"))
                            .query?.pages?.forEach { page ->
                                continueReadingCards.find { it.title.prefixedText == StringUtil.addUnderscores(page.title) }?.let {
                                    it.title.description = page.description
                                    it.title.thumbUrl = page.thumbUrl()
                                    it.title.displayText = page.displayTitle(site.languageCode)
                                    it.title.extract = page.extract
                                }
                            }
                    }
                    continueReadingCards
                }
                if (cards.isEmpty()) null else ForYouModule.ContinueReading(request.age, request.index, cards)
            }

            is ForYouModuleRequest.Random -> {
                val randomCard = withContext(Dispatchers.IO) {
                    RandomCard(ServiceFactory.getRest(site).getRandomSummary().getPageTitle(site))
                }
                if (hiddenCards.contains(randomCard.hideKey)) null
                else ForYouModule.Random(request.age, request.index, listOf(randomCard))
            }
        }
    }

    /**
     * Reads today's cached "For you" collection into memory, discarding it if it was saved on a previous day,
     * and returns the part of it that belongs to the given language.
     */
    private suspend fun restoreForYouCache(langCode: String): ForYouLanguageCollection {
        return forYouCacheMutex.withLock {
            val saved = withContext(Dispatchers.Default) {
                JsonUtil.decodeFromString<ForYouCollectionSaved>(Prefs.homeForYouModulesToday)
            }
            forYouCache = if (saved?.dateTime?.toLocalDate() == LocalDate.now()) saved else ForYouCollectionSaved()
            forYouCache.collectionPerLanguage[langCode] ?: ForYouLanguageCollection()
        }
    }

    /**
     * Applies an incremental change to the cached collection of the given language, and writes the whole
     * collection back to Prefs. Guarded by a mutex, since modules resolve concurrently.
     */
    private suspend fun updateForYouCache(langCode: String, transform: (ForYouLanguageCollection) -> ForYouLanguageCollection) {
        forYouCacheMutex.withLock {
            val collection = forYouCache.collectionPerLanguage[langCode] ?: ForYouLanguageCollection()
            val updated = ForYouCollectionSaved(
                dateTime = LocalDateTime.now(),
                collectionPerLanguage = forYouCache.collectionPerLanguage + (langCode to transform(collection))
            )
            forYouCache = updated
            withContext(Dispatchers.Default) {
                Prefs.homeForYouModulesToday = JsonUtil.encodeToString(updated).orEmpty()
            }
        }
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

    private suspend fun buildGameModule(supportedGames: List<WikiGames>): ForYouModule.Games {
        val today = LocalDate.now()
        val gameCards = supportedGames
            .mapNotNull { buildWikiGame(it, today)?.let { game ->
                WikiGameCard(
                    game,
                    today.toString()
                )
            } }
        val cards = gameCards + GamesModulePromptCard()

        return ForYouModule.Games(age = 0, index = 0, cards = cards)
    }

    private suspend fun buildWikiGame(game: WikiGames, date: LocalDate): WikiGame? = when (game) {
        WikiGames.WHICH_CAME_FIRST -> {
            val state = OnThisDayGameProvider.getGameState(wikiSite.value, date)
            WikiGame.OnThisDayGame(state)
        }
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

    private fun getSupportedGames(languageCode: String): List<WikiGames> {
        return WikiGames.entries.filter { it.isLangSupported(languageCode) }
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
