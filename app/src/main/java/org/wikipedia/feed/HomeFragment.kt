package org.wikipedia.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.UtcDate
import org.wikipedia.main.MainActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.imageservice.ImageService

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
                        selectedTab,
                        communityContentState = viewModel.communityState.collectAsState().value,
                        forYouContentState = viewModel.forYouState.collectAsState().value,
                        onSelectTab = viewModel::selectTab,
                        onLoadMoreCommunityContent = viewModel::loadCommunityContent,
                        onLoaDMoreForYouContent = viewModel::loadForYouContent
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
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

    Box(modifier = Modifier.fillMaxSize()) {

        // Tab content — switches in place based on selected tab.
        when (selectedTab) {
            HomeTab.COMMUNITY -> CommunityContentTab(
                state = communityContentState,
                onLoadMore = onLoadMoreCommunityContent
            )
            HomeTab.FOR_YOU -> ForYouContentTab(
                state = forYouContentState,
                onLoadMore = onLoaDMoreForYouContent
            )
        }

        // Floating toolbar with gradient scrim, wordmark, and tab selector.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(if (selectedTab == HomeTab.FOR_YOU)
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
                    ) else SolidColor(Color.Transparent)
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
                HomeTab.COMMUNITY -> "From the community"
                HomeTab.FOR_YOU -> "For you"
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
    state: CommunityContentState,
    onLoadMore: () -> Unit
) {
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = Modifier.fillMaxHeight())
        }
        state.error != null && state.days.isEmpty() -> {
            ErrorState(message = state.error.message, onRetry = onLoadMore)
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.backgroundColor),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 200.dp, bottom = 16.dp)
            ) {
                state.days.forEach { day ->

                    item(key = "day-header-${day.age}") {
                        DayHeader(day.date)
                    }

                    day.featuredArticle?.let { article ->
                        item(key = "tfa-${day.age}") {
                            FeaturedArticleCard(article)
                        }
                    }

                    // TODO: all the other types of content for this day.
                }

                item(key = "load-more-community") {
                    if (state.isLoadingMore) {
                        LoadingIndicator()
                    } else if (state.canLoadMore) {
                        LoadMoreButton(label = "Load previous day", onClick = onLoadMore)
                    }
                }

                if (state.error != null && state.days.isNotEmpty()) {
                    item(key = "error-community") {
                        ErrorState(message = state.error.message, onRetry = onLoadMore)
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
            ErrorState(message = state.error.message, onRetry = onLoadMore)
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
                            ErrorState(message = state.error.message, onRetry = onLoadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayHeader(date: UtcDate) {
    Text(
        text = "${date.month}/${date.day}/${date.year}",
        color = WikipediaTheme.colors.primaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun FeaturedArticleCard(article: PageSummary) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                // TODO: navigate.
            }
    ) {
        AsyncImage(
            model = article.thumbnailUrl?.let { ImageService.getRequest(context, url = it) },
            placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
            error = ColorPainter(WikipediaTheme.colors.backgroundColor),
            contentDescription = article.displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            IconButton(onClick = { /* TODO: bookmark */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_border_white_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { /* TODO: share */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WikipediaTheme.colors.paperColor.copy(alpha = 0.90f))
                .padding(16.dp)
        ) {
            HtmlText(
                text = article.displayTitle,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif
                )
            )
            article.description?.let { description ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = WikipediaTheme.colors.secondaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            article.extract?.let { extract ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp).width(48.dp),
                    thickness = 1.dp,
                    color = WikipediaTheme.colors.secondaryColor.copy(alpha = 0.2f)
                )
                Text(
                    text = extract,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
fun ErrorState(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "Something went wrong",
            color = WikipediaTheme.colors.destructiveColor,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedArticleCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        FeaturedArticleCard(
            article = PageSummary("Lorem ipsum", "Lorem ipsum", "Lorem ipsum", "Lorem ipsum", thumbnail = "", "")
        )
    }
}


@Preview(showBackground = true)
@Composable
fun CommunityContentTabPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityContentTab(
            state = CommunityContentState(isInitialLoading = true),
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ForYouContentTabPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouContentTab(
            state = ForYouContentState(isInitialLoading = true),
            onLoadMore = {}
        )
    }
}
