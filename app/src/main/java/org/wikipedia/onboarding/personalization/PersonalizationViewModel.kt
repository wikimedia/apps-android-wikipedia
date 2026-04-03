package org.wikipedia.onboarding.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import kotlin.collections.plus

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
    val selectedTopics: Set<String> = emptySet(),
    val searchQuery: String = "",
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
                        it.copy(isSelected = selectedTopics.contains(it.topicId))
                    }
                )
            },
            articlesState = when {
                articlesLoading -> ArticlesState.Loading
                articlesError != null -> ArticlesState.Error(
                    articlesError.message ?: "Unknown error"
                )

                else -> ArticlesState.Success(
                    articles = articles,
                    selectedArticles = selectedArticles
                )
            }
        )
    }

    // Each screen in the personalization flow would have its own function
    // fun toFeedPreferenceUiState(): FeedPreferenceUiState { ... }
}

class PersonalizationViewModel(
    private val repository: PersonalizationRepository = PersonalizationRepository()
) : ViewModel() {
    // Single source of truth for all personalization state, can be easily extended to include feed preference and language selection states as well
    private val state = MutableStateFlow(PersonalizedViewModelState())
    private val topicApiLookUp = OnboardingTopics.all.associate { it.topicId to it.articleTopics }

    // Each screen observes only its own derived UI state
    // runs automatically when any part of the raw state changes
    val interestUiState = state
        .map { it.toInterestUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = state.value.toInterestUiState()
        )

    fun onPageChanged(page: Int) {
        when (page) {
            1 -> {
                val langCode = WikipediaApp.instance.languageState.appLanguageCode
                loadTopics(langCode)
                loadInitialArticles()
            }
        }
    }

    private fun loadTopics(langCode: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(topicsLoading = false, topicsError = throwable) }
        }) {
            state.update { it.copy(topicsLoading = true) }
            val current = state.value

            if (current.topics.isNotEmpty()) {
                state.update { it.copy(topics = current.topics, topicsLoading = false) }
                return@launch
            }

            val topics = repository.getTopics(langCode)

            state.update { it.copy(topics = topics, topicsLoading = false) }
        }
    }

    private fun loadInitialArticles() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true) }
            val current = state.value

            if (current.articles.isNotEmpty()) {
                state.update { it.copy(articles = current.articles, articlesLoading = false) }
                return@launch
            }

            val selectedItems = Prefs.recommendedReadingListInterests
            val articles = repository.loadInitialArticles(selectedItems)
            state.update { it.copy(articles = articles, articlesLoading = false, selectedArticles = selectedItems.toSet()) }
        }
    }

    private fun loadArticlesByTopics(topics: List<String>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true) }
            val current = state.value

            if (current.articles.isNotEmpty()) {
                state.update { it.copy(articles = current.articles, articlesLoading = false) }
                return@launch
            }

            val titles = repository.getArticlesBytTopic(topics)
            state.update { it.copy(articles = titles, articlesLoading = false) }
        }
    }

    // as we have a single state it becomes easier to update and control the state
    fun onTopicSelected(topic: OnboardingTopic) {
        // When a category is selected, we want to reset the articles state and load articles for the selected category
        val selectedTopics = if (state.value.selectedTopics.contains(topic.topicId)) {
            state.value.selectedTopics - topic.topicId
        } else {
            state.value.selectedTopics + topic.topicId
        }

        state.update {
            it.copy(
                selectedTopics = selectedTopics,
                articles = emptyList(),
                articlesLoading = true,
                articlesError = null
            )
        }

        val topicQueryIds = selectedTopics.mapNotNull { topicApiLookUp[it] }
        loadArticlesByTopics(topics = topicQueryIds.toList())
    }

    fun addArticle(title: PageTitle) {
        state.update {
            val newItems = listOf(title) + it.articles
            val newSelection = it.selectedArticles + title
            it.copy(articles = newItems, selectedArticles = newSelection)
        }
    }

    fun toggleSelection(title: PageTitle) {
        state.update {
            val newSelection = if (it.selectedArticles.contains(title)) {
                it.selectedArticles - title
            } else {
                it.selectedArticles + title
            }
            it.copy(selectedArticles = newSelection)
        }
    }

    fun deselectAllArticles() {
        state.update {
            it.copy(
                selectedArticles = emptySet(),
                articlesLoading = false,
                articlesError = null
            )
        }
    }
}
