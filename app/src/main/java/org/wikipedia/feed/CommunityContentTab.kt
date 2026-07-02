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
import org.wikipedia.extensions.getString
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.didyouknow.DidYouKnowCard
import org.wikipedia.feed.didyouknow.DidYouKnowModule
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.model.EmptyCommunityCard
import org.wikipedia.feed.news.NewsCard
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
    actions: HomeActions = HomeActions()
) {
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = modifier.fillMaxHeight())
        }
        state.emptyState == FeedEmptyState.ALL_MODULES_HIDDEN || state.emptyState == FeedEmptyState.NO_DATA -> {
            val context = LocalContext.current
            val card = EmptyCommunityCard()
            actions.onCardImpression(card, 0)
            FeedEmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                title = context.getString(wikiSite.languageCode, R.string.home_feed_screen_empty_state_label),
                description = context.getString(wikiSite.languageCode, R.string.home_feed_community_screen_all_modules_disabled_description),
                buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_screen_all_modules_disabled_btn_label),
                onCallToActionClick = actions.onManageModulesClick
            )
        }

        state.error != null && state.cards.isEmpty() -> {
            ErrorState(state.error, onRetry = actions.onLoadMoreCommunityContent)
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
                                            actions.onPageClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onShareClick = {
                                            actions.onPageShareClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onBookmarkClick = {
                                            actions.onPageBookmarkClick(card,
                                                it.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_FEATURED
                                                )
                                            )
                                        },
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) }
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
                                                onDismiss = actions.onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = { entry ->
                                            actions.onPageClick(card,
                                                entry.getHistoryEntry(
                                                    wikiSite,
                                                    HistoryEntry.SOURCE_FEED_MOST_READ
                                                )
                                            )
                                        },
                                        onPageOverflowClick = { pageSummary, index ->
                                            actions.onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_MOST_READ, "top-read-${card.age}-$index")
                                        },
                                        onFooterClick = { actions.onCardFooterClick(card) },
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is NewsCard -> {
                                item(key = "news-${card.age}") {
                                    NewsModule(
                                        wikiSite = wikiSite,
                                        newsItems = card.news,
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onNewsClick = { newsItem ->
                                            actions.onNewsClick(card, newsItem)
                                        },
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) }
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
                                                onDismiss = actions.onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = { pageSummary ->
                                            actions.onPageClick(card, pageSummary.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_ON_THIS_DAY))
                                        },
                                        onPageOverflowClick = { pageSummary, eventIndex, itemIndex ->
                                            actions.onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_ON_THIS_DAY, "on-this-day-${card.age}-$eventIndex-$itemIndex")
                                        },
                                        onFooterClick = { actions.onCardFooterClick(card) },
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is FeaturedImageCard -> {
                                item(key = "tfi-${card.age}") {
                                    FeaturedImageModule(
                                        wikiSite = wikiSite,
                                        card = card,
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onClick = actions.onImageClick,
                                        onDownloadClick = actions.onImageDownloadClick,
                                        onShareClick = actions.onImageShareClick,
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) }
                                    )
                                }
                            }
                            is DidYouKnowCard -> {
                                item(key = "dyk-${card.date}") {
                                    DidYouKnowModule(
                                        wikiSite = wikiSite,
                                        dyk = card.items,
                                        onHideCardClick = { actions.onHideCommunityCardClick(card) },
                                        onHideModuleClick = {
                                            actions.onHideModuleClick(card.moduleKey())
                                        },
                                        onPageClick = {
                                            actions.onPageClick(card, HistoryEntry(it, HistoryEntry.SOURCE_FEED_DID_YOU_KNOW))
                                        },
                                        onFooterClick = { actions.onCardFooterClick(card) },
                                        onCardImpression = { actions.onCardImpression(card, cardIndex) },
                                        pageOverflowContent = { index ->
                                            PageOverflowMenu(
                                                menuKey = "dyk-${card.date}-$index",
                                                overflowMenuState = overflowMenuState,
                                                onDismiss = actions.onPageOverflowDismiss,
                                                items = overflowMenuState?.items.orEmpty()
                                            )
                                        },
                                        onPageOverflowClick = { pageSummary, index ->
                                            actions.onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_DID_YOU_KNOW, "dyk-${card.date}-$index")
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
                                onClick = actions.onLoadMoreCommunityContent
                            )
                        }
                    }

                    if (state.error != null && state.cards.isNotEmpty()) {
                        item(key = "error-community") {
                            ErrorState(state.error, onRetry = actions.onLoadMoreCommunityContent)
                        }
                    }
                }
            }
        }
    }
}
