package org.wikipedia.feed

import android.os.Bundle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.WikiLangCodeBox
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.news.NewsModule
import org.wikipedia.feed.onboarding.ExploreFeedUpdatePromptActivity
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
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()

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

                BaseTheme(currentTheme = if (selectedTab == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme) {
                    HomeScreen(
                        viewModel = viewModel,
                        wikiSite = wikiSite,
                        selectedTab = selectedTab,
                        communityContentState = viewModel.communityState.collectAsState().value,
                        forYouContentState = viewModel.forYouState.collectAsState().value,
                        onSelectTab = {
                            viewModel.selectTab(it)
                            (requireActivity() as? MainActivity)?.onTabChanged(NavTab.HOME)
                        },
                        onLoadMoreCommunityContent = viewModel::loadCommunityContent,
                        onLoadMoreForYouContent = viewModel::loadForYouContent,
                        onPageClick = {
                            (parentFragment as? MainFragment)?.onFeedSelectPage(it, false)
                        },
                        onPageBookmarkClick = {
                            (parentFragment as? MainFragment)?.onFeedAddPageToList(it, false)
                        },
                        onPageShareClick = {
                            ShareUtil.shareText(requireContext(), it.title)
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
    viewModel: HomeViewModel,
    wikiSite: WikiSite,
    selectedTab: HomeTab,
    communityContentState: CommunityContentState,
    forYouContentState: ForYouContentState,
    onSelectTab: (HomeTab) -> Unit = {},
    onLoadMoreCommunityContent: () -> Unit = {},
    onLoadMoreForYouContent: () -> Unit = {},
    onPageClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageBookmarkClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageShareClick: (historyEntry: HistoryEntry) -> Unit = {},
    onNewsClick: (newsItem: NewsItem) -> Unit = {},
    onImageClick: (image: FeaturedImage) -> Unit = {},
    onImageDownloadClick: (image: FeaturedImage) -> Unit = {},
    onImageShareClick: (image: FeaturedImage, age: Int) -> Unit = { _, _ -> },
    onLanguageSelected: (String) -> Unit = {},
    onManageLanguagesClick: () -> Unit = {}
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
        onRefresh = {
            if (selectedTab == HomeTab.COMMUNITY) {
                viewModel.refreshCommunityContent()
            } else {
                viewModel.refreshForYouContent()
            }
        },
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
                            modifier = Modifier.padding(top = 8.dp),
                            wikiSite = wikiSite,
                            selectedTab = selectedTab,
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
                            viewModel = viewModel,
                            wikiSite = wikiSite,
                            state = communityContentState,
                            onLoadMore = onLoadMoreCommunityContent,
                            onPageClick = onPageClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onPageShareClick = onPageShareClick,
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
                    HomeTab.COMMUNITY -> stringResource(R.string.explore_feed_community_tab_label)
                    HomeTab.FOR_YOU -> stringResource(R.string.explore_feed_for_you_tab_label)
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
            languageCodes = WikipediaApp.instance.languageState.appLanguageCodes,
            selectedLanguageCode = wikiSite.languageCode,
            onLanguageSelected = { onLanguageSelected(it) },
            onManageLanguagesClick = { onManageLanguagesClick() },
            languageState = WikipediaApp.instance.languageState
        )
    }
}

@Composable
fun CommunityContentTab(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    wikiSite: WikiSite,
    state: CommunityContentState,
    onLoadMore: () -> Unit,
    onPageClick: (historyEntry: HistoryEntry) -> Unit,
    onPageBookmarkClick: (historyEntry: HistoryEntry) -> Unit = {},
    onPageShareClick: (historyEntry: HistoryEntry) -> Unit = {},
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
        state.error != null && state.days.isEmpty() -> {
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
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                state.days.forEach { day ->

                    item(key = "day-header-${day.age}") {
                        DayHeader(day.date)
                    }

                    day.featuredArticle?.let { article ->
                        item(key = "tfa-${day.age}") {
                            FeaturedArticleModule(
                                article,
                                onPageClick = {
                                    onPageClick(it.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_FEATURED))
                                },
                                onOverflowClick = {
                                    // TODO
                                },
                                onShareClick = {
                                    onPageShareClick(it.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_FEATURED))
                                },
                                onBookmarkClick = {
                                    onPageBookmarkClick(it.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_FEATURED))
                                }
                            )
                        }
                    }

                    day.topRead?.let {
                        item(key = "top-read-${day.age}") {
                            TopReadModule(
                                topRead = it,
                                onOverflowClick = {
                                    // TODO: implement overflow menu
                                },
                                onPageClick = { entry ->
                                    onPageClick(entry.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_MOST_READ))
                                },
                                onPageOverflowClick = { pageSummary ->
                                    // TODO: implement page overflow menu
                                },
                                onFooterClick = {
                                    // TODO: simplify TopReadListCard after we remove the old feed UIs.
                                    activity?.startActivity(
                                        TopReadArticlesActivity.newIntent(activity, TopReadListCard(it, wikiSite))
                                    )
                                }
                            )
                        }
                    }

                    // TODO: insert Today's Featured Picture module here
                    // TODO: insert DYK module here

                    if (day.news.isNotEmpty()) {
                        item(key = "news-${day.age}") {
                            NewsModule(
                                newsItems = day.news,
                                onNewsClick = { newsItem ->
                                    onNewsClick(newsItem)
                                },
                                onOverflowClick = {
                                    // TODO: implement overflow menu
                                }
                            )
                        }
                    }

                    // TODO: insert On this day module here

                    day.featuredImage?.let { image ->
                        item(key = "tfi-${day.age}") {
                            FeaturedImageModule(
                                image,
                                onClick = onImageClick,
                                onDownloadClick = onImageDownloadClick,
                                onShareClick = { onImageShareClick(image, day.age) }
                            )
                        }
                    }

                    // TODO: insert Media of the day (Commons) module here
                }

                item(key = "load-more-community") {
                    if (state.isLoadingMore) {
                        LoadingIndicator()
                    } else if (state.canLoadMore) {
                        LoadMoreButton(isCommunity = true, onClick = onLoadMore)
                    }
                }

                if (state.error != null && state.days.isNotEmpty()) {
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
                            LoadMoreButton(isCommunity = false, onClick = onLoadMore)
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
    modifier: Modifier
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
                text = stringResource(R.string.explore_feed_community_disclaimer),
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
fun DayHeader(date: LocalDate) {
    Text(
        text = date.toString(),
        color = WikipediaTheme.colors.secondaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun LoadMoreButton(
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
                        text = stringResource(R.string.explore_feed_community_load_more_label),
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
    languageCodes: List<String>,
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
                    text = stringResource(R.string.explore_feed_manage_languages_label),
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
            viewModel = viewModel(),
            wikiSite = WikiSite("en.wikipedia.org"),
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
            viewModel = viewModel(),
            wikiSite = WikiSite("en.wikipedia.org"),
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
                .height(72.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadMoreButtonPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LoadMoreButton(isCommunity = true, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageDropDownMenuPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LanguageDropDownMenu(
            languageCodes = listOf("en", "es", "fr"),
            selectedLanguageCode = "en",
            onLanguageSelected = {},
            onManageLanguagesClick = {}
        )
    }
}
