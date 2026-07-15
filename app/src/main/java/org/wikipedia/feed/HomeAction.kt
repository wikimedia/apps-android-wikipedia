package org.wikipedia.feed

import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.history.HistoryEntry

sealed interface HomeAction {
    data class SelectTab(val tab: HomeTab, val card: Card?) : HomeAction
    data class RefreshTab(val tab: HomeTab) : HomeAction
    data object LoadMoreCommunityContent : HomeAction
    data object LoadMoreForYouContent : HomeAction
    data class HideCommunityCard(val card: Card) : HomeAction
    data class HideForYouCard(val module: ForYouModule, val card: ForYouCard) : HomeAction
    data class HideModule(val moduleKey: String) : HomeAction
    data class PageClick(val card: Card, val historyEntry: HistoryEntry) : HomeAction
    data class PageBookmarkClick(val card: Card, val historyEntry: HistoryEntry) : HomeAction
    data class PageShareClick(val card: Card, val historyEntry: HistoryEntry) : HomeAction
    data class PageOverflowClick(val card: Card, val pageSummary: PageSummary, val source: Int, val menuKey: String) : HomeAction
    data object PageOverflowDismiss : HomeAction
    data class NewsClick(val card: NewsCard, val newsItem: NewsItem) : HomeAction
    data class ImageClick(val card: FeaturedImageCard) : HomeAction
    data class ImageDownloadClick(val card: FeaturedImageCard) : HomeAction
    data class ImageShareClick(val card: FeaturedImageCard) : HomeAction
    data class LanguageSelected(val languageCode: String) : HomeAction
    data object ManageLanguagesClick : HomeAction
    data object TabClick : HomeAction
    data object UpdateTabCount : HomeAction
    data class CustomizeClick(val card: Card?) : HomeAction
    data class CardImpression(val card: Card, val index: Int) : HomeAction
    data class CardFooterClick(val card: Card) : HomeAction
    data object NotificationClick : HomeAction
    data object ManageModulesClick : HomeAction
    data object ShuffleClick : HomeAction
    data object PlacesTeaserClick : HomeAction
    data object DiscoverTeaserClick : HomeAction
    data object SeeAllRecommendationsClick : HomeAction
    data class GameActionClick(val wikiGame: WikiGame) : HomeAction
    data object GoToGamesHubClick : HomeAction
}
