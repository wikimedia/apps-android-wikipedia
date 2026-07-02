package org.wikipedia.feed

import androidx.compose.runtime.Stable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.history.HistoryEntry

@Stable
class HomeActions(
    val onSelectTab: (tab: HomeTab, card: Card?) -> Unit = { _, _ -> },
    val onRefreshTab: (tab: HomeTab) -> Unit = {},
    val onLoadMoreCommunityContent: () -> Unit = {},
    val onLoadMoreForYouContent: () -> Unit = {},
    val onHideCommunityCardClick: (card: Card) -> Unit = {},
    val onHideForYouCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    val onHideModuleClick: (moduleKey: String) -> Unit = {},
    val onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    val onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    val onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    val onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    val onPageOverflowDismiss: () -> Unit = {},
    val onNewsClick: (card: NewsCard, newsItem: NewsItem) -> Unit = { _, _ -> },
    val onImageClick: (card: FeaturedImageCard) -> Unit = {},
    val onImageDownloadClick: (card: FeaturedImageCard) -> Unit = {},
    val onImageShareClick: (card: FeaturedImageCard) -> Unit = {},
    val onLanguageSelected: (languageCode: String) -> Unit = {},
    val onManageLanguagesClick: () -> Unit = {},
    val onTabClick: () -> Unit = {},
    val onUpdateTabCount: () -> Unit = {},
    val onCustomizeClick: (card: Card?) -> Unit = {},
    val onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    val onCardFooterClick: (card: Card) -> Unit = {},
    val onNotificationClick: () -> Unit = {},
    val onManageModulesClick: () -> Unit = {},
    val onShuffleClick: () -> Unit = {},
    val onPlacesTeaserClick: () -> Unit = {},
    val onDiscoverTeaserClick: () -> Unit = {},
    val onSeeAllRecommendationsClick: () -> Unit = {},
    val onGameActionClick: (wikiGame: WikiGame) -> Unit = {},
    val onGoToGamesHubClick: () -> Unit = {}
)
