package org.wikipedia.feed

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil3.compose.AsyncImage
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.TabsBox
import org.wikipedia.compose.components.WikiLangCodeBox
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.components.menu.PageOverflowMenu
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.news.NewsModule
import org.wikipedia.feed.onboarding.ExploreFeedUpdatePromptActivity
import org.wikipedia.feed.onthisday.OnThisDayActivity
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.onthisday.OnThisDayModule
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.feed.topread.TopReadModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageState
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private val pageOverflowMenuViewModel: PageOverflowMenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            maybeShowExploreFeedUpdatePrompt()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val wikiSite by viewModel.wikiSite.collectAsState()
                val tabsCount by viewModel.tabsState.collectAsState()

                BaseTheme(currentTheme = if (selectedTab == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme) {
                    HomeScreen(
                        wikiSite = wikiSite,
                        languageState = WikipediaApp.instance.languageState,
                        selectedTab = selectedTab,
                        communityContentState = viewModel.communityState.collectAsState().value,
                        forYouContentState = viewModel.forYouState.collectAsState().value,
                        overflowMenuState = pageOverflowMenuViewModel.pageOverflowMenuState,
                        tabsCountState = tabsCount,
                        onSelectTab = {
                            viewModel.selectTab(it)
                            (requireActivity() as? MainActivity)?.onTabChanged(NavTab.HOME)
                        },
                        onRefreshTab = {
                            if (it == HomeTab.COMMUNITY) {
                                viewModel.refreshCommunityContent()
                            } else {
                                viewModel.refreshForYouContent()
                            }
                        },
                        onLoadMoreCommunityContent = viewModel::loadCommunityContent,
                        onLoadMoreForYouContent = viewModel::loadForYouContent,
                        onHideCardClick = { card ->
                            val cardIndex = viewModel.hideCard(card)
                            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
                                .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                                    viewModel.restoreCard(card, cardIndex)
                                }.show()
                        },
                        onPageClick = {
                            (parentFragment as? MainFragment)?.onFeedSelectPage(it, false)
                        },
                        onPageBookmarkClick = {
                            (parentFragment as? MainFragment)?.onFeedAddPageToList(it, false)
                        },
                        onPageShareClick = {
                            ShareUtil.shareText(requireContext(), it.title)
                        },
                        onPageOverflowClick = { pageSummary, source, menuKey ->
                            pageOverflowMenuViewModel.onPageOverflowClick(
                                context = requireContext(),
                                wikiSite = wikiSite,
                                pageSummary = pageSummary,
                                source = source,
                                menuKey = menuKey,
                                onOpenPage = { entry ->
                                    (parentFragment as? MainFragment)?.onFeedSelectPage(entry, false)
                                },
                                onOpenInNewTab = { entry ->
                                    (parentFragment as? MainFragment)?.onFeedSelectPage(entry, true)
                                    viewModel.updateTabCount()
                                },
                                onAddRequest = { entry, addToDefault ->
                                    (parentFragment as? MainFragment)?.onFeedAddPageToList(entry, addToDefault)
                                },
                                onMoveRequest = { id, entry ->
                                    (parentFragment as? MainFragment)?.onFeedMovePageToList(id, entry)
                                },
                                onRemoveRequest = { entry, lists ->
                                    (parentFragment as? MainFragment)?.onFeedRemovePageFromList(entry, lists)
                                },
                                onShareRequest = { entry ->
                                    (parentFragment as? MainFragment)?.onFeedSharePage(entry)
                                },
                                onLinkCopyRequest = { entry ->
                                    (parentFragment as? MainFragment)?.onFeedCopyLink(entry)
                                }
                            )
                        },
                        onPageOverflowDismiss = {
                            pageOverflowMenuViewModel.dismissPageOverflowMenu()
                        },
                        onNewsClick = { newsItem ->
                            (parentFragment as? MainFragment)?.onFeedNewsItemSelected(newsItem, wikiSite)
                        },
                        onImageClick = {
                            (parentFragment as? MainFragment)?.onFeaturedImageSelected(it)
                        },
                        onImageShareClick = { image, age ->
                            (parentFragment as? MainFragment)?.onFeedShareImage(image, age)
                        },
                        onImageDownloadClick = {
                            (parentFragment as? MainFragment)?.onFeedDownloadImage(it)
                        },
                        onLanguageSelected = { languageCode ->
                            viewModel.updateLanguage(languageCode)
                        },
                        onManageLanguagesClick = {
                            requireActivity().startActivity(WikipediaLanguagesActivity.newIntent(requireContext(), invokeSource = Constants.InvokeSource.FEED))
                        }
                    )
                }
            }
        }
    }

    fun getCurrentTab(): HomeTab {
        return viewModel.selectedTab.value
    }

    private fun maybeShowExploreFeedUpdatePrompt() {
        if (!Prefs.isInitialOnboardingEnabled && Prefs.isExploreFeedUpdatePromptShown.not()) {
            startActivity(ExploreFeedUpdatePromptActivity.newIntent(requireContext()))
        }
    }
}

@Composable
fun HomeScreen(
    wikiSite: WikiSite,
    languageState: AppLanguageState? = null,
    selectedTab: HomeTab,
    communityContentState: CommunityContentState,
    forYouContentState: ForYouContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    tabsCountState: Int = 0,
    onSelectTab: (HomeTab) -> Unit = {},
    onRefreshTab: (HomeTab) -> Unit = {},
    onLoadMoreCommunityContent: () -> Unit = {},
    onLoadMoreForYouContent: () -> Unit = {},
    onHideCardClick: (card: Card) -> Unit = {},
    onPageClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageBookmarkClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageShareClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageOverflowClick: (pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onNewsClick: (newsItem: NewsItem) -> Unit = {},
    onImageClick: (image: FeaturedImage) -> Unit = {},
    onImageDownloadClick: (image: FeaturedImage) -> Unit = {},
    onImageShareClick: (image: FeaturedImage, age: Int) -> Unit = { _, _ -> },
    onLanguageSelected: (String) -> Unit = {},
    onManageLanguagesClick: () -> Unit = {},
    onTabClick: () -> Unit = {}
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
                        Row {
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
                            if (tabsCountState > 0) {
                                IconButton(
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .padding(top = topInset.dp),
                                    onClick = { onTabClick() }
                                ) {
                                    TabsBox(
                                        modifier = Modifier.size(20.dp),
                                        count = tabsCountState
                                    )
                                }
                            }
                        }

                        // Tab selector
                        HomeTabBar(
                            modifier = Modifier.padding(top = 8.dp),
                            wikiSite = wikiSite,
                            selectedTab = selectedTab,
                            languageState = languageState,
                            onTabSelected = onSelectTab,
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
                            onHideCardClick = onHideCardClick,
                            onPageClick = onPageClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onPageShareClick = onPageShareClick,
                            onPageOverflowClick = onPageOverflowClick,
                            onPageOverflowDismiss = onPageOverflowDismiss,
                            onNewsClick = onNewsClick,
                            onImageClick = onImageClick,
                            onImageDownloadClick = onImageDownloadClick,
                            onImageShareClick = onImageShareClick
                        )
                    }
                }

                HomeTab.FOR_YOU -> {
                    ForYouContentTab(
                        state = forYouContentState,
                        wikiSite = wikiSite,
                        onLoadMore = onLoadMoreForYouContent
                    )

                    // Floating toolbar with gradient scrim, wordmark, and tab selector.
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black.copy(alpha = 0.78f),
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

                        // Tab selector
                        HomeTabBar(
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                            wikiSite = wikiSite,
                            selectedTab = selectedTab,
                            languageState = languageState,
                            onTabSelected = onSelectTab,
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

@Composable
fun HomeTabBar(
    modifier: Modifier,
    wikiSite: WikiSite,
    selectedTab: HomeTab,
    languageState: AppLanguageState? = null,
    onTabSelected: (HomeTab) -> Unit,
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
                        .clickable { onTabSelected(tab) }
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
fun CommunityContentTab(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    state: CommunityContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onLoadMore: () -> Unit,
    onHideCardClick: (card: Card) -> Unit = {},
    onPageClick: (historyEntry: HistoryEntry) -> Unit,
    onPageBookmarkClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageShareClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageOverflowClick: (pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onNewsClick: (newsItem: NewsItem) -> Unit = {},
    onImageClick: (image: FeaturedImage) -> Unit = {},
    onImageDownloadClick: (image: FeaturedImage) -> Unit = {},
    onImageShareClick: (image: FeaturedImage, age: Int) -> Unit = { _, _ -> }
) {
    val activity = LocalActivity.current as? MainActivity
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = modifier.fillMaxHeight())
        }
        state.error != null && state.cards.isEmpty() -> {
            ErrorState(state.error, onRetry = onLoadMore)
        }
        else -> {
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
                state.cards.forEach { card ->
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
                                        onPageClick(
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    },
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        // TODO
                                    },
                                    onShareClick = {
                                        onPageShareClick(
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    },
                                    onBookmarkClick = {
                                        onPageBookmarkClick(
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        is TopReadListCard -> {
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
                                        // TODO: implement hide module functionality
                                    },
                                    onPageClick = { entry ->
                                        onPageClick(
                                            entry.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_MOST_READ
                                            )
                                        )
                                    },
                                    onPageOverflowClick = { pageSummary, index ->
                                        onPageOverflowClick(pageSummary, HistoryEntry.SOURCE_FEED_MOST_READ, "top-read-${card.age}-$index")
                                    },
                                    onFooterClick = {
                                        // TODO: simplify TopReadListCard after we remove the old feed UIs.
                                        activity?.startActivity(
                                            TopReadArticlesActivity.newIntent(
                                                activity,
                                                TopReadListCard(card.articles, card.age, wikiSite)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        is FeaturedImageCard -> {
                            item(key = "tfi-${card.age}") {
                                FeaturedImageModule(
                                    wikiSite = wikiSite,
                                    featuredImage = card.featuredImage,
                                    onHideCardClick = { onHideCardClick(card) },
                                    onClick = onImageClick,
                                    onDownloadClick = onImageDownloadClick,
                                    onShareClick = { onImageShareClick(card.featuredImage, card.age) }
                                )
                            }
                        }
                        is NewsCard -> {
                            item(key = "news-${card.age}") {
                                NewsModule(
                                    wikiSite = wikiSite,
                                    newsItems = card.news,
                                    onHideCardClick = { onHideCardClick(card) },
                                    onNewsClick = { newsItem ->
                                        onNewsClick(newsItem)
                                    },
                                    onHideModuleClick = {
                                        // TODO: implement overflow menu
                                    }
                                )
                            }
                        }
                        is OnThisDayCard -> {
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
                                        // TODO: implement overflow menu
                                    },
                                    onPageClick = { pageSummary ->
                                        onPageClick(pageSummary.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_ON_THIS_DAY))
                                    },
                                    onPageOverflowClick = { pageSummary, eventIndex, itemIndex ->
                                        onPageOverflowClick(pageSummary, HistoryEntry.SOURCE_FEED_ON_THIS_DAY, "on-this-day-${card.age}-$eventIndex-$itemIndex")
                                    },
                                    onFooterClick = {
                                        activity?.startActivity(OnThisDayActivity.newIntent(activity, card.age, -1, wikiSite, InvokeSource.ON_THIS_DAY_CARD_FOOTER))
                                    }
                                )
                            }
                        }
                        else -> {
                            // TODO: Today's Featured Picture
                            // TODO: DYK
                            // TODO: Media of the day (Commons)
                        }
                    }
                }

                item(key = "load-more-community") {
                    if (state.isLoadingMore) {
                        LoadingIndicator()
                    } else if (state.canLoadMore) {
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

@Composable
fun ForYouContentTab(
    state: ForYouContentState,
    wikiSite: WikiSite,
    onLoadMore: () -> Unit
) {
    val context = LocalContext.current
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = Modifier.fillMaxHeight())
        }
        state.error != null && state.modules.isEmpty() -> {
            ErrorState(state.error, onRetry = onLoadMore)
        }
        else -> {
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
                    itemsIndexed(modules) { _, module ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(viewportHeight)
                        ) {
                            AsyncImage(
                                model = ImageService.getRequest(context, url = module.pages.first().thumbnailUrl),
                                placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
                                error = ColorPainter(WikipediaTheme.colors.backgroundColor),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    item(key = "load-more-foryou") {
                        if (state.isLoadingMore) {
                            LoadingIndicator()
                        } else if (state.canLoadMore) {
                            LoadMoreButton(
                                wikiSite = wikiSite,
                                isCommunity = false,
                                onClick = onLoadMore
                            )
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

@Composable
fun CommunityDisclaimer(
    modifier: Modifier,
    wikiSite: WikiSite
) {
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = WikipediaTheme.colors.borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
            Image(
                modifier = Modifier.size(45.dp),
                painter = painterResource(R.drawable.w_nav_mark),
                contentDescription = null
            )
        }
    }
}
@Composable
fun DayHeader(date: LocalDate, isFirst: Boolean = true) {
    val dateFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(LocalLocale.current.platformLocale, "MMM dd, yyyy"))
    Text(
        text = if (LocalDate.now().dayOfYear == date.dayOfYear) stringResource(R.string.explore_feed_date_today, date.format(dateFormatter)) else date.format(dateFormatter),
        color = WikipediaTheme.colors.secondaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (isFirst) 16.dp else 24.dp)
    )
}

@Composable
fun LoadMoreButton(
    wikiSite: WikiSite,
    isCommunity: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isCommunity) {
            AppButton(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                onClick = onClick,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dynamic_feed_24dp),
                        tint = WikipediaTheme.colors.paperColor,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_load_more_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onClick) {
                    Text(
                        text = "Load more recommendations",
                        color = WikipediaTheme.colors.progressiveColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Composable
fun ErrorState(caught: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WikiErrorView(
            modifier = Modifier,
            caught,
            errorClickEvents = WikiErrorClickEvents(
                retryClickListener = {
                    onRetry()
                }
            ),
            retryForGenericError = true
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
                .border(width = 1.dp, color = WikipediaTheme.colors.primaryColor.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
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
fun HomeScreenCommunityPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true)
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
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CommunityDisclaimerPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityDisclaimer(
            modifier = Modifier
                .padding(16.dp)
                .height(72.dp),
            wikiSite = WikiSite.preview()
        )
    }
}

@Preview
@Composable
fun LoadMoreButtonPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LoadMoreButton(
            wikiSite = WikiSite.preview(),
            isCommunity = true,
            onClick = {}
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

@Preview
@Composable
fun DayHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        DayHeader(LocalDate.now())
    }
}
