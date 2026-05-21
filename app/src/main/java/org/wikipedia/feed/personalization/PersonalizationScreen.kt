package org.wikipedia.feed.personalization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.instrument
import org.wikipedia.feed.personalization.homepreference.HomePreferenceScreen
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.personalization.interest.InterestOnboardingScreen

@Composable
fun PersonalizationScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalizationViewModel,
    screens: List<PersonalizationPage>,
    onSkipClick: () -> Unit,
    onCompleteOnboardingClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBackButtonClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interestUiState = viewModel.interestUiState.collectAsState()
    val feedPreferenceUiState = viewModel.feedPreferenceUiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val pageActionSource = mapOf(
        PersonalizationPage.CURIOSITY to "feed_entry",
        PersonalizationPage.INTERESTS to "feed_customize",
        PersonalizationPage.HOME_PREFERENCE to "feed_order_customize"
    )

    LaunchedEffect(pagerState.currentPage) {
        context.instrument?.submitInteraction("impression", actionSource = pageActionSource[screens[pagerState.currentPage]])
        viewModel.onPageChanged(screens[pagerState.currentPage])
    }

    Scaffold(
        bottomBar = {
                OnboardingBottomBar(
                    pagerState = pagerState,
                    onNavigationRightClick = {
                        context.instrument?.submitInteraction(
                            "click",
                            elementId = "next_button",
                            actionSource = pageActionSource[screens[pagerState.currentPage]]
                        )
                        coroutineScope.launch {
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onCompleteOnboardingClick()
                            }
                        }
                    },
                    onSkipClick = {
                        context.instrument?.submitInteraction(
                            "click",
                            elementId = "skip_button",
                            actionSource = pageActionSource[screens[pagerState.currentPage]]
                        )
                        onSkipClick()
                    }
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
                            topicsList = interestUiState.value.topicsList,
                            articlesState = interestUiState.value.articlesState,
                            totalSelectedCount = interestUiState.value.totalSelectedCount,
                            languageCode = interestUiState.value.languageCode,
                            onTopicSelected = {
                                context.instrument?.submitInteraction("click", actionSource = pageActionSource[screens[pagerState.currentPage]], elementId = "topic_select")
                                viewModel.onTopicSelected(it)
                            },
                            onItemClick = {
                                context.instrument?.submitInteraction("click", actionSource = pageActionSource[screens[pagerState.currentPage]], elementId = "article_select")
                                viewModel.toggleArticleSelection(it)
                            },
                            onSearchClick = onSearchClick,
                            onDeselectAllClick = {
                                context.instrument?.submitInteraction("click", actionSource = pageActionSource[screens[pagerState.currentPage]], elementId = "deselect_all")
                                viewModel.deselectAllInterests()
                            },
                            retryLoading = {
                                context.instrument?.submitInteraction("click", actionSource = pageActionSource[screens[pagerState.currentPage]], elementId = "retry")
                                viewModel.retryInterestsLoading()
                            },
                            onBackButtonClick = onBackButtonClick
                        )
                    }
                    PersonalizationPage.HOME_PREFERENCE -> {
                        HomePreferenceScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WikipediaTheme.colors.paperColor)
                                .padding(top = 40.dp),
                            selectedType = feedPreferenceUiState.value.selectedType,
                            communityContentState = feedPreferenceUiState.value.communityState,
                            personalizedContentState = feedPreferenceUiState.value.personalizedState,
                            onTypeSelected = {
                                context.instrument?.submitInteraction(
                                    "click",
                                    actionSource = pageActionSource[screens[pagerState.currentPage]],
                                    elementId = if (it == HomePreferenceType.COMMUNITY) "community_first" else "for_you_first")
                                viewModel.onFeedPreferenceTypeSelected(it)
                            },
                            onRetryClick = {
                                context.instrument?.submitInteraction("click", actionSource = pageActionSource[screens[pagerState.currentPage]], elementId = "retry")
                                viewModel.retryFeedPreferenceLoading(it)
                            }
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
            if (pagerState.pageCount > 1) {
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
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

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
