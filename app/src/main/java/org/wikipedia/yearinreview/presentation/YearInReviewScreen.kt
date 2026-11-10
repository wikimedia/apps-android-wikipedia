package org.wikipedia.yearinreview.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.rive.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.components.menu.PageOverflowMenuItem
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.data.YearInReviewEndScreenABTest
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val YearInReviewCardCornerRadius = 28.dp
internal val TopAppBarHeight = 64.dp
private const val SWIPE_TUTORIAL_DELAY_MILLIS = 2000L

fun Modifier.placeholderBackground() = background(
    brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color(0xFF030303),
            0.3f to Color(0xFF010004),
            0.48f to Color(0xFF03072C),
            0.63f to Color(0xFF01124F),
            0.82f to Color(0xFF042C95),
            1f to Color(0xFF0050A6)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )
)

@Composable
fun YearInReviewScreen(
    uiState: YearInReviewUiState,
    isSoundOn: Boolean,
    modifier: Modifier = Modifier,
    tabArticles: List<PageTitle> = emptyList(),
    tabArticleOverflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onCloseClick: (YearInReviewPage?) -> Unit = {},
    onLearnMoreClick: () -> Unit = {},
    onAboutInsightsClick: () -> Unit = {},
    onShareFeedbackClick: () -> Unit = {},
    onSoundToggleClick: () -> Unit = {},
    onShareClick: (YearInReviewPage, Bitmap) -> Unit = { _, _ -> },
    onDonateClick: (YearInReviewPage) -> Unit = { _ -> },
    onEndCardClick: (YearInReviewEndCard) -> Unit = {},
    onShareHighlightsClick: (Bitmap) -> Unit = {},
    onTabArticleClick: (PageTitle) -> Unit = {},
    onTabArticleOverflowClick: (title: PageTitle, menuKey: String) -> Unit = { _, _ -> },
    onTabArticleOverflowDismiss: () -> Unit = {},
    onTabArticleOverflowItemClick: (PageOverflowMenuItem, HistoryEntry) -> Unit = { _, _ -> },
    onRetryClick: () -> Unit = {},
    onStatusBarIconColorChange: (useDarkIcons: Boolean) -> Unit = {},
    onPageImpression: (YearInReviewPage) -> Unit = {},
    onPageSettled: (YearInReviewPage) -> Unit = {},
    onRiveError: (Throwable) -> Unit = {}
) {
    when (uiState) {
        YearInReviewUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
        }

        is YearInReviewUiState.Content -> {
            YearInReviewContent(
                modifier = modifier,
                year = uiState.year,
                pages = uiState.pages,
                readingStats = uiState.readingStats,
                showDonateButton = uiState.isDonationEligible,
                canShareHighlights = uiState.isPersonalized && uiState.canShareHighlights,
                tabArticles = tabArticles,
                tabArticleOverflowMenuState = tabArticleOverflowMenuState,
                isSoundOn = isSoundOn,
                onCloseClick = onCloseClick,
                onLearnMoreClick = onLearnMoreClick,
                onAboutInsightsClick = onAboutInsightsClick,
                onShareFeedbackClick = onShareFeedbackClick,
                onSoundToggleClick = onSoundToggleClick,
                onShareClick = onShareClick,
                onDonateClick = onDonateClick,
                onEndCardClick = onEndCardClick,
                onShareHighlightsClick = onShareHighlightsClick,
                onTabArticleClick = onTabArticleClick,
                onTabArticleOverflowClick = onTabArticleOverflowClick,
                onTabArticleOverflowDismiss = onTabArticleOverflowDismiss,
                onTabArticleOverflowItemClick = onTabArticleOverflowItemClick,
                onStatusBarIconColorChange = onStatusBarIconColorChange,
                onPageImpression = onPageImpression,
                onPageSettled = onPageSettled,
                onRiveError = onRiveError
            )
        }

        is YearInReviewUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
            ) {
                WikiErrorView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    caught = uiState.error,
                    errorClickEvents = WikiErrorClickEvents(
                        retryClickListener = onRetryClick,
                        backClickListener = { onCloseClick(null) }
                    )
                )
            }
        }
    }
}

@Composable
private fun YearInReviewContent(
    year: Int,
    pages: List<YearInReviewPage>,
    readingStats: YearInReviewReadingStats,
    showDonateButton: Boolean,
    canShareHighlights: Boolean,
    tabArticles: List<PageTitle>,
    tabArticleOverflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState?,
    isSoundOn: Boolean,
    modifier: Modifier = Modifier,
    onCloseClick: (YearInReviewPage) -> Unit,
    onLearnMoreClick: () -> Unit,
    onAboutInsightsClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    onSoundToggleClick: () -> Unit,
    onShareClick: (YearInReviewPage, Bitmap) -> Unit,
    onDonateClick: (YearInReviewPage) -> Unit,
    onEndCardClick: (YearInReviewEndCard) -> Unit,
    onShareHighlightsClick: (Bitmap) -> Unit,
    onTabArticleClick: (PageTitle) -> Unit,
    onTabArticleOverflowClick: (title: PageTitle, menuKey: String) -> Unit,
    onTabArticleOverflowDismiss: () -> Unit,
    onTabArticleOverflowItemClick: (PageOverflowMenuItem, HistoryEntry) -> Unit,
    onStatusBarIconColorChange: (useDarkIcons: Boolean) -> Unit,
    onPageImpression: (YearInReviewPage) -> Unit,
    onPageSettled: (YearInReviewPage) -> Unit,
    onRiveError: (Throwable) -> Unit
) {
    val pagerState = rememberPagerState { pages.size }
    // Passes on the fling velocity left over by a slide's scrollable content, like the end slide's, which the default
    // connection drops, so that a fast fling changes slides there as it does on the other slides
    val defaultPageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Vertical)
    val pageNestedScrollConnection = remember(defaultPageNestedScrollConnection) {
        object : NestedScrollConnection by defaultPageNestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = Velocity.Zero
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val currentOnPageImpression by rememberUpdatedState(onPageImpression)
    val currentOnPageSettled by rememberUpdatedState(onPageSettled)
    LaunchedEffect(pagerState, pages) {
        snapshotFlow { pagerState.settledPage }.collect { position ->
            pages.getOrNull(position)?.let {
                currentOnPageSettled(it)
                currentOnPageImpression(it)
            }
        }
    }

    var showSwipeTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        if (!Prefs.isYearInReviewSwipeTutorialShown) {
            val swipedBeforeTutorial = withTimeoutOrNull(SWIPE_TUTORIAL_DELAY_MILLIS.milliseconds) {
                snapshotFlow { pagerState.isScrollInProgress }.first { it }
            } != null
            if (swipedBeforeTutorial) {
                Prefs.isYearInReviewSwipeTutorialShown = true
            } else {
                showSwipeTutorial = true
            }
        }
    }
    if (showSwipeTutorial) {
        val dismissSwipeTutorial = {
            showSwipeTutorial = false
            Prefs.isYearInReviewSwipeTutorialShown = true
        }
        BaseTheme(currentTheme = Theme.BLACK) {
            WikipediaAlertDialog(
                title = stringResource(R.string.year_in_review_swipe_tutorial_title),
                titleModifier = Modifier.fillMaxWidth(),
                message = stringResource(R.string.year_in_review_swipe_tutorial_message),
                image = {
                    Image(
                        painter = painterResource(R.drawable.illustration_swipeup),
                        contentDescription = null
                    )
                },
                confirmButtonText = stringResource(R.string.onboarding_got_it),
                onDismissRequest = dismissSwipeTutorial,
                onConfirmButtonClick = dismissSwipeTutorial
            )
        }
    }

    // Slides are restarted once they're off screen, so that scrolling back to one plays it from the beginning
    val restartKeysByPageId = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(pagerState, pages) {
        val playedPageIds = mutableSetOf<String>()
        launch {
            snapshotFlow { pagerState.currentPage }.collect { position ->
                pages.getOrNull(position)?.let { playedPageIds += it.id }
            }
        }
        snapshotFlow { pagerState.settledPage }.collect { position ->
            val settledPageId = pages.getOrNull(position)?.id
            playedPageIds.filter { it != settledPageId }.forEach { pageId ->
                restartKeysByPageId[pageId] = (restartKeysByPageId[pageId] ?: 0) + 1
                playedPageIds -= pageId
            }
        }
    }
    val useWhiteTopBarIconsByPageId = remember { mutableStateMapOf<String, Boolean>() }
    val useDarkStatusBarIcons by remember(pages) {
        derivedStateOf {
            pages.getOrNull(pagerState.currentPage)?.let { page ->
                useWhiteTopBarIconsByPageId[page.id]?.not() ?: page.useDarkStatusBarIcons
            } ?: false
        }
    }
    LaunchedEffect(useDarkStatusBarIcons) {
        onStatusBarIconColorChange(useDarkStatusBarIcons)
    }
    var isRenderingShareCard by remember { mutableStateOf(false) }
    val screenshotGetters = remember { mutableStateMapOf<String, RiveSlideScreenshotFun>() }
    val riveWorker = rememberYearInReviewRiveWorker(onRiveError)
    val resources = LocalResources.current
    val context = LocalContext.current
    val riveContents = remember(pages) {
        pages.associate { page -> page.id to YearInReviewRiveContentMapper.map(page, resources) }
    }
    val riveResourceIds = remember(riveContents) {
        riveContents.values.mapNotNull { it?.spec?.resourceId }.distinct()
    }
    // Referenced fonts must be registered before the file loads, since Rive resolves them while loading it
    val riveFontsResult = rememberYearInReviewRiveFonts(riveWorker, YearInReviewRiveFonts)
    val riveFiles = rememberYearInReviewRiveFiles(
        riveWorker = riveWorker,
        resourceIds = riveResourceIds,
        riveFontsResult = riveFontsResult
    )
    InstallRiveSystemFontFallback()
    Scaffold(
        modifier = modifier,
        containerColor = ComposeColors.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            YearInReviewBottomBar(
                showDonateButton = showDonateButton && pages.isNotEmpty() &&
                        pages.getOrNull(pagerState.currentPage) !is YearInReviewPage.YouMatter,
                shareEnabled = pages.getOrNull(pagerState.currentPage)?.let { page ->
                    screenshotGetters.containsKey(page.id)
                } == true,
                onShareClick = {
                    val getScreenshot = pages.getOrNull(pagerState.currentPage)?.let { screenshotGetters[it.id] }
                    if (getScreenshot != null) {
                        coroutineScope.launch {
                            try {
                                pages.getOrNull(pagerState.currentPage)?.let { page ->
                                    onShareClick(page, YearInReviewShareImage.decorate(context, getScreenshot()))
                                }
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (exception: Exception) {
                                onRiveError(exception)
                            }
                        }
                    }
                },
                onDonateClick = {
                    pages.getOrNull(pagerState.currentPage)?.let {
                        onDonateClick(it)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            VerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = RoundedCornerShape(
                        bottomStart = YearInReviewCardCornerRadius,
                        bottomEnd = YearInReviewCardCornerRadius
                    )),
                state = pagerState,
                beyondViewportPageCount = if (pages.size > 1) 1 else 0,
                key = { page -> pages[page].id },
                pageNestedScrollConnection = pageNestedScrollConnection
            ) { position ->
                val page = pages[position]
                val content = riveContents[page.id]
                when {
                    page is YearInReviewPage.End -> {
                        YearInReviewEndSlide(
                            isDonor = page.isDonor,
                            isTestGroupUser = YearInReviewEndScreenABTest().isUserEligible() && YearInReviewEndScreenABTest().isTestGroupUser(),
                            hasEnglishAppLanguage = WikipediaApp.instance.languageState.appLanguageCodes.contains("en"),
                            canShareHighlights = canShareHighlights,
                            tabArticles = tabArticles,
                            tabArticleOverflowMenuState = tabArticleOverflowMenuState,
                            onCardActionClick = onEndCardClick,
                            onShareHighlightsClick = { isRenderingShareCard = true },
                            onTabArticleClick = onTabArticleClick,
                            onTabArticleOverflowClick = onTabArticleOverflowClick,
                            onTabArticleOverflowDismiss = onTabArticleOverflowDismiss,
                            onTabArticleOverflowItemClick = onTabArticleOverflowItemClick
                        )
                    }
                    // A custom slide, such as an interactive one, gets its own branch above this one
                    content != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            YearInReviewRiveSlide(
                                riveFileResult = riveFiles[content.spec.resourceId] ?: Result.Loading,
                                content = content,
                                playing = pagerState.currentPage == position,
                                restartKey = restartKeysByPageId[page.id] ?: 0,
                                slideId = page.id,
                                screenshotGetters = screenshotGetters,
                                onUseWhiteTopBarIconsChange = { useWhiteTopBarIconsByPageId[page.id] = it },
                                onRiveError = onRiveError
                            )
                            if (page is YearInReviewPage.YouMatter && showDonateButton) {
                                YearInReviewSlideDonateButton(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    onClick = { onDonateClick(page) }
                                )
                            }
                        }
                    }
                    // TODO: else branch stands in for each page until its slide is built
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .placeholderBackground(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = page.id,
                                color = ComposeColors.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            if (isRenderingShareCard) {
                YearInReviewShareCardRenderer(
                    year = year,
                    readingStats = readingStats,
                    onRendered = { bitmap ->
                        isRenderingShareCard = false
                        onShareHighlightsClick(bitmap)
                    },
                    onError = { throwable ->
                        isRenderingShareCard = false
                        onRiveError(throwable)
                    }
                )
            }

            YearInReviewTopBar(
                iconColor = if (useDarkStatusBarIcons) ComposeColors.Black else ComposeColors.White,
                onCloseClick = {
                    pages.getOrNull(pagerState.currentPage)?.let {
                        onCloseClick(it)
                    }
                },
                onLearnMoreClick = onLearnMoreClick,
                onShareFeedbackClick = onShareFeedbackClick,
                onAboutInsightsClick = onAboutInsightsClick,
                isSoundOn = isSoundOn,
                onSoundToggleClick = onSoundToggleClick
            )

            YearInReviewProgressTracker(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopAppBarHeight,
                        bottom = YearInReviewCardCornerRadius,
                        end = 1.dp
                    )
            )
        }
    }
}

@Composable
private fun YearInReviewProgressTracker(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 32.dp,
    indicatorWidth: Dp = 4.dp
) {
    if (pagerState.pageCount <= 1) {
        return
    }
    val indicatorShape = RoundedCornerShape(2.dp)

    BoxWithConstraints(
        modifier = modifier.width(indicatorWidth)
    ) {

        Box(
            modifier = Modifier
                .offset {
                    val pagePosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (pagerState.pageCount - 1).toFloat())
                    val progress = pagePosition / (pagerState.pageCount - 1)
                    val availableHeight = (constraints.maxHeight - indicatorHeight.roundToPx())
                        .coerceAtLeast(0)
                    IntOffset(
                        x = 0,
                        y = (availableHeight * progress).roundToInt()
                    )
                }
                .width(indicatorWidth)
                .height(indicatorHeight)
                .background(Color.White, indicatorShape)
                .border(0.5.dp, Color.Black, indicatorShape)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun YearInReviewTopBar(
    iconColor: Color,
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onAboutInsightsClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    showWikipediaLogo: Boolean = true,
    isSoundOn: Boolean? = null,
    onSoundToggleClick: () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        windowInsets = windowInsets,
        title = {
            if (showWikipediaLogo) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(R.drawable.ic_wikipedia_w),
                        tint = iconColor,
                        contentDescription = stringResource(R.string.year_in_review_topbar_w_icon)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_black_24dp),
                    tint = iconColor,
                    contentDescription = stringResource(R.string.year_in_review_close)
                )
            }
        },
        actions = {
            Box {
                IconButton(
                    onClick = {
                        overflowMenuExpanded = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        tint = iconColor,
                        contentDescription = stringResource(R.string.menu_feed_overflow_label)
                    )
                }
                DropdownMenu(
                    offset = DpOffset(x = (-16).dp, y = 0.dp),
                    expanded = overflowMenuExpanded,
                    onDismissRequest = { overflowMenuExpanded = false },
                    containerColor = WikipediaTheme.colors.paperColor
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_info_24),
                                tint = WikipediaTheme.colors.secondaryColor,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.year_in_review_learn_more),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor
                            )
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            onLearnMoreClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_info_24),
                                tint = WikipediaTheme.colors.secondaryColor,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.year_in_review_about_your_insights),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor
                            )
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            onAboutInsightsClick()
                        }
                    )
                    if (isSoundOn != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(if (isSoundOn) R.drawable.volume_off_24dp else R.drawable.volume_up_24dp),
                                    tint = WikipediaTheme.colors.secondaryColor,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(if (isSoundOn) R.string.on_this_day_game_sound_off else R.string.on_this_day_game_sound_on),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WikipediaTheme.colors.primaryColor
                                )
                            },
                            onClick = {
                                overflowMenuExpanded = false
                                onSoundToggleClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat_24dp),
                                tint = WikipediaTheme.colors.secondaryColor,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.year_in_review_share_feedback),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor
                            )
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            onShareFeedbackClick()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun YearInReviewBottomBar(
    showDonateButton: Boolean,
    shareEnabled: Boolean,
    onShareClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onShareClick,
            enabled = shareEnabled
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_share),
                tint = if (shareEnabled) Color.White else Color.Transparent,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = stringResource(R.string.menu_page_article_share),
                style = MaterialTheme.typography.labelLarge,
                color = if (shareEnabled) Color.White else Color.Transparent
            )
        }
        if (showDonateButton) {
            TextButton(onClick = onDonateClick) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_heart_24),
                    tint = Color.White,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.year_in_review_donate),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun YearInReviewSlideDonateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The slide artwork looks the same in every app theme, so the button uses fixed colors
    AppButton(
        modifier = modifier,
        backgroundColor = Color.Black,
        contentColor = Color.White,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_heart_24),
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(R.string.year_in_review_donate),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview
@Composable
private fun YearInReviewSlideDonateButtonPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        YearInReviewSlideDonateButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        )
    }
}
