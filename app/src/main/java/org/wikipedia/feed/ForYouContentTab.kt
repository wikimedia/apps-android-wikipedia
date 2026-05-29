package org.wikipedia.feed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.becauseyouread.BecauseYouReadModule
import org.wikipedia.feed.continuereading.ContinueReadingModule
import org.wikipedia.feed.interests.BasedOnInterestModule
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.EmptyForYouCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.places.PlacesOfInterestCtaModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil

@Composable
fun ForYouContentTab(
    state: ForYouContentState,
    topInset: Int,
    wikiSite: WikiSite,
    onLoadMore: () -> Unit,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onCustomizeInterestsClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onManageModulesClick: () -> Unit,
    onSelectTab: (HomeTab, Card?) -> Unit = { _, _ -> },
    onPlacesCtaClick: () -> Unit = {}
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
            ForYouFeedEmptyView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(horizontal = 16.dp)
                    .padding(top = (topInset * 2 + 64).dp)
                    .verticalScroll(rememberScrollState()),
                wikiSite = wikiSite,
                showCustomizeInterests = !state.isInterestModuleHidden,
                onCustomizeInterestsClick = { onCustomizeInterestsClick(card) },
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
                            when (module) {
                                is ForYouModule.BasedOnInterest -> {
                                    item(key = "interest-${module.age}-$index") {
                                        BasedOnInterestModule(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(viewportHeight),
                                            wikiSite = wikiSite,
                                            module = module,
                                            onPageClick = onPageClick,
                                            onPageShareClick = onPageShareClick,
                                            onPageBookmarkClick = onPageBookmarkClick,
                                            onHideCardClick = onHideCardClick,
                                            onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                            onCardInView = { onCardImpression(it, index) },
                                            onCustomizeInterestsClick = onCustomizeInterestsClick
                                        )
                                    }
                                }

                                is ForYouModule.ContinueReading -> {
                                    item(key = "continue-reading-${module.age}-$index") {
                                        ContinueReadingModule(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(viewportHeight),
                                            wikiSite = wikiSite,
                                            module = module,
                                            onPageClick = onPageClick,
                                            onPageShareClick = onPageShareClick,
                                            onPageBookmarkClick = onPageBookmarkClick,
                                            onHideCardClick = onHideCardClick,
                                            onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                            onCardInView = { onCardImpression(it, index) },
                                            onCustomizeInterestsClick = onCustomizeInterestsClick
                                        )
                                    }
                                }

                                is ForYouModule.PlacesOfInterest -> {
                                    item(key = "places-of-interest-${module.age}-$index") {
                                        if (!module.hasLocationPermission) {
                                            PlacesOfInterestCtaModule(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(viewportHeight)
                                                    .background(ComposeColors.Green800)
                                                    .padding(horizontal = 16.dp)
                                                    .padding(top = (topInset * 2 + 64).dp)
                                                    .navigationBarsPadding(),
                                                wikiSite = wikiSite,
                                                onGoToPlacesClick = onPlacesCtaClick
                                            )
                                        } else {
                                            // TODO: render the nearby place article cards module.
                                        }
                                    }
                                }

                                is ForYouModule.BecauseYouRead -> {
                                    item(key = "because-you-read-${module.age}-$index") {
                                        BecauseYouReadModule(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(viewportHeight),
                                            wikiSite = wikiSite,
                                            module = module,
                                            onPageClick = onPageClick,
                                            onPageShareClick = onPageShareClick,
                                            onPageBookmarkClick = onPageBookmarkClick,
                                            onHideCardClick = onHideCardClick,
                                            onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                            onCardInView = { onCardImpression(it, index) },
                                            onCustomizeInterestsClick = onCustomizeInterestsClick
                                        )
                                    }
                                }
                            }
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
                            }
                        }

                        if (state.error != null && state.modules.isNotEmpty()) {
                            item(key = "error-foryou") {
                                ErrorState(state.error, onRetry = onLoadMore)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForYouFeedEmptyView(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    showCustomizeInterests: Boolean = true,
    onCustomizeInterestsClick: () -> Unit,
    navigateToCommunityTab: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.empty_feed_illustration),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            text = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.primaryColor
        )

        Text(
            text = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_ways_to_start),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp
            ),
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
        if (showCustomizeInterests) {
            EmptyStateActionRow(
                iconRes = R.drawable.ic_baseline_tune_24,
                text = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_add_interests),
                onLinkClick = onCustomizeInterestsClick
            )
        }

        EmptyStateActionRow(
            iconRes = R.drawable.ic_baseline_person_24,
            text = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_see_community),
            onLinkClick = navigateToCommunityTab
        )
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

@Preview
@Composable
fun ForYouFeedEmptyViewPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouFeedEmptyView(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            wikiSite = WikiSite.preview(),
            showCustomizeInterests = true,
            onCustomizeInterestsClick = {},
            navigateToCommunityTab = {}
        )
    }
}
