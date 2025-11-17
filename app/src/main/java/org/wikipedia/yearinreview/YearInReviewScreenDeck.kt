package org.wikipedia.yearinreview

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.util.UriUtil
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewScreenDeck(
    modifier: Modifier = Modifier,
    state: UiState<List<YearInReviewScreenData>>,
    requestScreenshotBitmap: ((Int, Int) -> Bitmap)?,
    onDonateClick: (String) -> Unit = {},
    onNextButtonClick: (PagerState, YearInReviewScreenData) -> Unit = { _, _ -> },
    onCloseButtonClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    when (state) {
        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Success -> {
            LaunchedEffect(Unit) {
                YearInReviewViewModel.updateYearInReviewModel { it.copy(slideViewedCount = it.slideViewedCount + 1) }
            }

            val pages = state.data
            val pagerState = rememberPagerState(pageCount = { pages.size })
            var captureRequest by remember { mutableStateOf<YearInReviewCaptureRequest?>(null) }

            captureRequest?.let { request ->
                YearInReviewScreenCaptureHandler(
                    request = request,
                    onComplete = {
                        captureRequest = null
                    }
                )
            }

            Scaffold(
                modifier = modifier
                    .safeDrawingPadding(),
                containerColor = WikipediaTheme.colors.paperColor,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = WikipediaTheme.colors.paperColor
                        ),
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = {
                                YearInReviewEvent.submit(action = "close_click", slide = pages[pagerState.currentPage].slideName)
                                onCloseButtonClick()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_black_24dp),
                                    tint = WikipediaTheme.colors.primaryColor,
                                    contentDescription = stringResource(R.string.year_in_review_close)
                                )
                            }
                        },
                        actions = {
                            if (pages[pagerState.currentPage].allowDonate && pages[pagerState.currentPage].showDonateInToolbar) {
                                Box(
                                    modifier = Modifier
                                        .clickable(onClick = {
                                            onDonateClick(pages[pagerState.currentPage].slideName)
                                        })
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .wrapContentWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_heart_24),
                                            tint = WikipediaTheme.colors.destructiveColor,
                                            contentDescription = stringResource(R.string.year_in_review_heart_icon),
                                        )

                                        Text(
                                            text = stringResource(R.string.year_in_review_donate),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = WikipediaTheme.colors.destructiveColor
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    MainBottomBar(
                        pages,
                        onNavigationRightClick = {
                            YearInReviewEvent.submit(action = "next_click", slide = pages[pagerState.currentPage].slideName)
                            onNextButtonClick(pagerState, pages[pagerState.currentPage])
                        },
                        pagerState = pagerState,
                        totalPages = pages.size,
                        onShareClick = {
                            YearInReviewEvent.submit(action = "share_click", slide = pages[pagerState.currentPage].slideName)
                            when (pages[pagerState.currentPage]) {
                                is YearInReviewScreenData.GeoScreen -> { captureRequest = YearInReviewCaptureRequest.GeoScreen(pages[pagerState.currentPage], requestScreenshotBitmap) }
                                is YearInReviewScreenData.StandardScreen -> { captureRequest = YearInReviewCaptureRequest.StandardScreen(pages[pagerState.currentPage]) }
                                is YearInReviewScreenData.HighlightsScreen -> {}
                            }
                        },
                        onBottomBtnClick = { screenData ->
                            when (screenData) {
                                is YearInReviewScreenData.HighlightsScreen -> {
                                    YearInReviewEvent.submit(action = "share_click", slide = pages[pagerState.currentPage].slideName)
                                    captureRequest =
                                        YearInReviewCaptureRequest.HighlightsScreen(screenData)
                                }
                                is YearInReviewScreenData.StandardScreen -> {
                                    onDonateClick(pages[pagerState.currentPage].slideName)
                                }
                                else -> {}
                            }
                        }
                    )
                },
                content = { paddingValues ->

                    LaunchedEffect(pagerState.currentPage) {
                        YearInReviewEvent.submit(
                            action = "impression",
                            slide = pages[pagerState.currentPage].slideName
                        )
                    }

                    HorizontalPager(
                        verticalAlignment = Alignment.Top,
                        state = pagerState,
                        contentPadding = PaddingValues(0.dp),
                    ) { page ->
                        YearInReviewScreenContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState()),
                            requestScreenshotBitmap = requestScreenshotBitmap,
                            screenData = pages[page]
                        )
                    }
                }
            )
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                WikiErrorView(
                    modifier = modifier.align(Alignment.Center),
                    caught = state.error,
                    errorClickEvents = WikiErrorClickEvents(
                        retryClickListener = {
                            onRetryClick()
                        },
                        backClickListener = {
                            onCloseButtonClick()
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun MainBottomBar(
    pages: List<YearInReviewScreenData>,
    pagerState: PagerState,
    totalPages: Int,
    onNavigationRightClick: () -> Unit,
    onShareClick: () -> Unit,
    onBottomBtnClick: (YearInReviewScreenData) -> Unit
) {
    val context = LocalContext.current
    val currentScreen = pages[pagerState.currentPage]
    Column {
        HorizontalDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(),
            color = WikipediaTheme.colors.borderColor
        )
        Box {
            pages[pagerState.currentPage].BottomButton(context, onBottomBtnClick)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            if (currentScreen !is YearInReviewScreenData.HighlightsScreen) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(R.string.year_in_review_share_icon)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                val animationDuration = 500
                repeat(totalPages) { iteration ->
                    val colorTransition by animateColorAsState(
                        targetValue = if (pagerState.currentPage == iteration) {
                            WikipediaTheme.colors.progressiveColor
                        } else {
                            WikipediaTheme.colors.inactiveColor
                        },
                        animationSpec = tween(durationMillis = animationDuration)
                    )
                    val sizeTransition by animateDpAsState(
                        targetValue = paginationSizeGradient(
                            totalIndicators = totalPages,
                            iteration = iteration,
                            pagerState = pagerState
                        ).dp,
                        animationSpec = tween(durationMillis = animationDuration)
                    )
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(colorTransition)
                            .align(Alignment.CenterVertically)
                            .size(sizeTransition)
                    )
                }
            }
            IconButton(
                onClick = { onNavigationRightClick() },
                modifier = Modifier
                    .padding(0.dp)
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.year_in_review_navigate_right)
                )
            }
        }
    }
}

@Composable
fun CreateScreenShotBitmap(
    screenContent: YearInReviewScreenData,
    requestScreenshotBitmap: ((Int, Int) -> Bitmap)?,
    onBitmapReady: (Bitmap) -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    var isImageLoaded by remember { mutableStateOf(false) }

    if (isImageLoaded) {
        LaunchedEffect(Unit) {
            val bitmap = graphicsLayer.toImageBitmap()
            onBitmapReady(bitmap.asAndroidBitmap())
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .background(color = WikipediaTheme.colors.paperColor)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wikipedia_b),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.app_name_prod),
                modifier = Modifier
                    .height(20.dp)
                    .width(31.dp)
            )
        }

        YearInReviewScreenContent(
            modifier = Modifier
                .padding(0.dp),
            screenData = screenContent,
            requestScreenshotBitmap = requestScreenshotBitmap,
            screenCaptureMode = true,
        ) {
            isLoaded -> isImageLoaded = isLoaded
        }

        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = processString(R.string.year_in_review_hashtag),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun YearInReviewScreenContent(
    modifier: Modifier = Modifier,
    screenData: YearInReviewScreenData,
    requestScreenshotBitmap: ((Int, Int) -> Bitmap)?,
    screenCaptureMode: Boolean = false,
    isOnboardingScreen: Boolean = false,
    isImageResourceLoaded: ((Boolean) -> Unit)? = null
) {
    when (screenData) {
        is YearInReviewScreenData.StandardScreen -> {
            StandardScreenContent(
                modifier = modifier,
                screenData = screenData,
                screenCaptureMode = screenCaptureMode,
                isOnboardingScreen = isOnboardingScreen,
                isImageResourceLoaded = isImageResourceLoaded,
            )
        }
        is YearInReviewScreenData.GeoScreen -> {
            GeoScreenContent(
                modifier = modifier,
                screenData = screenData,
                requestScreenshotBitmap = requestScreenshotBitmap,
                screenCaptureMode = screenCaptureMode,
                isImageResourceLoaded = isImageResourceLoaded,
            )
        }
        is YearInReviewScreenData.HighlightsScreen -> {
            YearInReviewHighlightsScreen(
                modifier = modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .yearInReviewHeaderBackground()
                    .padding(horizontal = 18.dp),
                screenData = screenData
            )
        }
    }
}

@Composable
private fun StandardScreenContent(
    modifier: Modifier = Modifier,
    screenData: YearInReviewScreenData.StandardScreen,
    screenCaptureMode: Boolean = false,
    isOnboardingScreen: Boolean = false,
    isImageResourceLoaded: ((Boolean) -> Unit)? = null,
) {
    val headerAspectRatio = 3f / 2f
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        screenData.Header(context, screenCaptureMode, isImageResourceLoaded, headerAspectRatio)
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 16.dp, end = 8.dp)
                        .height(IntrinsicSize.Min)
                        .weight(1f),
                    text = processString(screenData.headlineText),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!screenCaptureMode && !isOnboardingScreen) {
                    IconButton(
                        onClick = {
                            UriUtil.handleExternalLink(
                                context = context,
                                uri = context.getString(R.string.year_in_review_media_wiki_faq_url).toUri()
                            )
                        }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_info_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = stringResource(R.string.year_in_review_information_icon)
                        )
                    }
                }
            }
            if (screenData is YearInReviewScreenData.ReadingPatterns) {
                val readingPattersMap = mapOf(
                    screenData.favoriteTimeText to R.string.year_in_review_slide_reading_patterns_body_favorite_time,
                    screenData.favoriteDayText to R.string.year_in_review_slide_reading_patterns_body_favorite_day,
                    screenData.favoriteMonthText to R.string.year_in_review_slide_reading_patterns_body_favorite_month,
                )
                readingPattersMap.forEach { (title, description) ->
                    ReadingPatternsItem(
                        title = title,
                        description = description
                    )
                }
            } else {
                HtmlText(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .height(IntrinsicSize.Min),
                    text = processString(screenData.bodyText),
                    color = WikipediaTheme.colors.primaryColor,
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 16.sp
                        )
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ReadingPatternsItem(
    title: String,
    description: Int,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            text = processString(title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            text = processString(description),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(24.dp),
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Composable
fun processString(resource: Any?): String {
    return when (resource) {
        is Int -> stringResource(resource)
        else -> resource.toString()
    }
}

private fun paginationSizeGradient(totalIndicators: Int, iteration: Int, pagerState: PagerState): Int {
    return when {
        totalIndicators <= 3 -> 8
        (iteration - pagerState.currentPage).absoluteValue <= 2 -> 8
        (iteration - pagerState.currentPage).absoluteValue == 3 -> 4
        else -> 2
    }
}

@Preview
@Composable
fun PreviewScreenShot() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CreateScreenShotBitmap(
            screenContent = YearInReviewScreenData.StandardScreen(
                allowDonate = true,
                imageResource = R.drawable.yir_puzzle_browser,
                headlineText = "Over 3 billion bytes added",
                bodyText = "TBD",
                slideName = "test"
            ),
            requestScreenshotBitmap = null
        ) { /* No logic, preview only */ }
    }
}

@Preview
@Composable
fun PreviewStandardContent() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewScreenDeck(
            state = UiState.Success(listOf(
                YearInReviewScreenData.StandardScreen(
                    allowDonate = true,
                    imageResource = R.drawable.yir_puzzle_bytes,
                    headlineText = "Over 3 billion bytes added",
                    bodyText = "TBD",
                    slideName = "test"
                )
            )),
            requestScreenshotBitmap = null
        )
    }
}

@Preview
@Composable
fun PreviewReadingPatternsContent() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewScreenDeck(
            state = UiState.Success(listOf(
                YearInReviewScreenData.ReadingPatterns(
                    allowDonate = false,
                    imageResource = R.drawable.yir_puzzle_browser,
                    headlineText = "You have clear reading patterns",
                    bodyText = "",
                    favoriteTimeText = "Afternoon",
                    favoriteDayText = "Wednesday",
                    favoriteMonthText = "February",
                    slideName = "test"
                )
            )),
            requestScreenshotBitmap = null
        )
    }
}

@Preview
@Composable
fun PreviewScreenDeckError() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewScreenDeck(
            state = UiState.Error(Exception("Error")),
            requestScreenshotBitmap = null
        )
    }
}
