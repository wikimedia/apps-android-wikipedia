package org.wikipedia.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.feed.topread.TopReadModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val selectedTab by viewModel.selectedTab.collectAsState()

                BaseTheme(currentTheme = if (selectedTab == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme) {
                    HomeScreen(
                        viewModel = viewModel,
                        selectedTab = selectedTab,
                        communityContentState = viewModel.communityState.collectAsState().value,
                        forYouContentState = viewModel.forYouState.collectAsState().value,
                        onSelectTab = {
                            viewModel.selectTab(it)
                            (requireActivity() as? MainActivity)?.onTabChanged(NavTab.HOME)
                        },
                        onLoadMoreCommunityContent = viewModel::loadCommunityContent,
                        onLoaDMoreForYouContent = viewModel::loadForYouContent
                    )
                }
            }
        }
    }

    fun getCurrentTab(): HomeTab {
        return viewModel.selectedTab.value
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    selectedTab: HomeTab,
    communityContentState: CommunityContentState,
    forYouContentState: ForYouContentState,
    onSelectTab: (HomeTab) -> Unit = {},
    onLoadMoreCommunityContent: () -> Unit = {},
    onLoaDMoreForYouContent: () -> Unit = {}
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
                            selectedTab = selectedTab,
                            onTabSelected = onSelectTab
                        )

                        CommunityContentTab(
                            modifier = Modifier.weight(1f),
                            viewModel = viewModel,
                            state = communityContentState,
                            onLoadMore = onLoadMoreCommunityContent
                        )
                    }
                }

                HomeTab.FOR_YOU -> {
                    ForYouContentTab(
                        state = forYouContentState,
                        onLoadMore = onLoaDMoreForYouContent
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
                            selectedTab = selectedTab,
                            onTabSelected = onSelectTab
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
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
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
}

@Composable
fun CommunityContentTab(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    state: CommunityContentState,
    onLoadMore: () -> Unit
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
                state.days.forEach { day ->

                    item(key = "day-header-${day.age}") {
                        DayHeader(day.date)
                    }

                    day.featuredArticle?.let { article ->
                        item(key = "tfa-${day.age}") {
                            FeaturedArticleModule(article)
                        }
                    }

                    day.topRead?.let {
                        item(key = "top-read-${day.age}") {
                            TopReadModule(
                                topRead = it,
                                onOverflowClick = {
                                    // TODO: implement overflow menu
                                },
                                onPageClick = { pageSummary ->
                                    activity?.fragment?.onLoadPage(
                                        entry = pageSummary.getHistoryEntry(viewModel.wikiSite, HistoryEntry.SOURCE_FEED_MOST_READ)
                                    )
                                },
                                onPageOverflowClick = { pageSummary ->
                                    // TODO: implement page overflow menu
                                },
                                onFooterClick = {
                                    // TODO: simplify TopReadListCard after we remove the old feed UIs.
                                    activity?.startActivity(
                                        TopReadArticlesActivity.newIntent(activity, TopReadListCard(it, viewModel.wikiSite))
                                    )
                                }
                            )
                        }
                    }
                }

                item(key = "load-more-community") {
                    if (state.isLoadingMore) {
                        LoadingIndicator()
                    } else if (state.canLoadMore) {
                        LoadMoreButton(label = stringResource(R.string.explore_feed_load_previous_day_label), onClick = onLoadMore)
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
                            LoadMoreButton(label = "Load more recommendations", onClick = onLoadMore)
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
fun LoadMoreButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = label,
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
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

@Preview(showBackground = true)
@Composable
fun HomeScreenCommunityPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            viewModel = viewModel(),
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
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true)
        )
    }
}
