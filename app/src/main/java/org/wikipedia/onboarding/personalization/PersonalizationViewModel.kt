package org.wikipedia.onboarding.personalization

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

// TODO: remove comments once reviewed
// this is a is a raw, flat, internal representation of ALL state
// needed across the personalization flow (interest, feed preference, language screens)
// this enables SINGLE SOURCE OF TRUTH — one place to update, no risk of states going out of sync
// DERIVED UI STATES — each screen gets its own UI state derived from a function like toInterestUIState()
// instead of maintaining separate StateFlows per screen or one giant combined UI state
private data class PersonalizedViewModelState(
    // Interest screen
    val categories: List<OnboardingCategory> = emptyList(),
    val categoriesLoading: Boolean = false,
    val categoriesError: Throwable? = null,
    val selectedCategoryId: String? = null,
    val articles: List<PageTitle> = emptyList(),
    val articlesLoading: Boolean = false,
    val articlesError: Throwable? = null,
    val selectedArticleIds: Set<PageTitle> = emptySet(),
    val searchQuery: String = "",
    // Feed preference screen properties
    // Language screen properties
) {
    fun toInterestUiState(): InterestUiState {
        return InterestUiState(
            categoriesState = when {
                categoriesLoading -> CategoriesState.Loading
                categoriesError != null -> CategoriesState.Error(
                    categoriesError.message ?: "Unknown error"
                )

                else -> CategoriesState.Success(categories)
            },
            articlesState = when {
                articlesLoading -> ArticlesState.Loading
                articlesError != null -> ArticlesState.Error(
                    articlesError.message ?: "Unknown error"
                )

                else -> ArticlesState.Success(articles)
            },
            selectedCategory = selectedCategoryId,
            selectedArticles = selectedArticleIds,
            selectionCount = selectedArticleIds.size
        )
    }

    // Each screen in the personalization flow would have its own function
    // fun toFeedPreferenceUiState(): FeedPreferenceUiState { ... }
    // fun toLanguageUiState(): LanguageUiState { ... }
}

class PersonalizationViewModel : ViewModel() {
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

    fun onPageChanged(page: Int) {
        when (page) {
            1 -> {
                loadCategories()
                loadArticlesForCategory("")
            }
        }
    }

    // TODO: add actual api call
    private fun loadCategories() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(categoriesLoading = false, categoriesError = throwable) }
        }) {
            state.update { it.copy(categoriesLoading = true) }
            val current = state.value
            if (current.categories.isNotEmpty()) return@launch

            println("orange loading categories...")
            delay(5000) // simulate network delay
            val categories = listOf(
                OnboardingCategory("1", "Science"),
                OnboardingCategory("2", "History"),
                OnboardingCategory("3", "Art")
            )
            state.update { it.copy(categories = categories, categoriesLoading = false) }
        }
    }

    // TODO: add actual api call
    private fun loadArticlesForCategory(categoryId: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            state.update { it.copy(articlesLoading = false, articlesError = throwable) }
        }) {
            state.update { it.copy(articlesLoading = true) }
            val current = state.value
            if (current.articles.isNotEmpty()) return@launch
            println("orange loading articles for category $categoryId...")
            delay(5000) // simulate network delay
            val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
            val titles = listOf(
                PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
                PageTitle(text = "Industrial design", wiki = site, thumbUrl = "foo.jpg", description = "Process of design applied to physical products", displayText = null),
                PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
                PageTitle(text = "Sample title without description", wiki = site, thumbUrl = "foo.jpg", description = "", displayText = null),
                PageTitle(text = "Sample title without thumbnail", wiki = site, thumbUrl = "", description = "Sample description", displayText = null),
                PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null),
                PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
            )
            state.update { it.copy(articles = titles, articlesLoading = false) }
        }
    }

    // as we have a single state it becomes easier to update and control the state
    fun onCategorySelected(category: OnboardingCategory) {
        // When a category is selected, we want to reset the articles state and load articles for the selected category
        state.update {
            it.copy(
                selectedCategoryId = category.id,
                articles = emptyList(),
                articlesLoading = true,
                articlesError = null
            )
        }
        loadArticlesForCategory(category.id)
    }
}
