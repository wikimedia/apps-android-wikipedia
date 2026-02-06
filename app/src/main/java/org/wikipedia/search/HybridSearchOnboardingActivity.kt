package org.wikipedia.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_9
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.UriUtil

private val onboardingItems = listOf(
    OnboardingItem(
        icon = R.drawable.ic_chat_bubble_outline,
        title = R.string.hybrid_search_onboarding_search_title,
        subTitle = R.string.hybrid_search_onboarding_search_description
    ),
    OnboardingItem(
        icon = R.drawable.ic_timer_black_24dp,
        title = R.string.hybrid_search_onboarding_opt_in_choice_title,
        subTitle = R.string.hybrid_search_onboarding_opt_in_choice_description
    )
)

private val defaultSearchQueries = listOf(
    R.string.hybrid_search_onboarding_search_example_query_pluto_as_planet,
    R.string.hybrid_search_onboarding_search_example_query_first_olympics,
    R.string.hybrid_search_onboarding_search_example_query_rna_vs_dna,
    R.string.hybrid_search_onboarding_search_example_query_pineapple_pizza,
    R.string.hybrid_search_onboarding_search_example_query_biggest_cities_europe
)

class HybridSearchOnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        Prefs.isHybridSearchEnabled = true
        val source = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as? Constants.InvokeSource

        setContent {
            BaseTheme {
                HybridSearchOnboardingScreen(
                    onGetStarted = { exampleQuery ->
                        if (exampleQuery == null) {
                            val intent = SearchActivity.newIntent(
                                context = this,
                                source = source ?: Constants.InvokeSource.NAV_MENU,
                                query = null
                            )
                            if (!Prefs.isHybridSearchEnabled) {
                                intent.putExtra(
                                    SearchActivity.EXTRA_SHOW_SNACKBAR_MESSAGE,
                                    R.string.hybrid_search_onboarding_opt_out_toast_message
                                )
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            startActivity(SearchActivity.newIntent(
                                context = this,
                                source = source ?: Constants.InvokeSource.NAV_MENU,
                                query = exampleQuery,
                                showHybridSearch = true
                            ))
                            finish()
                        }
                    },
                    onLearnMoreClick = {
                        UriUtil.visitInExternalBrowser(this, getString(R.string.hybrid_search_info_link).toUri())
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context, source: Constants.InvokeSource): Intent {
            return Intent(context, HybridSearchOnboardingActivity::class.java)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridSearchOnboardingScreen(
    modifier: Modifier = Modifier,
    isHybridSearchEnabled: Boolean = Prefs.isHybridSearchEnabled,
    onGetStarted: (String?) -> Unit,
    onLearnMoreClick: () -> Unit
) {
    val totalPageCount = 2
    val pagerState = rememberPagerState(pageCount = { totalPageCount })
    val coroutineScope = rememberCoroutineScope()
    var isHybridSearchEnabled by remember { mutableStateOf(isHybridSearchEnabled) }

    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .padding(top = 58.dp, bottom = 32.dp)
                            .padding(horizontal = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = WikipediaTheme.colors.progressiveColor,
                                    shape = RoundedCornerShape(size = 16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hybrid_search_beta_tag).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.hybrid_search_onboarding_screen_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor
                )
            )
        },
        bottomBar = {
            MainBottomBar(
                isHybridSearchEnabled = isHybridSearchEnabled,
                pagerState = pagerState,
                onNextButtonClick = {
                    if (pagerState.currentPage == 0 && isHybridSearchEnabled) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = 1,
                                animationSpec = tween(durationMillis = 400)
                            )
                        }
                    } else {
                        onGetStarted(null)
                    }
                },
                onLearnMoreClick = onLearnMoreClick
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top,
            userScrollEnabled = isHybridSearchEnabled
        ) { page ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (page) {
                    0 -> {
                        val isVisible = remember { MutableTransitionState(false) }
                        LaunchedEffect(Unit) {
                            isVisible.targetState = true
                        }

                        AnimatedVisibility(
                            visibleState = isVisible,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = 300),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 300)
                            )
                        ) {
                            Column {
                                onboardingItems.forEachIndexed { index, onboardingItem ->
                                    OnboardingListItem(item = onboardingItem)
                                }
                                ExperimentalFeatureToggleView(
                                    isChecked = isHybridSearchEnabled,
                                    onCheckedChange = {
                                        isHybridSearchEnabled = it
                                        Prefs.isHybridSearchEnabled = it
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        SearchExamplesView(
                            modifier = Modifier
                                .background(WikipediaTheme.colors.paperColor),
                            onClick = { exampleQuery ->
                                onGetStarted(exampleQuery)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExperimentalFeatureToggleView(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(36.dp)) // (Icon size + spacer width) which aligns with the OnboardingList item
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(R.string.hybrid_search_experimental_feature_switch_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WikipediaTheme.colors.primaryColor
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                checkedThumbColor = WikipediaTheme.colors.paperColor
            )
        )
    }
}

@Composable
fun SearchExamplesView(
    modifier: Modifier = Modifier,
    searchExamples: List<Int> = defaultSearchQueries,
    onClick: (String) -> Unit
) {
    val isVisible = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        isVisible.targetState = true
    }
    Column(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visibleState = isVisible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 300),
                initialOffsetY = { it / 2 }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            OnboardingListItem(
                item = OnboardingItem(
                    icon = R.drawable.ic_light_bulb,
                    title = R.string.hybrid_search_onboarding_search_example_title,
                    subTitle = R.string.hybrid_search_onboarding_search_example_description
                )
            )
        }

        FlowRow(
            modifier = Modifier
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searchExamples.forEachIndexed { index, exampleQuery ->
                val exampleQueryString = stringResource(exampleQuery)
                AnimatedVisibility(
                    visibleState = isVisible,
                    enter = slideInVertically(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = (index * 50) + 150,
                            easing = LinearOutSlowInEasing
                        ),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = (index * 50) + 150,
                            easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    SuggestionChip(
                        onClick = { onClick(exampleQueryString) },
                        label = {
                            Text(
                                text = exampleQueryString,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = WikipediaTheme.colors.paperColor,
                            labelColor = WikipediaTheme.colors.secondaryColor,
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = WikipediaTheme.colors.borderColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MainBottomBar(
    isHybridSearchEnabled: Boolean,
    pagerState: PagerState,
    onNextButtonClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.paperColor
            ),
            onClick = onLearnMoreClick
        ) {
            Text(
                text = stringResource(R.string.hybrid_search_onboarding_learn_more),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor
            )
        }

        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isHybridSearchEnabled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                PageIndicator(
                    modifier = Modifier,
                    pagerState = pagerState
                )
            }
        }

        Button(
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor
            ),
            onClick = onNextButtonClick
        ) {
            AnimatedContent(
                targetState = if (pagerState.currentPage == 0) stringResource(
                    R.string.onboarding_next
                ) else stringResource(R.string.onboarding_get_started),
            ) { targetText ->
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.paperColor
                )
            }
        }
    }
}

@Preview(showBackground = true, device = PIXEL_9, showSystemUi = true)
@Composable
private fun HybridSearchOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchOnboardingScreen(
            isHybridSearchEnabled = true,
            onGetStarted = {},
            onLearnMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchExamplesPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SearchExamplesView(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor),
            onClick = {}
        )
    }
}
