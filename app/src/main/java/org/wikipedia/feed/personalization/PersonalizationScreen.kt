package org.wikipedia.feed.personalization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.feed.personalization.homepreference.FeedPreferenceScreen
import org.wikipedia.feed.personalization.interest.InterestOnboardingScreen

// TODO: probably renaming the screen name
@Composable
fun PersonalizationScreen(
    modifier: Modifier = Modifier,
    screens: List<PersonalizationPage>,
    onSkipClick: () -> Unit,
    onCompleteOnboardingClick: () -> Unit,
    onSearchClick: () -> Unit,
    showError: (Throwable) -> Unit,
    viewModel: PersonalizationViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val interestUiState = viewModel.interestUiState.collectAsState()
    val feedPreferenceUiState = viewModel.feedPreferenceUiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { screens.size })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(screens[pagerState.currentPage])
    }

    Scaffold(
        bottomBar = {
                OnboardingBottomBar(
                    pagerState = pagerState,
                    onNavigationRightClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onCompleteOnboardingClick()
                            }
                        }
                    },
                    onSkipClick = onSkipClick
                )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState
            ) { pageIndex ->
                when (screens[pageIndex]) {
                    PersonalizationPage.CURIOSITY -> {
                        OnboardingCuriosityScreen(modifier = Modifier.fillMaxWidth())
                    }
                    PersonalizationPage.INTERESTS -> {
                        InterestOnboardingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WikipediaTheme.colors.paperColor)
                                .padding(top = 40.dp),
                            topicsState = interestUiState.value.topicsState,
                            articlesState = interestUiState.value.articlesState,
                            totalSelectedCount = interestUiState.value.totalSelectedCount,
                            onTopicSelected = {
                                viewModel.onTopicSelected(it)
                            },
                            onItemClick = {
                                viewModel.toggleArticleSelection(it)
                            },
                            onSearchClick = onSearchClick,
                            onDeselectAllClick = {
                                viewModel.deselectAllArticles()
                            },
                            retryLoading = {
                                viewModel.retryInterestsLoading()
                            },
                            showError = showError
                        )
                    }
                    PersonalizationPage.FEED_PREFERENCE -> {
                        FeedPreferenceScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WikipediaTheme.colors.paperColor)
                                .padding(top = 40.dp),
                            selectedType = feedPreferenceUiState.value.selectedType,
                            communityContentState = feedPreferenceUiState.value.communityState,
                            personalizedContentState = feedPreferenceUiState.value.personalizedState,
                            onTypeSelected = { viewModel.onFeedPreferenceTypeSelected(it) },
                            onRetryClick = { viewModel.retryFeedPreferenceLoading(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingBottomBar(
    pagerState: PagerState,
    onNavigationRightClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Column {
        HorizontalDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(),
            color = WikipediaTheme.colors.borderColor
        )

        Row(
            modifier = Modifier
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onSkipClick() },
                modifier = Modifier
                    .wrapContentWidth(Alignment.Start)
                    .wrapContentHeight(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_skip),
                    color = WikipediaTheme.colors.placeholderColor
                )
            }

            PageIndicator(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(Alignment.CenterVertically),
                pagerState = pagerState
            )

            IconButton(
                onClick = { onNavigationRightClick() },
                modifier = Modifier
                    .wrapContentWidth(Alignment.End)
                    .wrapContentHeight(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward_black_24dp),
                    contentDescription = stringResource(id = R.string.onboarding_next),
                    tint = WikipediaTheme.colors.progressiveColor
                )
            }
        }
    }
}
