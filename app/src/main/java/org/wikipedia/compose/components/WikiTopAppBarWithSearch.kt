package org.wikipedia.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.wikipedia.compose.theme.WikipediaTheme

enum class TopAppBarState {
    SEARCH,
    NORMAL
}

@Composable
fun WikiTopAppBarWithSearch(
    appBarTitle: String,
    placeHolderTitle: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackButtonClick: () -> Unit,
) {
    var currentState by remember { mutableStateOf(TopAppBarState.NORMAL) }

    // Simple animation when switching states
    val enterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        )
    )

    val exitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        )
    )

    AnimatedContent(
        targetState = currentState,
        transitionSpec = {
            enterTransition.togetherWith(exitTransition)
        },
        content = { state ->
            when (state) {
                TopAppBarState.SEARCH -> {
                    SearchTopAppBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = {
                            onSearchQueryChange(it)
                        },
                        placeHolderTitle = placeHolderTitle,
                        onBackButtonClick = {
                            onSearchQueryChange("")
                            currentState = TopAppBarState.NORMAL
                        }
                    )
                }
                TopAppBarState.NORMAL -> {
                    WikiTopAppBar(
                        title = appBarTitle,
                        onNavigationClick = onBackButtonClick,
                        actions = {
                            IconButton(
                                onClick = {
                                    currentState = TopAppBarState.SEARCH
                                },
                                content = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = WikipediaTheme.colors.primaryColor
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    )
}
