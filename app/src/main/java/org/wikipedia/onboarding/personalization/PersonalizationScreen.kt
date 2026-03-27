package org.wikipedia.onboarding.personalization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

// TODO: probably renaming the screen name
@Composable
fun PersonalizationScreen(
    modifier: Modifier = Modifier,
    onSkipClick: () -> Unit,
    viewModel: PersonalizationViewModel = viewModel()
) {
    val uiState = viewModel.interestUiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Scaffold(
        bottomBar = {
                OnboardingBottomBar(
                    pagerState = pagerState,
                    onNavigationRightClick = { /*TODO*/ },
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
            ) { page ->
                when (page) {
                    0 -> OnboardingCuriosityScreen(modifier = Modifier.fillMaxWidth())
                    1 -> {
                        InterestOnboardingScreen(
                            categoriesState = uiState.value.categoriesState,
                            articlesState = uiState.value.articlesState,
                            modifier = Modifier,
                            onCategorySelected = {
                                viewModel.onCategorySelected(it)
                            }
                        )
                    }
                    2 -> {}
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

@Preview
@Composable
private fun PersonalizationScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        PersonalizationScreen(
            onSkipClick = {}
        )
    }
}
