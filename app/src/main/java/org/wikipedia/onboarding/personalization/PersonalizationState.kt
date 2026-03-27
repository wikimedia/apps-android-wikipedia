package org.wikipedia.onboarding.personalization

import org.wikipedia.page.PageTitle

// TODO: update the states below as needed as we build out the screen
data class OnboardingCategory(
    val id: String,
    val title: String
)

data class InterestUiState(
    val categoriesState: CategoriesState = CategoriesState.Loading,
    val articlesState: ArticlesState = ArticlesState.Loading,
    val selectedCategory: String? = null,
    val selectedArticles: Set<PageTitle> = emptySet(),
    val selectionCount: Int = 0,
)

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Success(val categories: List<OnboardingCategory>) : CategoriesState
    data class Error(val message: String) : CategoriesState
}

sealed interface ArticlesState {
    data object Loading : ArticlesState
    data class Success(val articles: List<PageTitle>) : ArticlesState
    data class Error(val message: String) : ArticlesState
}
