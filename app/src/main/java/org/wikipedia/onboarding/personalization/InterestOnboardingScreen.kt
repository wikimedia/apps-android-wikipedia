package org.wikipedia.onboarding.personalization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun InterestOnboardingScreen(
    modifier: Modifier = Modifier,
    categoriesState: CategoriesState,
    articlesState: ArticlesState
) {
    Scaffold(
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (categoriesState) {
                is CategoriesState.Error -> {}
                CategoriesState.Loading -> {}
                is CategoriesState.Success -> {
                    categoriesState.categories.forEach {
                        Text(text = it.title)
                    }
                }
            }

            when (articlesState) {
                is ArticlesState.Error -> {}
                ArticlesState.Loading -> {}
                is ArticlesState.Success -> {
                    articlesState.articles.forEach {
                        Text(text = it.displayText)
                    }
                }
            }
        }
    }
}
