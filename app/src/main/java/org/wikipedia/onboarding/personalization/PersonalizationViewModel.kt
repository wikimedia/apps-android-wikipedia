package org.wikipedia.onboarding.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineExceptionHandler
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
    val selectedTopics: List<OnboardingTopic> = emptyList()
    // Feed preference screen properties
) {
    fun toInterestUiState(): InterestUiState {
        return InterestUiState(
            topicsState = when {
                topicsLoading -> TopicsState.Loading
                topicsError != null -> TopicsState.Error(
                    topicsError.message ?: "Unknown error"
                )

                else -> TopicsState.Success(
                    topics = topics.map {
                        it.copy(isSelected = selectedTopics.any { selected -> selected.topicId == it.topicId })
                    }
                )
            },
            articlesState = when {
                articlesLoading -> ArticlesState.Loading
                articlesError != null -> ArticlesState.Error(
                    articlesError
                )

                else -> ArticlesState.Success(
                    articles = articles,
                    selectedArticles = selectedArticles
                )
            },
            totalSelectedCount = selectedTopics.size + selectedArticles.size
        )
    }

    // Each screen in the personalization flow would have its own function
    // fun toFeedPreferenceUiState(): FeedPreferenceUiState { ... }
}

class PersonalizationViewModel(
    private val repository: PersonalizationRepository
) : ViewModel() {
    // Single source of truth for all personalization state, can be easily extended to include feed preference and language selection states as well
    private val state = MutableStateFlow(PersonalizedViewModelState())

    // Each screen observes only its own derived UI state
    // runs automatically when any part of the raw state changes
    val interestUiState = state
        .map { it.toInterestUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = state.value.toInterestUiState()
        )

    fun onPageChanged(screen: PersonalizationPage) {
        when (screen) {
            PersonalizationPage.INTERESTS -> {
                if (state.value.topics.isEmpty()) loadTopics(repository.wikiSite.languageCode)
                if (state.value.articles.isEmpty()) loadInitialArticles()
            }
            else -> {}
        }
    }

    private fun loadTopics(langCode: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(topicsLoading = false, topicsError = throwable) }
        }) {
            state.update { it.copy(topicsLoading = true, topicsError = null) }

            val topics = repository.getTopics(langCode)
            state.update { it.copy(topics = topics, topicsLoading = false) }
        }
    }

    private fun loadInitialArticles() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true, articlesError = null) }

            val selectedItems = Prefs.recommendedReadingListInterests
            val articles = repository.loadInitialArticles(selectedItems)
            state.update {
                it.copy(
                    articles = articles,
                    articlesLoading = false,
                    selectedArticles = selectedItems.toSet()
                )
            }
        }
    }

    private fun loadArticlesByTopic(topic: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true, articlesError = null) }

            val articles = repository.getArticlesByTopic(topic)
            state.update { current ->
                val newArticles = (current.selectedArticles.toList() + articles).distinct()
                current.copy(articles = newArticles, articlesLoading = false)
            }
        }
    }

    // as we have a single state it becomes easier to update and control the state
    fun onTopicSelected(topic: OnboardingTopic) {
        val lang = repository.wikiSite.languageCode
        val isSelected = state.value.selectedTopics.any { selected -> selected.topicId == topic.topicId }

        // When a category is selected, we want to reset the articles state and load articles for the selected category
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(topicsError = throwable) }
        }) {
            val selectedTopics = if (isSelected) {
                state.value.selectedTopics.filter { it.topicId != topic.topicId }
            } else {
                state.value.selectedTopics + topic
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
                    articlesLoading = true,
                    articlesError = null
                )
            }

            val topicQueryId = selectedTopics.lastOrNull()?.queryTopicId
            if (topicQueryId == null) loadInitialArticles() else loadArticlesByTopic(topic = topicQueryId)
        }
    }

    fun addArticleFromSearch(title: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                state.update { it.copy(articlesError = throwable) }
            }
        ) {
            repository.saveArticle(title, repository.wikiSite.languageCode)
            state.update {
                val newItems = listOf(title) + it.articles
                val newSelection = it.selectedArticles + title
                it.copy(articles = newItems, selectedArticles = newSelection)
            }
        }
    }

    fun toggleSelection(title: PageTitle) {
        val lang = repository.wikiSite.languageCode
        val isSelected = state.value.selectedArticles.contains(title)
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesError = throwable) }
        }) {
            if (isSelected) {
                repository.deleteArticle(title, lang)
            } else {
                repository.saveArticle(title, lang)
            }

            state.update { current ->
                current.copy(
                    selectedArticles = if (isSelected) {
                        current.selectedArticles - title
                    } else {
                        current.selectedArticles + title
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
            repository.deleteAllTopics()
            repository.deleteAllArticles()

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
            loadArticlesByTopic(topic = last.queryTopicId)
        } else {
            loadInitialArticles()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PersonalizationViewModel(
                    repository = PersonalizationRepository(
                        topicInterestDao = AppDatabase.instance.topicInterestDao(),
                        articleInterestDao = AppDatabase.instance.articleInterestDao(),
                        historyEntryWithImageDao = AppDatabase.instance.historyEntryWithImageDao(),
                        readingListPageDao = AppDatabase.instance.readingListPageDao(),
                        wikiSite = WikipediaApp.instance.wikiSite
                    )
                )
            }
        }
    }
}
