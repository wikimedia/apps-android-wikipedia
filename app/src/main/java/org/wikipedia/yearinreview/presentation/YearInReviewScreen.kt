package org.wikipedia.yearinreview.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.rive.GetBitmapFun
import app.rive.Result
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.WikipediaTheme
import kotlin.math.roundToInt

private val YearInReviewCardCornerRadius = 28.dp
private val TopAppBarHeight = 64.dp

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
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {},
    onShareFeedbackClick: () -> Unit = {},
    onShareClick: (Bitmap) -> Unit = {},
    onDonateClick: (String) -> Unit = { _ -> },
    onRetryClick: () -> Unit = {},
    onStatusBarIconColorChange: (useDarkIcons: Boolean) -> Unit = {},
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
                showDonateButton = uiState.isDonationEligible,
                onCloseClick = onCloseClick,
                onLearnMoreClick = onLearnMoreClick,
                onShareFeedbackClick = onShareFeedbackClick,
                onShareClick = onShareClick,
                onDonateClick = onDonateClick,
                onStatusBarIconColorChange = onStatusBarIconColorChange,
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
                        backClickListener = onCloseClick
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
    showDonateButton: Boolean,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    onShareClick: (Bitmap) -> Unit,
    onDonateClick: (String) -> Unit,
    onStatusBarIconColorChange: (useDarkIcons: Boolean) -> Unit,
    onRiveError: (Throwable) -> Unit
) {
    val pagerState = rememberPagerState { pages.size }
    val useDarkStatusBarIcons by remember(pages) {
        derivedStateOf { pages.getOrNull(pagerState.currentPage)?.useDarkStatusBarIcons ?: false }
    }
    LaunchedEffect(useDarkStatusBarIcons) {
        onStatusBarIconColorChange(useDarkStatusBarIcons)
    }
    val screenshotGetters = remember { mutableStateMapOf<String, GetBitmapFun>() }
    val riveWorker = rememberYearInReviewRiveWorker(onRiveError)
    val riveResourceIds = remember(pages) {
        pages.mapNotNull { page -> page.riveSpec?.resourceId }.distinct()
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
                showDonateButton = showDonateButton && pages.isNotEmpty(),
                shareEnabled = pages.getOrNull(pagerState.currentPage)?.let { page ->
                    screenshotGetters.containsKey(page.id)
                } == true,
                onShareClick = {
                    val page = pages.getOrNull(pagerState.currentPage)
                    if (page != null) {
                        try {
                            screenshotGetters[page.id]?.invoke()?.let { bitmap ->
                                onShareClick(bitmap)
                            }
                        } catch (exception: Exception) {
                            onRiveError(exception)
                        }
                    }
                },
                onDonateClick = {
                    pages.getOrNull(pagerState.currentPage)?.let {
                        onDonateClick(it.id)
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
                key = { page -> pages[page].id }
            ) { position ->
                val page = pages[position]
                val riveSpec = page.riveSpec
                when {
                    riveSpec != null -> {
                        YearInReviewRiveSlide(
                            riveFileResult = riveFiles[riveSpec.resourceId] ?: Result.Loading,
                            spec = riveSpec,
                            // TODO: pass the user's own stats once the page carries them; the spec's instance shows the designer's sample text until then
                            textProperties = emptyMap(),
                            accessibilityDescription = "",
                            playing = pagerState.currentPage == position,
                            slideId = page.id,
                            screenshotGetters = screenshotGetters,
                            onRiveError = onRiveError
                        )
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

            YearInReviewTopBar(
                iconColor = if (useDarkStatusBarIcons) ComposeColors.Black else ComposeColors.White,
                onCloseClick = onCloseClick,
                onLearnMoreClick = onLearnMoreClick,
                onShareFeedbackClick = onShareFeedbackClick
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

private val YearInReviewPage.riveSpec: RiveSlideSpec?
    get() = when (this) {
        is YearInReviewPage.Cover -> allTemplatesSlideSpec("cover")
        is YearInReviewPage.ArticlesRead -> allTemplatesSlideSpec(if (isEmptyState) "frame1-empty" else "frame1")
        is YearInReviewPage.Visits -> allTemplatesSlideSpec("frame2")
        is YearInReviewPage.TimeSpent -> allTemplatesSlideSpec("frame3")
        is YearInReviewPage.ReadingStreak -> allTemplatesSlideSpec(if (isEmptyState) "frame4-empty" else "frame4")
        is YearInReviewPage.ReadingPattern -> allTemplatesSlideSpec(if (isEmptyState) "frame5-empty" else "frame5")
        is YearInReviewPage.TopTopic -> allTemplatesSlideSpec(if (isEmptyState) "frame6-empty" else "frame6")
        is YearInReviewPage.OtherTopTopics -> allTemplatesListSlideSpec("frame7")
        is YearInReviewPage.BiggestReadingDay -> allTemplatesSlideSpec("frame8")
        is YearInReviewPage.BiggestReadingDayArticles -> allTemplatesListSlideSpec("frame9")
        is YearInReviewPage.Category -> allTemplatesSlideSpec("frame13")
        is YearInReviewPage.RevisitedArticles -> allTemplatesListSlideSpec(if (isEmptyState) "frame12-empty" else "frame12")
        is YearInReviewPage.Geography -> allTemplatesListSlideSpec(if (isEmptyState) "frame14-empty" else "frame14")
        is YearInReviewPage.SavedArticles -> allTemplatesListSlideSpec(if (isEmptyState) "frame15-empty" else "frame15")
        is YearInReviewPage.TotalEdits -> allTemplatesSlideSpec(if (isEmptyState) "frame16-empty" else "frame16")
        is YearInReviewPage.EditedArticleViews -> allTemplatesSlideSpec("frame17")
        is YearInReviewPage.MostViewedEditedArticles -> allTemplatesListSlideSpec("frame18")
        is YearInReviewPage.ThankYou -> allTemplatesSlideSpec("end")
        // The file has no collective artboards yet, so these reuse the personal artboard closest to each insight
        is YearInReviewPage.Collective -> when (insight) {
            YearInReviewCollectiveInsight.HOURS_READ -> allTemplatesSlideSpec("frame3")
            YearInReviewCollectiveInsight.LANGUAGES -> allTemplatesSlideSpec("frame2")
            YearInReviewCollectiveInsight.ARTICLES -> allTemplatesSlideSpec("frame1")
            YearInReviewCollectiveInsight.SAVED_ARTICLES -> allTemplatesListSlideSpec("frame15")
            YearInReviewCollectiveInsight.VOLUNTEER_EDITORS -> allTemplatesSlideSpec("frame17")
            YearInReviewCollectiveInsight.EDITS -> allTemplatesSlideSpec("frame16")
            YearInReviewCollectiveInsight.GLOBAL_REACH -> allTemplatesListSlideSpec("frame14")
        }
        // TODO: else branch stands in for each page until its slide is built
        else -> null
    }

private val AllTemplatesGlobalProperties = RiveGlobalViewModel(
    name = "GlobalProperties",
    textSizes = mapOf(
        "headlineFontSize" to 24f,
        "headlineLineHeight" to 25.44f,
        "bodyCopyFontSize" to 16f,
        "bodyCopyLineHeight" to 20f
    )
)

// In all_templates.riv, each artboard has a matching state machine and a same-named view model instance holding sample text
private fun allTemplatesSlideSpec(artboardName: String, viewModelName: String = "DataTemplate") = RiveSlideSpec(
    resourceId = R.raw.all_templates,
    artboardName = artboardName,
    stateMachineName = "$artboardName-statemachine",
    viewModelName = viewModelName,
    instanceType = RiveInstanceType.Named(artboardName),
    globalViewModel = AllTemplatesGlobalProperties
)

// Artboards that show up to three articles or topics, each with an icon, title and subtitle
private fun allTemplatesListSlideSpec(artboardName: String) = allTemplatesSlideSpec(artboardName, viewModelName = "List")

@Composable
private fun YearInReviewProgressTracker(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 32.dp,
    indicatorWidth: Dp = 3.dp
) {
    if (pagerState.pageCount <= 1) {
        return
    }

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
                .clip(RoundedCornerShape(2.dp))
                .background(ComposeColors.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun YearInReviewTopBar(
    iconColor: Color,
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onShareFeedbackClick: () -> Unit
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        title = {
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
                tint = if (shareEnabled) WikipediaTheme.colors.progressiveColor else WikipediaTheme.colors.inactiveColor,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = stringResource(R.string.menu_page_article_share),
                style = MaterialTheme.typography.labelLarge,
                color = if (shareEnabled) WikipediaTheme.colors.progressiveColor else WikipediaTheme.colors.inactiveColor
            )
        }
        if (showDonateButton) {
            TextButton(onClick = onDonateClick) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_heart_24),
                    tint = WikipediaTheme.colors.destructiveColor,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.year_in_review_donate),
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.destructiveColor
                )
            }
        }
    }
}
