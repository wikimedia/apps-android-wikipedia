package org.wikipedia.onboarding.personalization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.WikipediaTheme

// TODO: add actual UI
@Composable
fun InterestOnboardingScreen(
    modifier: Modifier = Modifier,
    categoriesState: CategoriesState,
    articlesState: ArticlesState,
    onCategorySelected: (OnboardingCategory) -> Unit
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
                CategoriesState.Loading -> {
                    Text("Loading categories...")
                }
                is CategoriesState.Success -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriesState.categories) {
                            Button(
                                onClick = { onCategorySelected(it) }
                            ) {
                                Text(text = it.title)
                            }
                        }
                    }
                }
            }

            when (articlesState) {
                is ArticlesState.Error -> {}
                ArticlesState.Loading -> {
                    Text("Loading articles...")
                }
                is ArticlesState.Success -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        articlesState.articles.forEach {
                            Text(text = it.displayText)
                        }
                    }
                }
            }
        }
    }
}
