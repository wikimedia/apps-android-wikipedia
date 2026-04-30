package org.wikipedia.feed

import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.continuereading.ContinueReadingCard
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.interests.BasedOnInterestCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.time.LocalDate

enum class HomeTab { COMMUNITY, FOR_YOU }

sealed class ForYouModule {
    abstract val age: Int
    abstract val index: Int
    abstract val cards: List<Card>

    abstract fun withCards(cards: List<Card>): ForYouModule

    fun matchesIdentity(other: ForYouModule): Boolean {
        return this::class == other::class && age == other.age && index == other.index
    }

    data class BasedOnInterest(
        override val age: Int,
        override val index: Int,
        override val cards: List<Card>
    ) : ForYouModule() {
        override fun withCards(cards: List<Card>): ForYouModule = copy(cards = cards)
    }

    data class ContinueReading(
        override val age: Int,
        override val index: Int,
        override val cards: List<Card>
    ) : ForYouModule() {
        override fun withCards(cards: List<Card>): ForYouModule = copy(cards = cards)
    }
}

data class CommunityContentState(
    val cards: List<Card> = emptyList(),
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
                content.potd?.let {
                    add(FeaturedImageCard(it, age, wikiSite.value))
                }
                if (!content.news.isNullOrEmpty()) {
                    add(NewsCard(content.news, age, wikiSite.value))
                }
                if (!content.onthisday.isNullOrEmpty()) {
                    add(OnThisDayCard(content.onthisday.take(2), age, wikiSite.value))
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

    fun hideCard(card: Card): Int {
        Prefs.hiddenCards += card.hideKey
        val cardIndex = _communityState.value.cards.indexOf(card)
        if (cardIndex >= 0) {
            _communityState.value = _communityState.value.copy(
                cards = _communityState.value.cards - card
            )
        }
        return cardIndex
    }

    fun restoreCard(card: Card, index: Int) {
        Prefs.hiddenCards -= card.hideKey
         _communityState.value = _communityState.value.copy(
            cards = _communityState.value.cards.toMutableList().apply {
                if (index in 0..size) {
                    add(index, card)
                }
            }
        )
    }

    fun hideCard(module: ForYouModule, card: Card): Int {
        Prefs.hiddenCards += card.hideKey
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

    fun restoreCard(module: ForYouModule, card: Card, index: Int) {
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

    private suspend fun fetchForYouModules(age: Int): List<ForYouModule> {
        val modules = mutableListOf<ForYouModule>()
        val hiddenCards = Prefs.hiddenCards

        // --- Interests ---

        val interestTopics = AppDatabase.instance.topicInterestDao().getAll().distinctBy { it.topicId }
        interestTopics.forEachIndexed { index, topic ->
            val entries = ServiceFactory.get(wikiSite.value).getArticlesByTopic("articletopic:" + topic.queryTopicId + "^90", 10)
                .query?.pages?.sortedBy { it.index }?.map { page ->
                    val pageTitle = PageTitle(
                        text = page.title,
                        wiki = wikiSite.value,
                        thumbUrl = page.thumbUrl(),
                        description = page.description,
                        displayText = page.displayTitle(wikiSite.value.languageCode),
                    ).also {
                        if (!page.sectionTitle.isNullOrEmpty()) it.fragment = StringUtil.addUnderscores(page.sectionTitle)
                        it.extract = page.extract
                    }
                    HistoryEntry(pageTitle, HistoryEntry.SOURCE_FEED_INTERESTS)
                }.orEmpty().map {
                    // TODO: filter items that have already been suggested.
                    BasedOnInterestCard(it, interestTopic = topic)
                }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)

                if (entries.isNotEmpty()) {
                    modules.add(ForYouModule.BasedOnInterest(age, index, entries))
                }
        }

        // --- Continue reading ---

        val continueReadingCards = buildList {
            val lastReadEntries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age + 1, 30, wikiSite.value.languageCode)
            if (lastReadEntries.size > age) {
                add(ContinueReadingCard(lastReadEntries[age].also { it.source = HistoryEntry.SOURCE_HISTORY }))
            }
            val readingListPages = AppDatabase.instance.readingListPageDao().getPagesByRandomByLang(wikiSite.value.languageCode, 10).map {
                HistoryEntry(ReadingListPage.toPageTitle(it), HistoryEntry.SOURCE_READING_LIST)
            }
            addAll(readingListPages.map { ContinueReadingCard(it) })
        }.filterNot { hiddenCards.contains(it.hideKey) }.take(4)

        if (continueReadingCards.isNotEmpty()) {
            ServiceFactory.get(wikiSite.value).getInfoWithExtractsByPageTitles(continueReadingCards.map { it.entry.apiTitle }.fastJoinToString("||"))
                .query?.pages?.forEach { page ->
                    continueReadingCards.find {
                        StringUtil.addUnderscores(it.entry.apiTitle) == StringUtil.addUnderscores(page.title) ||
                                StringUtil.addUnderscores(it.entry.apiTitle) == StringUtil.addUnderscores(page.redirectFrom)
                    }?.let {
                        it.entry.title.extract = page.extract
                        it.entry.title.description = page.description
                        it.entry.title.thumbUrl = page.thumbUrl()
                    }
                }
            modules.add(ForYouModule.ContinueReading(age, 0, continueReadingCards))
        }

        return modules
    }
}
