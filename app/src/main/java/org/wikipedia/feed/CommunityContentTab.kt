package org.wikipedia.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.menu.PageOverflowMenu
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.didyouknow.DidYouKnowCard
import org.wikipedia.feed.didyouknow.DidYouKnowModule
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.EmptyCommunityCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.news.NewsModule
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.onthisday.OnThisDayModule
import org.wikipedia.feed.topread.TopReadCard
import org.wikipedia.feed.topread.TopReadModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.L10nUtil
import java.time.LocalDate
import kotlin.collections.orEmpty

@Composable
fun CommunityContentTab(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    state: CommunityContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onLoadMore: () -> Unit,
    onHideCardClick: (card: Card) -> Unit = {},
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onNewsClick: (card: NewsCard, newsItem: NewsItem) -> Unit = { _, _ -> },
    onImageClick: (card: FeaturedImageCard) -> Unit = {},
    onImageDownloadClick: (card: FeaturedImageCard) -> Unit = {},
    onImageShareClick: (card: FeaturedImageCard) -> Unit = {},
    onCardFooterClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onManageModulesClick: () -> Unit
) {
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = modifier.fillMaxHeight())
        }
        state.emptyState == FeedEmptyState.ALL_MODULES_HIDDEN || state.emptyState == FeedEmptyState.NO_DATA -> {
            val context = LocalContext.current
            val card = EmptyCommunityCard()
            onCardImpression(card, 0)
            FeedEmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                title = context.getString(wikiSite.languageCode, R.string.home_feed_screen_empty_state_label),
                description = context.getString(wikiSite.languageCode, R.string.home_feed_community_screen_all_modules_disabled_description),
                buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_screen_all_modules_disabled_btn_label),
                onCallToActionClick = onManageModulesClick
            )
        }

        state.error != null && state.cards.isEmpty() -> {
            ErrorState(state.error, onRetry = onLoadMore)
        }
        else -> {
            val layoutDirection = if (L10nUtil.isLangRTL(wikiSite.languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    item {
                        CommunityDisclaimer(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            wikiSite = wikiSite
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    var lastCardWasDayHeader = false
                    state.cards.forEachIndexed { cardIndex, card ->
                        when (card) {
                            is DayHeaderCard -> {
                                item(key = "day-header-${card.age}") {
                                    DayHeader(LocalDate.now().minusDays(card.age.toLong()), isFirst = card.age == 0)
                                }
                            }
                            is FeaturedArticleCard -> {
                                item(key = "tfa-${card.age}") {
                                    FeaturedArticleModule(
                                        wikiSite = wikiSite,
                                        card.page,
                                        onPageClick = {
                                            onPageClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onShareClick = {
                                            onPageShareClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onBookmarkClick = {
                                            onPageBookmarkClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onCardImpression = { onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is TopReadCard -> {
                                if (lastCardWasDayHeader) {
                                    item(key = "top-read-spacer-${card.age}") {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                                item(key = "top-read-${card.age}") {
                                    TopReadModule(
                                        wikiSite = wikiSite,
                                        topRead = card.articles,
                                        pageOverflowContent = { index ->
                                            PageOverflowMenu(
                                                menuKey = "top-read-${card.age}-$index",
                                                overflowMenuState = overflowMenuState,
                                                onDismiss = onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = { entry ->
                                            onPageClick(card,
                                                entry.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_MOST_READ
                                                )
                                            )
                                        },
                                        onPageOverflowClick = { pageSummary, index ->
                                            onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_MOST_READ, "top-read-${card.age}-$index")
                                        },
                                        onFooterClick = { onCardFooterClick(card) },
                                        onCardImpression = { onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is NewsCard -> {
                                item(key = "news-${card.age}") {
                                    NewsModule(
                                        wikiSite = wikiSite,
                                        newsItems = card.news,
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onNewsClick = { newsItem ->
                                            onNewsClick(card, newsItem)
                                        },
                                        onCardImpression = { onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is OnThisDayCard -> {
                                if (lastCardWasDayHeader) {
                                    item(key = "on-this-day-spacer-${card.age}") {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                                item(key = "on-this-day-${card.age}") {
                                    OnThisDayModule(
                                        wikiSite = wikiSite,
                                        events = card.events,
                                        pageOverflowContent = { eventIndex, itemIndex ->
                                            PageOverflowMenu(
                                                menuKey = "on-this-day-${card.age}-$eventIndex-$itemIndex",
                                                overflowMenuState = overflowMenuState,
                                                onDismiss = onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = { pageSummary ->
                                            onPageClick(card, pageSummary.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_ON_THIS_DAY))
                                        },
                                        onPageOverflowClick = { pageSummary, eventIndex, itemIndex ->
                                            onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_ON_THIS_DAY, "on-this-day-${card.age}-$eventIndex-$itemIndex")
                                        },
                                        onFooterClick = { onCardFooterClick(card) },
                                        onCardImpression = { onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is FeaturedImageCard -> {
                                item(key = "tfi-${card.age}") {
                                    FeaturedImageModule(
                                        wikiSite = wikiSite,
                                        card = card,
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onClick = onImageClick,
                                        onDownloadClick = onImageDownloadClick,
                                        onShareClick = onImageShareClick,
                                        onCardImpression = { onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is DidYouKnowCard -> {
                                item(key = "dyk-${card.date}") {
                                    DidYouKnowModule(
                                        wikiSite = wikiSite,
                                        dyk = card.items,
                                        onHideCardClick = { onHideCardClick(card) },
                                        onHideModuleClick = {
                                            onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = {
                                            onPageClick(card, HistoryEntry(it, HistoryEntry.SOURCE_FEED_DID_YOU_KNOW))
                                        },
                                        onFooterClick = { onCardFooterClick(card) },
                                        onCardImpression = { onCardImpression(card, cardIndex) },
                                        pageOverflowContent = { index ->
                                            PageOverflowMenu(
                                                menuKey = "dyk-${card.date}-$index",
                                                overflowMenuState = overflowMenuState,
                                                onDismiss = onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onPageOverflowClick = { pageSummary, index ->
                                            onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_DID_YOU_KNOW, "dyk-${card.date}-$index")
                                        }
                                    )
                                }
                            }
                            else -> {
                                // TODO: Today's Featured Picture
                                // TODO: Media of the day (Commons)
                            }
                        }
                        lastCardWasDayHeader = card is DayHeaderCard
                    }

                    item(key = "load-more-community") {
                        if (state.isLoadingMore) {
                            LoadingIndicator()
                        } else if (state.canLoadMore && state.cards.isNotEmpty()) {
                            LoadMoreButton(
                                wikiSite = wikiSite,
                                isCommunity = true,
                                onClick = onLoadMore
                            )
                        }
                    }

                    if (state.error != null && state.cards.isNotEmpty()) {
                        item(key = "error-community") {
                            ErrorState(state.error, onRetry = onLoadMore)
                        }
                    }
                }
            }
        }
    }
}
