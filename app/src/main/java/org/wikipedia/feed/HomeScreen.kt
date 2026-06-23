package org.wikipedia.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.NotificationBell
import org.wikipedia.compose.components.NotificationBellState
import org.wikipedia.compose.components.TabsBox
import org.wikipedia.compose.components.WikiLangCodeBox
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.extensions.pulse
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageState
import org.wikipedia.main.MainActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import kotlin.collections.orEmpty

@Composable
fun HomeScreen(
    wikiSite: WikiSite,
    languageState: AppLanguageState? = null,
    selectedTab: HomeTab,
    communityContentState: CommunityContentState,
    forYouContentState: ForYouContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    tabsState: TabsState,
    notificationBellState: NotificationBellState,
    onSelectTab: (HomeTab, Card?) -> Unit = { _, _ -> },
    onRefreshTab: (HomeTab) -> Unit = {},
    onLoadMoreCommunityContent: () -> Unit = {},
    onLoadMoreForYouContent: () -> Unit = {},
    onHideCommunityCardClick: (card: Card) -> Unit = {},
    onHideForYouCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
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
    onLanguageSelected: (String) -> Unit = {},
    onManageLanguagesClick: () -> Unit = {},
    onTabClick: () -> Unit = {},
    onUpdateTabCount: () -> Unit = {},
    onCustomizeClick: (card: Card?) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onCardFooterClick: (card: Card) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onManageModulesClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onPlacesTeaserClick: () -> Unit = {},
    onDiscoverTeaserClick: () -> Unit = {},
    onSeeAllRecommendationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val topInset = if (context is MainActivity) {
        DimenUtil.roundedPxToDp((context.getStatusBarInsets()?.top ?: 0).toFloat())
    } else 64
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = pullToRefreshState.isAnimating && (communityContentState.isInitialLoading || forYouContentState.isInitialLoading)

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = { onRefreshTab(selectedTab) },
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = WikipediaTheme.colors.paperColor,
                color = WikipediaTheme.colors.progressiveColor,
                state = pullToRefreshState
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                HomeTab.COMMUNITY -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxSize()
                            .background(WikipediaTheme.colors.paperColor)
                    ) {
                        HomeToolbar(
                            topInset = topInset,
                            tabsState = tabsState,
                            onTabClick = onTabClick,
                            onUpdateTabCount = onUpdateTabCount,
                            notificationBellState = notificationBellState,
                            onNotificationClick = onNotificationClick
                        )

                        HomeTabBar(
                            modifier = Modifier.padding(top = 8.dp),
                            wikiSite = wikiSite,
                            selectedTab = selectedTab,
                            languageState = languageState,
                            onSelectTab = onSelectTab,
                            onLanguageSelected = {
                                onLanguageSelected(it)
                            },
                            onManageLanguagesClick = {
                                onManageLanguagesClick()
                            }
                        )

                        CommunityContentTab(
                            modifier = Modifier.weight(1f),
                            wikiSite = wikiSite,
                            state = communityContentState,
                            overflowMenuState = overflowMenuState,
                            onLoadMore = onLoadMoreCommunityContent,
                            onHideCardClick = onHideCommunityCardClick,
                            onHideModuleClick = onHideModuleClick,
                            onPageClick = onPageClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onPageShareClick = onPageShareClick,
                            onPageOverflowClick = onPageOverflowClick,
                            onPageOverflowDismiss = onPageOverflowDismiss,
                            onNewsClick = onNewsClick,
                            onImageClick = onImageClick,
                            onImageDownloadClick = onImageDownloadClick,
                            onImageShareClick = onImageShareClick,
                            onCardImpression = onCardImpression,
                            onCardFooterClick = onCardFooterClick,
                            onManageModulesClick = onManageModulesClick
                        )
                    }
                }

                HomeTab.FOR_YOU -> {
                    ForYouContentTab(
                        topInset = topInset,
                        state = forYouContentState,
                        wikiSite = wikiSite,
                        onLoadMore = onLoadMoreForYouContent,
                        onPageClick = onPageClick,
                        onHideCardClick = onHideForYouCardClick,
                        onHideModuleClick = onHideModuleClick,
                        onPageBookmarkClick = onPageBookmarkClick,
                        onPageShareClick = onPageShareClick,
                        onCustomizeClick = onCustomizeClick,
                        onCardImpression = onCardImpression,
                        onManageModulesClick = onManageModulesClick,
                        onSelectTab = onSelectTab,
                        onShuffleClick = onShuffleClick,
                        onPlacesTeaserClick = onPlacesTeaserClick,
                        onDiscoverTeaserClick = onDiscoverTeaserClick,
                        onSeeAllRecommendationsClick = onSeeAllRecommendationsClick
                    )

                    // Floating toolbar with gradient scrim, wordmark, and tab selector.
                    Column(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.80f))
                        ) {
                            HomeToolbar(
                                topInset = topInset,
                                tabsState = tabsState,
                                onTabClick = onTabClick,
                                onUpdateTabCount = onUpdateTabCount,
                                notificationBellState = notificationBellState,
                                onNotificationClick = onNotificationClick
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.80f),
                                            0.18f to Color.Black.copy(alpha = 0.64f),
                                            0.38f to Color.Black.copy(alpha = 0.40f),
                                            0.58f to Color.Black.copy(alpha = 0.20f),
                                            0.76f to Color.Black.copy(alpha = 0.08f),
                                            0.90f to Color.Black.copy(alpha = 0.02f),
                                            1.0f to Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            HomeTabBar(
                                modifier = Modifier.padding(top = 8.dp, bottom = 64.dp),
                                wikiSite = wikiSite,
                                selectedTab = selectedTab,
                                languageState = languageState,
                                onSelectTab = onSelectTab,
                                onLanguageSelected = {
                                    onLanguageSelected(it)
                                },
                                onManageLanguagesClick = {
                                    onManageLanguagesClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeToolbar(
    topInset: Int,
    tabsState: TabsState,
    notificationBellState: NotificationBellState,
    onTabClick: () -> Unit,
    onUpdateTabCount: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.feed_header_wordmark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WikipediaTheme.colors.primaryColor),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, top = (topInset + 16).dp)
                .width(128.dp)
        )
        Spacer(modifier = Modifier.weight(1f))

        val actionButtonModifier = Modifier
            .statusBarsPadding()
            .padding(top = topInset.dp)
            .size(48.dp)

        if (tabsState.count > 0) {
            IconButton(
                modifier = actionButtonModifier,
                onClick = { onTabClick() }
            ) {
                TabsBox(
                    modifier = Modifier
                        .width(21.dp)
                        .height(20.dp)
                        .then(
                            if (tabsState.pulse) {
                                Modifier.pulse(
                                    durationMillis = 300,
                                    toScale = 1.25f,
                                    onCompleted = {
                                        onUpdateTabCount()
                                    }
                                )
                            } else {
                                Modifier
                            }),
                    backgroundColor = Color.Transparent,
                    count = tabsState.count
                )
            }
        }
        if (notificationBellState.canShow) {
            NotificationBell(
                modifier = actionButtonModifier,
                unreadCount = notificationBellState.unreadCount,
                onClick = onNotificationClick
            )
        }
        if (tabsState.count == 0 && !notificationBellState.canShow) {
            Spacer(modifier = actionButtonModifier)
        }
    }
}

@Composable
fun HomeTabBar(
    modifier: Modifier,
    wikiSite: WikiSite,
    selectedTab: HomeTab,
    languageState: AppLanguageState? = null,
    onSelectTab: (HomeTab, Card?) -> Unit = { _, _ -> },
    onLanguageSelected: (String) -> Unit,
    onManageLanguagesClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = when (tab) {
                    HomeTab.COMMUNITY -> LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_tab_label)
                    HomeTab.FOR_YOU -> LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_for_you_tab_label)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelectTab(tab, null) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selectedTab == HomeTab.FOR_YOU) WikipediaTheme.colors.primaryColor else if (isSelected) WikipediaTheme.colors.progressiveColor else WikipediaTheme.colors.primaryColor,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (isSelected) {
                                    if (selectedTab == HomeTab.FOR_YOU) WikipediaTheme.colors.primaryColor
                                    else WikipediaTheme.colors.progressiveColor
                                } else Color.Transparent
                            )
                    )
                }
            }
        }
        LanguageDropDownMenu(
            selectedLanguageCode = wikiSite.languageCode,
            onLanguageSelected = { onLanguageSelected(it) },
            onManageLanguagesClick = { onManageLanguagesClick() },
            languageState = languageState
        )
    }
}

@Composable
fun LanguageDropDownMenu(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onManageLanguagesClick: () -> Unit,
    languageState: AppLanguageState? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = WikipediaTheme.colors.primaryColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WikiLangCodeBox(
                modifier = Modifier
                    .height(20.dp)
                    .widthIn(min = 20.dp),
                languageCode = selectedLanguageCode,
                backgroundColor = WikipediaTheme.colors.primaryColor.copy(alpha = 0.8f),
                borderColor = Color.Transparent,
                textColor = WikipediaTheme.colors.paperColor,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_arrow_down_24),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor
            )
        }
        DropdownMenu(
            expanded = expanded,
            containerColor = WikipediaTheme.colors.paperColor,
            onDismissRequest = { expanded = false }
        ) {
            val languageCodes = languageState?.appLanguageCodes.orEmpty()
            repeat(languageCodes.size) {
                val langCode = languageCodes[it]
                DropdownMenuItem(
                    leadingIcon = {
                        WikiLangCodeBox(
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(min = 20.dp),
                            languageCode = langCode,
                            borderColor = WikipediaTheme.colors.secondaryColor,
                            textColor = WikipediaTheme.colors.secondaryColor,
                        )
                    },
                    trailingIcon = {
                        if (langCode == selectedLanguageCode) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_black_24dp),
                                contentDescription = null,
                                tint = WikipediaTheme.colors.secondaryColor
                            )
                        }
                    },
                    text = {
                        Text(
                            text = languageState?.getAppLanguageLocalizedName(langCode) ?: langCode,
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    },
                    onClick = {
                        onLanguageSelected(langCode)
                        expanded = false
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = WikipediaTheme.colors.borderColor
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onManageLanguagesClick()
                        expanded = false
                    }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = LocalContext.current.getString(selectedLanguageCode, R.string.explore_feed_manage_languages_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenCommunityAllModulesOffPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(emptyState = FeedEmptyState.ALL_MODULES_HIDDEN, wikiSite = WikiSite.preview()),
            forYouContentState = ForYouContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenForYouAllModulesOffPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            forYouContentState = ForYouContentState(emptyState = FeedEmptyState.ALL_MODULES_HIDDEN, wikiSite = WikiSite.preview()),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenCommunityPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            forYouContentState = ForYouContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenForYouPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            forYouContentState = ForYouContentState(isInitialLoading = true, wikiSite = WikiSite.preview()),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 99, canShow = true)
        )
    }
}

@Preview
@Composable
fun LanguageDropDownMenuPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LanguageDropDownMenu(
            selectedLanguageCode = "en",
            onLanguageSelected = {},
            onManageLanguagesClick = {}
        )
    }
}
