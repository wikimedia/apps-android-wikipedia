package org.wikipedia.feed

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.becauseyouread.BecauseYouReadModule
import org.wikipedia.feed.continuereading.ContinueReadingModule
import org.wikipedia.feed.discover.DiscoverArticlesModule
import org.wikipedia.feed.discover.DiscoverEnablePromptModule
import org.wikipedia.feed.interests.BasedOnInterestModule
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.DiscoverEnablePromptCard
import org.wikipedia.feed.model.EmptyForYouCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.PlacesOfInterestLocationPromptCard
import org.wikipedia.feed.places.PlacesOfInterestArticlesModule
import org.wikipedia.feed.places.PlacesOfInterestLocationPromptModule
import org.wikipedia.feed.random.RandomModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil

@Composable
fun ForYouContentTab(
    state: ForYouContentState,
    topInset: Int,
    wikiSite: WikiSite,
    onLoadMore: () -> Unit,
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onCustomizeClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onManageModulesClick: () -> Unit,
    onSelectTab: (HomeTab, Card?) -> Unit = { _, _ -> },
    onShuffleClick: () -> Unit = {},
    onPlacesTeaserClick: () -> Unit = {},
    onDiscoverTeaserClick: () -> Unit = {},
    onSeeAllRecommendationsClick: () -> Unit = {}
) {
    when {
        state.isInitialLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = WikipediaTheme.colors.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.fillMaxHeight())
            }
        }
        state.emptyState == FeedEmptyState.NO_DATA -> {
            val card = EmptyForYouCard()
            onCardImpression(card, 0)
            ForYouFeedMessageView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(horizontal = 16.dp)
                    .padding(top = (topInset * 2 + 64).dp)
                    .verticalScroll(rememberScrollState()),
                wikiSite = wikiSite,
                illustrationResId = R.drawable.empty_feed_illustration,
                titleResId = R.string.home_feed_for_you_screen_empty_title,
                descriptionResId = R.string.home_feed_for_you_screen_empty_description,
                headerResId = R.string.home_feed_for_you_screen_empty_ways_to_start,
                customizeInterestsTextResId = R.string.home_feed_for_you_screen_empty_add_interests,
                showCustomizeInterests = !state.isInterestModuleHidden,
                onCustomizeClick = { onCustomizeClick(card) },
                navigateToCommunityTab = { onSelectTab(HomeTab.COMMUNITY, card) }
            )
        }
        state.emptyState == FeedEmptyState.ALL_MODULES_HIDDEN -> {
            val context = LocalContext.current
            FeedEmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(horizontal = 16.dp)
                    .padding(top = (topInset * 2 + 64).dp)
                    .verticalScroll(rememberScrollState()),
                title = context.getString(wikiSite.languageCode, R.string.home_feed_screen_empty_state_label),
                description = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_all_modules_disabled_description),
                buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_screen_all_modules_disabled_btn_label),
                onCallToActionClick = onManageModulesClick
            )
        }
        state.error != null && state.modules.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = WikipediaTheme.colors.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(state.error, onRetry = onLoadMore)
            }
        }
        else -> {
            val layoutDirection = if (L10nUtil.isLangRTL(wikiSite.languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                val listState = rememberLazyListState()
                val modules = state.modules

                LaunchedEffect(listState, modules.size) {
                    snapshotFlow {
                        Pair(listState.firstVisibleItemIndex, listState.isScrollInProgress)
                    }.collect { (index, isScrolling) ->
                        val dummyIndex = modules.size + 1
                        if (!isScrolling && index >= dummyIndex) {
                            listState.scrollToItem(0)
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight

                    LazyColumn(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WikipediaTheme.colors.backgroundColor)
                    ) {
                        modules.forEachIndexed { index, module ->
                            forYouModuleItem(
                                module = module,
                                index = index,
                                topInset = topInset,
                                viewPortHeight = viewportHeight,
                                wikiSite = wikiSite,
                                onPageClick = onPageClick,
                                onPageShareClick = onPageShareClick,
                                onPageBookmarkClick = onPageBookmarkClick,
                                onHideCardClick = onHideCardClick,
                                onHideModuleClick = onHideModuleClick,
                                onCardImpression = onCardImpression,
                                onCustomizeClick = onCustomizeClick,
                                onShuffleClick = onShuffleClick,
                                onPlacesTeaserClick = onPlacesTeaserClick,
                                onDiscoverTeaserClick = onDiscoverTeaserClick,
                                onSeeAllRecommendationsClick = onSeeAllRecommendationsClick
                            )
                        }

                        item(key = "load-more-foryou") {
                            if (state.isLoadingMore) {
                                LoadingIndicator()
                            } else if (state.canLoadMore) {
                                // In case we want to load more For You items in the future:
                                // LoadMoreButton(
                                //     wikiSite = wikiSite,
                                //     isCommunity = false,
                                //     onClick = onLoadMore
                                // )
                            } else if (state.modules.isNotEmpty()) {
                                // only when we don't serve new content on the same day
                                val card = EmptyForYouCard()
                                ForYouFeedMessageView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(viewportHeight)
                                        .background(colorResource(R.color.green800))
                                        .padding(16.dp)
                                        .padding(top = (topInset * 2 + 64).dp)
                                        .navigationBarsPadding(),
                                    wikiSite = wikiSite,
                                    illustrationResId = R.drawable.yir_puzzle_browser,
                                    titleResId = R.string.home_feed_for_you_screen_end_of_feed_title,
                                    descriptionResId = R.string.home_feed_for_you_screen_end_of_feed_description,
                                    headerResId = R.string.home_feed_for_you_screen_end_of_feed_ways_to_keep_learning,
                                    customizeInterestsTextResId = R.string.home_feed_for_you_screen_end_of_feed_add_interests,
                                    onCustomizeClick = { onCustomizeClick(card) },
                                    navigateToCommunityTab = { onSelectTab(HomeTab.COMMUNITY, card) }
                                )
                            }
                        }

                        if (state.error != null && state.modules.isNotEmpty()) {
                            item(key = "error-foryou") {
                                ErrorState(state.error, onRetry = onLoadMore)
                            }
                        }

                        if (state.error == null && !state.isLoadingMore && !state.canLoadMore && state.modules.isNotEmpty()) {
                            forYouModuleItem(
                                module = state.modules.first(),
                                index = modules.size + 1,
                                topInset = topInset,
                                viewPortHeight = viewportHeight,
                                wikiSite = wikiSite,
                                onPageClick = onPageClick,
                                onPageShareClick = onPageShareClick,
                                onPageBookmarkClick = onPageBookmarkClick,
                                onHideCardClick = onHideCardClick,
                                onHideModuleClick = onHideModuleClick,
                                onCardImpression = { _, _ -> },
                                onCustomizeClick = onCustomizeClick,
                                onShuffleClick = onShuffleClick,
                                onPlacesTeaserClick = onPlacesTeaserClick,
                                onDiscoverTeaserClick = onDiscoverTeaserClick,
                                onSeeAllRecommendationsClick = onSeeAllRecommendationsClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.forYouModuleItem(
    module: ForYouModule,
    index: Int,
    topInset: Int,
    viewPortHeight: Dp,
    wikiSite: WikiSite,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit,
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit,
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit,
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit,
    onHideModuleClick: (moduleKey: String) -> Unit,
    onCardImpression: (card: Card, index: Int) -> Unit,
    onCustomizeClick: (card: Card) -> Unit,
    onShuffleClick: () -> Unit,
    onPlacesTeaserClick: () -> Unit,
    onDiscoverTeaserClick: () -> Unit,
    onSeeAllRecommendationsClick: () -> Unit
) {
    val key = "${module.javaClass.simpleName}-${module.age}-$index"
    when (module) {
        is ForYouModule.BasedOnInterest -> {
            item(key = key) {
                BasedOnInterestModule(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewPortHeight),
                    wikiSite = wikiSite,
                    module = module,
                    onPageClick = onPageClick,
                    onPageShareClick = onPageShareClick,
                    onPageBookmarkClick = onPageBookmarkClick,
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                    onCardInView = { onCardImpression(it, index) },
                    onCustomizeClick = onCustomizeClick
                )
            }
        }
        is ForYouModule.ContinueReading -> {
            item(key = key) {
                ContinueReadingModule(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewPortHeight),
                    wikiSite = wikiSite,
                    module = module,
                    onPageClick = onPageClick,
                    onPageShareClick = onPageShareClick,
                    onPageBookmarkClick = onPageBookmarkClick,
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                    onCardInView = { onCardImpression(it, index) },
                    onCustomizeClick = onCustomizeClick
                )
            }
        }
        is ForYouModule.BecauseYouRead -> {
            item(key = key) {
                BecauseYouReadModule(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewPortHeight),
                    wikiSite = wikiSite,
                    module = module,
                    onPageClick = onPageClick,
                    onPageShareClick = onPageShareClick,
                    onPageBookmarkClick = onPageBookmarkClick,
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                    onCardInView = { onCardImpression(it, index) },
                    onCustomizeClick = onCustomizeClick
                )
            }
        }
        is ForYouModule.PlacesOfInterest -> {
            item(key = key) {
                when {
                    module.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                    !module.hasLocationPermission -> {
                        onCardImpression(PlacesOfInterestLocationPromptCard(), index)
                        PlacesOfInterestLocationPromptModule(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight)
                                .background(ComposeColors.Green800)
                                .padding(horizontal = 16.dp)
                                .padding(top = (topInset * 2 + 64).dp)
                                .navigationBarsPadding(),
                            wikiSite = wikiSite,
                            onGoToPlacesClick = onPlacesTeaserClick
                        )
                    }
                    else -> {
                        PlacesOfInterestArticlesModule(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight),
                            wikiSite = wikiSite,
                            module = module,
                            onPageClick = onPageClick,
                            onPageShareClick = onPageShareClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onHideCardClick = onHideCardClick,
                            onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                            onCardInView = { onCardImpression(it, index) },
                            onCustomizeClick = onCustomizeClick
                        )
                    }
                }
            }
        }
        is ForYouModule.Discover -> {
            item(key = key) {
                when {
                    module.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                    !module.isEnabled -> {
                        onCardImpression(DiscoverEnablePromptCard(), index)
                        DiscoverEnablePromptModule(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight)
                                .background(ComposeColors.Green800)
                                .padding(horizontal = 16.dp)
                                .padding(top = (topInset * 2 + 64).dp)
                                .navigationBarsPadding(),
                            wikiSite = wikiSite,
                            onEnableDiscoverClick = onDiscoverTeaserClick
                        )
                    }
                    else -> {
                        DiscoverArticlesModule(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewPortHeight),
                            topInset = topInset,
                            wikiSite = wikiSite,
                            module = module,
                            updateFrequency = module.updateFrequency.displayStringRes,
                            onPageClick = onPageClick,
                            onPageShareClick = onPageShareClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onHideCardClick = onHideCardClick,
                            onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                            onCardInView = { onCardImpression(it, index) },
                            onCustomizeClick = onCustomizeClick,
                            onSeeAllRecommendationsClick = onSeeAllRecommendationsClick
                        )
                    }
                }
            }
        }
        is ForYouModule.Random -> {
            item(key = key) {
                RandomModule(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewPortHeight),
                    wikiSite = wikiSite,
                    module = module,
                    onPageClick = onPageClick,
                    onPageShareClick = onPageShareClick,
                    onPageBookmarkClick = onPageBookmarkClick,
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                    onCardInView = { onCardImpression(it, index) },
                    onCustomizeClick = onCustomizeClick,
                    onShuffleClick = onShuffleClick
                )
            }
        }
    }
}

@Composable
fun EmptyStateActionRow(
    @DrawableRes iconRes: Int,
    text: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            tint = WikipediaTheme.colors.primaryColor,
            contentDescription = null
        )
        HtmlText(
            text = text,
            linkStyle = TextLinkStyles(
                style = SpanStyle(
                    fontSize = 14.sp,
                    color = WikipediaTheme.colors.primaryColor,
                    textDecoration = TextDecoration.Underline
                )
            ),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            linkInteractionListener = LinkInteractionListener { onLinkClick() }
        )
    }
}

@Composable
fun ForYouFeedMessageView(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    @DrawableRes illustrationResId: Int,
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int,
    @StringRes headerResId: Int,
    @StringRes customizeInterestsTextResId: Int,
    @StringRes communityTabTextResId: Int = R.string.home_feed_for_you_screen_empty_see_community,
    showCustomizeInterests: Boolean = true,
    onCustomizeClick: () -> Unit,
    navigateToCommunityTab: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.size(125.dp),
            model = ImageRequest.Builder(context)
                .data(illustrationResId)
                .allowHardware(false)
                .build(),
            success = { SubcomposeAsyncImageContent() },
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, titleResId),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif
            ),
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, descriptionResId),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, headerResId),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = WikipediaTheme.colors.primaryColor
        )
        if (showCustomizeInterests) {
            EmptyStateActionRow(
                iconRes = R.drawable.ic_baseline_tune_24,
                text = context.getString(wikiSite.languageCode, customizeInterestsTextResId),
                onLinkClick = onCustomizeClick
            )
        }
        EmptyStateActionRow(
            iconRes = R.drawable.ic_diversity_3_24dp,
            text = context.getString(wikiSite.languageCode, communityTabTextResId),
            onLinkClick = navigateToCommunityTab
        )
    }
}

@Preview
@Composable
fun ForYouFeedMessageViewPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        ForYouFeedMessageView(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.green800))
                .padding(16.dp),
            wikiSite = WikiSite.preview(),
            onCustomizeClick = {},
            illustrationResId = R.drawable.ic_yir_puzzle,
            titleResId = R.string.home_feed_for_you_screen_end_of_feed_title,
            descriptionResId = R.string.home_feed_for_you_screen_end_of_feed_description,
            headerResId = R.string.home_feed_for_you_screen_end_of_feed_ways_to_keep_learning,
            customizeInterestsTextResId = R.string.home_feed_for_you_screen_end_of_feed_add_interests,
            communityTabTextResId = R.string.home_feed_for_you_screen_empty_see_community,
            navigateToCommunityTab = {}
        )
    }
}

@Preview
@Composable
fun ForYouFeedEmptyViewPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouFeedMessageView(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            wikiSite = WikiSite.preview(),
            illustrationResId = R.drawable.empty_feed_illustration,
            titleResId = R.string.home_feed_for_you_screen_empty_title,
            descriptionResId = R.string.home_feed_for_you_screen_empty_description,
            headerResId = R.string.home_feed_for_you_screen_empty_ways_to_start,
            customizeInterestsTextResId = R.string.home_feed_for_you_screen_empty_add_interests,
            communityTabTextResId = R.string.home_feed_for_you_screen_empty_see_community,
            showCustomizeInterests = true,
            onCustomizeClick = {},
            navigateToCommunityTab = {}
        )
    }
}
