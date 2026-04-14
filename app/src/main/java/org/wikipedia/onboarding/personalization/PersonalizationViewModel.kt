package org.wikipedia.onboarding.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

// this is a raw, flat, internal representation of ALL state
// needed across the personalization flow (interest and feed preference)
// this enables SINGLE SOURCE OF TRUTH — one place to update, no risk of states going out of sync
// DERIVED UI STATES — each screen gets its own UI state derived from a function like toInterestUIState()
// instead of maintaining separate StateFlows per screen or one giant combined UI state
private data class PersonalizedViewModelState(
    // Interest screen
    val topics: List<OnboardingTopic> = emptyList(),
    val topicsLoading: Boolean = false,
    val topicsError: Throwable? = null,
    val articles: List<PageTitle> = emptyList(),
    val articlesLoading: Boolean = false,
    val articlesError: Throwable? = null,
    val selectedArticles: Set<PageTitle> = emptySet(),
    val selectedTopics: List<OnboardingTopic> = emptyList(),
    // Feed preference screen properties
    val feedPreferenceType: FeedPreferenceType = FeedPreferenceType.COMMUNITY,
    val communityContent: List<FeedPreferenceContent> = emptyList(),
    val communityLoading: Boolean = false,
    val communityError: Throwable? = null,
    val personalizedContent: List<FeedPreferenceContent> = emptyList(),
    val personalizedLoading: Boolean = false,
    val personalizedError: Throwable? = null
) {
    fun toInterestUiState(): InterestUiState {
        return InterestUiState(
            topicsState = when {
                topicsLoading -> TopicsState.Loading
                topicsError != null -> TopicsState.Error(topicsError)

                else -> TopicsState.Success(
                    topics = topics.map {
                        it.copy(isSelected = selectedTopics.any { selected -> selected.topicId == it.topicId })
                    }
                )
            },
            articlesState = when {
                articlesLoading -> ArticlesState.Loading
                articlesError != null -> ArticlesState.Error(articlesError)

                else -> ArticlesState.Success(
                    articles = articles,
                    selectedArticles = selectedArticles
                )
            },
            totalSelectedCount = selectedTopics.size + selectedArticles.size
        )
    }

    fun toFeedPreferenceUiState(): FeedPreferenceUiState {
        return FeedPreferenceUiState(
            selectedType = feedPreferenceType,
            communityState = when {
                communityLoading -> FeedContentState.Loading
                communityError != null -> FeedContentState.Error(communityError)
                else -> FeedContentState.Success(communityContent)
            },
            personalizedState = when {
                personalizedLoading -> FeedContentState.Loading
                personalizedError != null -> FeedContentState.Error(personalizedError)
                else -> FeedContentState.Success(personalizedContent)
            }
        )
    }
}

class PersonalizationViewModel(
    private val repository: PersonalizationRepository
) : ViewModel() {
    // Single source of truth for all personalization state, can be easily extended to include feed preference and language selection states as well
    private val state = MutableStateFlow(PersonalizedViewModelState())
    private var articlesJob: Job? = null

    // Each screen observes only its own derived UI state
    // runs automatically when any part of the raw state changes
    val interestUiState = state
        .map { it.toInterestUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = state.value.toInterestUiState()
        )

    val feedPreferenceUiState = state
        .map { it.toFeedPreferenceUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = state.value.toFeedPreferenceUiState()
        )

    fun onPageChanged(screen: PersonalizationPage) {
        when (screen) {
            PersonalizationPage.INTERESTS -> loadScreen()
            else -> {}
        }
    }

    private fun loadScreen() {
        viewModelScope.launch( CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
         }) {
            loadTopics()
            initialize()
        }
    }

    private suspend fun loadTopics() {
        if (state.value.topics.isNotEmpty()) return

        runCatching {
            state.update { it.copy(topicsLoading = true, topicsError = null) }

            val langCode = repository.wikiSite.languageCode
            val topics = repository.getTopics(langCode)

            state.update { it.copy(topics = topics, topicsLoading = false) }
        }.onFailure { throwable ->
            state.update { it.copy(topicsLoading = false, topicsError = throwable) }
        }
    }

    private suspend fun initialize() {
        runCatching {
            val langCode = repository.wikiSite.languageCode
            // check db for persisted interest (topic and articles) data
            val persistedTopics = repository.getPersistedTopics(langCode)
            val persistedArticles = repository.getPersistedArticles(langCode)

            val hasPersistedData = persistedTopics.isNotEmpty() || persistedArticles.isNotEmpty()
            if (!hasPersistedData && state.value.articles.isEmpty()) {
                loadInitialArticles()
                return@runCatching
            }

            // restore selections
            state.update { current ->
                current.copy(
                    selectedTopics = persistedTopics,
                    selectedArticles = persistedArticles.toSet()
                )
            }

            val lasTopic = persistedTopics.lastOrNull()
            if (lasTopic != null) {
                loadArticlesByTopic(topic = lasTopic)
            } else {
                loadInitialArticles()
            }
        }.onFailure { throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }
    }

    private fun loadInitialArticles() {
        if (state.value.articles.isNotEmpty()) return
        articlesJob?.cancel()

        articlesJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true, articlesError = null) }

            val selectedItems = Prefs.recommendedReadingListInterests
            val articles = repository.loadInitialArticles(selectedItems)
            state.update { current ->
                val newArticles = (current.selectedArticles + articles).distinct()
                current.copy(
                    articles = newArticles,
                    articlesLoading = false
                )
            }
        }
    }

    private fun loadArticlesByTopic(topic: OnboardingTopic) {
        if (state.value.articles.isNotEmpty()) return
        articlesJob?.cancel()

        articlesJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true, articlesError = null) }

            val articles = repository.getArticlesByTopic(topic.queryTopicId)
            state.update { current ->
                val newArticles = (current.selectedArticles.toList() + articles).distinct()
                current.copy(articles = newArticles, articlesLoading = false)
            }
        }
    }

    // as we have a single state it becomes easier to update and control the state
    fun onTopicSelected(topic: OnboardingTopic) {
        val lang = repository.wikiSite.languageCode

        // When a category is selected, we want to reset the articles state and load articles for the selected category
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(topicsError = throwable) }
        }) {
            val currentTopics = state.value.selectedTopics
            val isSelected = currentTopics.any { selected -> selected.topicId == topic.topicId }

            val selectedTopics = if (isSelected) {
                currentTopics.filter { it.topicId != topic.topicId }
            } else {
                currentTopics + topic
            }

            if (isSelected) {
                repository.deleteTopic(topic, lang)
            } else {
                repository.saveTopic(topic, lang)
            }

            state.update { current ->
                current.copy(
                    selectedTopics = selectedTopics,
                    articles = emptyList(),
                    articlesError = null
                )
            }

            val lastSelectedTopic = selectedTopics.lastOrNull()
            if (lastSelectedTopic == null) loadInitialArticles() else loadArticlesByTopic(topic = lastSelectedTopic)
        }
    }

    fun addArticleFromSearch(title: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                state.update { it.copy(articlesError = throwable) }
            }
        ) {
            repository.saveArticle(title, repository.wikiSite.languageCode, null)
            state.update {
                val newItems = listOf(title) + it.articles
                val newSelection = it.selectedArticles + title
                it.copy(articles = newItems, selectedArticles = newSelection)
            }
        }
    }

    fun toggleArticleSelection(title: PageTitle) {
        val lang = repository.wikiSite.languageCode

        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesError = throwable) }
        }) {
            val current = state.value
            val isSelected = current.selectedArticles.contains(title)
            val currentSelectedTopic = current.selectedTopics.lastOrNull()

            if (isSelected) {
                repository.deleteArticle(title, lang, currentSelectedTopic)
            } else {
                repository.saveArticle(title, lang, currentSelectedTopic)
            }

            state.update { currentState ->
                currentState.copy(
                    selectedArticles = if (isSelected) {
                        currentState.selectedArticles - title
                    } else {
                        currentState.selectedArticles + title
                    }
                )
            }
        }
    }

    fun deselectAllArticles() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                state.update { it.copy(articlesError = throwable) }
            }
        ) {
            repository.deleteAllInterests()

            state.update {
                it.copy(
                    selectedArticles = emptySet(),
                    selectedTopics = emptyList(),
                    articlesLoading = false,
                    articlesError = null
                )
            }
        }
    }

    fun retryLoading() {
        val last = state.value.selectedTopics.lastOrNull()
        if (last != null) {
            loadArticlesByTopic(topic = last)
        } else {
            loadInitialArticles()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PersonalizationViewModel(
                    repository = PersonalizationRepository(
                        interestTopicDao = AppDatabase.instance.topicInterestDao(),
                        interestArticleDao = AppDatabase.instance.articleInterestDao(),
                        historyEntryWithImageDao = AppDatabase.instance.historyEntryWithImageDao(),
                        readingListPageDao = AppDatabase.instance.readingListPageDao(),
                        wikiSite = WikipediaApp.instance.wikiSite
                    )
                )
            }
        }
    }
}
