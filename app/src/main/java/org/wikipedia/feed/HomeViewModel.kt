package org.wikipedia.feed

import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.NotificationBellState
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.BasedOnInterestCard
import org.wikipedia.feed.model.BecauseYouReadCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ContinueReadingCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.json.LocalDateTimeSerializer
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsRepository
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.settings.homefeed.ForYouModuleType
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.time.LocalDate
import java.time.LocalDateTime

enum class HomeTab { COMMUNITY, FOR_YOU }
private const val MAX_HIDDEN_CARDS = 100
private const val MAX_STOP_TIMEOUT_MILLIS = 5000L

@Serializable
sealed class ForYouModule {
    abstract val age: Int
    abstract val index: Int
    abstract val cards: List<ForYouCard>

    abstract fun withCards(cards: List<ForYouCard>): ForYouModule
    abstract fun moduleKey(): String

    fun matchesIdentity(other: ForYouModule): Boolean {
        return this::class == other::class && age == other.age && index == other.index
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
}

@Serializable
class ForYouCollectionSaved(
    @Serializable(with = LocalDateTimeSerializer::class) val dateTime: LocalDateTime? = null,
    val modulesPerLanguage: Map<String, List<ForYouModule>> = emptyMap()
)

data class CommunityContentState(
    val cards: List<Card> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = true,
    val areAllModulesHidden: Boolean = false
)

data class ForYouContentState(
    val modules: List<ForYouModule> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val canLoadMore: Boolean = true,
    val interestHidden: Boolean = false,
    val emptyState: ForYouEmptyState? = null
)

enum class ForYouEmptyState {
    ALL_MODULES_HIDDEN,
    NO_DATA
}

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
    val communityState = combine(_communityState, SettingsRepository.hiddenModules) { state, hiddenModules ->
        val visibleModules = state.cards.filterNot { hiddenModules.contains(it.moduleKey()) }
        state.copy(cards = visibleModules, areAllModulesHidden = CommunityModuleType.entries.all { hiddenModules.contains(it.name) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(MAX_STOP_TIMEOUT_MILLIS), CommunityContentState())

    private val _forYouState = MutableStateFlow(ForYouContentState())
    val forYouState = combine(_forYouState, SettingsRepository.hiddenModules) { state, hiddenModules ->
        val visibleModules = state.modules.filterNot { hiddenModules.contains(it.moduleKey()) }
        val allHidden = ForYouModuleType.entries.all { hiddenModules.contains(it.name) }
        val interestHidden = hiddenModules.contains(ForYouModuleType.BASED_ON_INTEREST.name)
        val emptyState = when {
            allHidden -> ForYouEmptyState.ALL_MODULES_HIDDEN
            !state.isInitialLoading && state.error == null && visibleModules.isEmpty() -> ForYouEmptyState.NO_DATA
            else -> null
        }
        state.copy(
            modules = visibleModules,
            emptyState = emptyState,
            interestHidden = interestHidden
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
        if (_selectedTab.value == HomeTab.COMMUNITY) {
            loadCommunityContent()
        } else {
            loadForYouContent()
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
        if (tab == HomeTab.FOR_YOU &&
            _forYouState.value.modules.isEmpty() &&
            !_forYouState.value.isInitialLoading
        ) {
            loadForYouContent()
        } else if (tab == HomeTab.COMMUNITY &&
            _communityState.value.cards.isEmpty() &&
            !_communityState.value.isInitialLoading
        ) {
            loadCommunityContent()
        }
    }

    fun selectTabWithoutLoadingContent(tab: HomeTab) {
        _selectedTab.value = tab
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

    fun updateTabCount(pulse: Boolean = false) {
        _tabsState.value = TabsState(WikipediaApp.instance.tabCount, pulse)
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
                isInitialLoading = isInitial,
                isLoadingMore = !isInitial,
                error = null
            )

            val age = nextCommunityAge
            val date = LocalDate.now().minusDays(nextCommunityAge.toLong())
            val content = ServiceFactory.getRest(wikiSite.value)
                .getFeedFeatured(date.year.toString(), "%02d".format(date.monthValue), "%02d".format(date.dayOfMonth), wikiSite.value.languageCode)

            // Construct Card objects based on the day's content
            val hiddenCards = Prefs.hiddenCards

            val cardsForDay = buildList<Card> {
                content.tfa?.let {
                    add(FeaturedArticleCard(it, age, wikiSite.value))
                }
                content.topRead?.let {
                    add(TopReadListCard(it, age, wikiSite.value))
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
            }.filterNot { hiddenCards.contains(it.hideKey) }.toMutableList()
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

    fun hideCommunityCard(card: Card): Int {
        addHiddenCard(card)
        val cardIndex = _communityState.value.cards.indexOf(card)
        if (cardIndex >= 0) {
            _communityState.value = _communityState.value.copy(
                cards = _communityState.value.cards - card
            )
        }
        return cardIndex
    }

    fun restoreCommunityCard(card: Card, index: Int) {
        Prefs.hiddenCards -= card.hideKey
         _communityState.value = _communityState.value.copy(
            cards = _communityState.value.cards.toMutableList().apply {
                if (index in 0..size) {
                    add(index, card)
                }
            }
        )
    }

    fun hideForYouCard(module: ForYouModule, card: ForYouCard): Int {
        addHiddenCard(card)
        val modules = _forYouState.value.modules
        val moduleIndex = modules.indexOfFirst { it.matchesIdentity(module) }
        if (moduleIndex < 0) {
            return -1
        }
        val currentModule = modules[moduleIndex]
        val cardIndex = currentModule.cards.indexOf(card)
        if (cardIndex < 0) {
            return -1
        }
        val updatedCards = currentModule.cards.toMutableList().apply {
            removeAt(cardIndex)
        }
        // If this was the last card in the module, remove the module.
        val updatedModules = modules.toMutableList().apply {
            if (updatedCards.isEmpty()) {
                removeAt(moduleIndex)
            } else {
                this[moduleIndex] = currentModule.withCards(updatedCards)
            }
        }
        _forYouState.update { it.copy(modules = updatedModules) }
        if (updatedCards.isEmpty()) {
            return moduleIndex
        }
        return cardIndex
    }

    fun restoreForYouCard(module: ForYouModule, card: ForYouCard, index: Int) {
        Prefs.hiddenCards -= card.hideKey
        val modules = _forYouState.value.modules.toMutableList()
        val moduleIndex = modules.indexOfFirst { it.matchesIdentity(module) }
        if (moduleIndex >= 0) {
            val currentModule = modules[moduleIndex]
            val insertIndex = index.coerceIn(0, currentModule.cards.size)
            val updatedCards = currentModule.cards.toMutableList().apply {
                add(insertIndex, card)
            }
            modules[moduleIndex] = currentModule.withCards(updatedCards)
        } else {
            // If the module was removed when the card was hidden, add it back in.
            val insertIndex = index.coerceIn(0, modules.size)
            modules.add(insertIndex, module.withCards(listOf(card)))
        }
        _forYouState.update { it.copy(modules = modules) }
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

    private fun addHiddenCard(card: Card) {
        val hiddenCards = Prefs.hiddenCards.toMutableList()
        if (hiddenCards.size > MAX_HIDDEN_CARDS) {
            hiddenCards.removeAt(0)
        }
        hiddenCards += card.hideKey
        Prefs.hiddenCards = hiddenCards
    }

    private suspend fun fetchForYouModules(age: Int): List<ForYouModule> {
        val modules = mutableListOf<ForYouModule>()
        val hiddenCards = Prefs.hiddenCards
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

        // --- Continue reading ---

        val continueReadingCards = buildList {
            val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, wikiSite.value.languageCode)
            if (lastReadEntries.size > age) {
                add(ContinueReadingCard(lastReadEntries[age].title, HistoryEntry.SOURCE_HISTORY))
            }
            val readingListPageTitles = AppDatabase.instance.readingListPageDao().getPagesByRandomByLang(wikiSite.value.languageCode, 10).take(3)
                .map { it.apiTitle }
                .fastJoinToString("|")
            if (readingListPageTitles.isNotEmpty()) {
                ServiceFactory.get(wikiSite.value).getInfoWithExtractsByPageTitles(readingListPageTitles)
                    .query?.pages?.forEach { page ->
                        add(ContinueReadingCard(PageTitle(
                            text = page.title,
                            wiki = wikiSite.value,
                            thumbUrl = page.thumbUrl(),
                            description = page.description,
                            displayText = page.displayTitle(wikiSite.value.languageCode),
                        ).also {
                            if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                            it.extract = page.extract
                        }, HistoryEntry.SOURCE_READING_LIST))
                    }
            }
        }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)
        if (continueReadingCards.isNotEmpty()) {
            // The index for this module is always 0 because there is always a single instance of this module, per age.
            modules.add(ForYouModule.ContinueReading(age, 0, continueReadingCards))
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

        forYouCollectionSaved = ForYouCollectionSaved(
            dateTime = LocalDateTime.now(),
            modulesPerLanguage = forYouCollectionSaved.modulesPerLanguage + (wikiSite.value.languageCode to modules)
        )
        withContext(Dispatchers.Default) {
            Prefs.homeForYouModulesToday = JsonUtil.encodeToString(forYouCollectionSaved).orEmpty()
        }
        return modules
    }

    fun refreshUnreadNotificationCount() {
        _unreadCount.update { it.copy(unreadCount = Prefs.notificationUnreadCount, canShow = AccountUtil.isLoggedIn) }
    }
}
