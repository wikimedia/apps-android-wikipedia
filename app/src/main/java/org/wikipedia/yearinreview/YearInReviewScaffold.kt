package org.wikipedia.yearinreview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.nonEnglishCollectiveEditCountData
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewScreen(
    context: Context = LocalContext.current,
    customBottomBar: @Composable (PagerState) -> Unit,
    screenContent: @Composable (PaddingValues, YearInReviewScreenData, PagerState) -> Unit,
    navController: NavHostController,
    contentData: List<YearInReviewScreenData>,
    canShowSurvey: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { contentData.size })
    var startCapture by remember { mutableStateOf(false) }

    if (startCapture) {
        CreateScreenShotBitmap(
            screenContent = contentData[pagerState.currentPage],
            context = context,
        ) { bitmap ->
            ShareUtil.shareImage(
                coroutineScope = coroutineScope,
                context = context,
                bmp = bitmap,
                imageFileName = "year_in_review",
                subject = context.getString(R.string.year_in_review_share_subject),
                text = context.getString(R.string.year_in_review_share_url)
            )
            startCapture = false
        }
    }

    Scaffold(
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor),
                title = {
                    Icon(
                        painter = painterResource(R.drawable.ic_w_transparent),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(R.string.year_in_review_topbar_w_icon)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (contentData.size > 1 && pagerState.currentPage != 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        } else if (navController.currentDestination?.route == YearInReviewNavigation.Onboarding.name) {
                            canShowSurvey?.invoke()
                        } else {
                            navController.navigate(
                                route = YearInReviewNavigation.Onboarding.name)
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = stringResource(R.string.year_in_review_navigate_left)
                        )
                    }
                },
                actions = {
                    if (contentData.size > 1) {
                        IconButton(onClick = {
                            startCapture = true
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = stringResource(R.string.year_in_review_share_icon)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = { customBottomBar(pagerState) },
    ) { innerPadding ->
        if (contentData.size > 1) {
            HorizontalPager(
                verticalAlignment = Alignment.Top,
                state = pagerState,
                contentPadding = PaddingValues(0.dp),
            ) { page ->
                screenContent(innerPadding, contentData[page], pagerState)
            }
        } else {
            screenContent(innerPadding, contentData[0], pagerState)
        }
    }
}

@Composable
fun MainBottomBar(
    pagerState: PagerState,
    totalPages: Int,
    onNavigationRightClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    Column {
        HorizontalDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(),
            color = WikipediaTheme.colors.inactiveColor
        )
        BottomAppBar(
            containerColor = WikipediaTheme.colors.paperColor,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .wrapContentWidth()
                            .align(Alignment.CenterStart)
                            .clickable(
                                onClick = { onDonateClick() }
                            ),
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
                    if (pagerState.currentPage + 1 < totalPages) {
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
        )
    }
}

@Composable
fun OnboardingBottomBar(
    onGetStartedClick: () -> Unit,
    context: Context
) {
    BottomAppBar(
        containerColor = WikipediaTheme.colors.paperColor,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        contentColor = WikipediaTheme.colors.progressiveColor),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = {
                        UriUtil.handleExternalLink(
                            context = context,
                            uri = context.getString(R.string.year_in_review_media_wiki_url).toUri()
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.year_in_review_learn_more),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = WikipediaTheme.colors.paperColor
                    ),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = { onGetStartedClick() }
                ) {
                    Text(
                        text = stringResource(R.string.year_in_review_get_started),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    )
}

@Composable
fun YearInReviewScreenContent(
    innerPadding: PaddingValues,
    screenData: YearInReviewScreenData,
    context: Context,
    screenCaptureMode: Boolean = false,
    isOnboardingScreen: Boolean = false,
    isImageResourceLoaded: ((Boolean) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val gifAspectRatio = 3f / 2f

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(scrollState)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(if (screenCaptureMode) screenData.staticImageResource else screenData.animatedImageResource)
                .allowHardware(false)
                .build(),
            loading = { LoadingIndicator() },
            success = { SubcomposeAsyncImageContent() },
            onSuccess = { isImageResourceLoaded?.invoke(true) },
            contentDescription = stringResource(R.string.year_in_review_screendeck_image_content_description),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(gifAspectRatio)
                .clip(RoundedCornerShape(16.dp))
        )
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
                    text = processString(screenData.headLineText),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!screenCaptureMode && !isOnboardingScreen) {
                    IconButton(
                        onClick = {
                            UriUtil.handleExternalLink(
                                context = context,
                                uri = context.getString(R.string.year_in_review_media_wiki_url).toUri()
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
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(IntrinsicSize.Min),
                text = processString(screenData.bodyText),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CreateScreenShotBitmap(
    screenContent: YearInReviewScreenData,
    context: Context,
    onBitmapReady: (Bitmap) -> Unit
) {
    val shadowColor = WikipediaTheme.colors.primaryColor
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
            .fillMaxSize()
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .background(color = WikipediaTheme.colors.paperColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wikipedia_b),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.year_in_review_navigate_left),
                modifier = Modifier
                    .height(32.dp)
                    .width(50.dp)
            )
        }
        YearInReviewScreenContent(
            innerPadding = PaddingValues(0.dp),
            screenData = screenContent,
            screenCaptureMode = true,
            context = context
        ) { isLoaded -> isImageLoaded = isLoaded }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = WikipediaTheme.colors.paperColor
            ),
            modifier = Modifier
                .width(312.dp)
                .padding(top = 36.dp)
                .drawBehind {
                    /* Manually creating card shadow for render compatibility with graphicsLayer.toImageBitmap() */
                    val paint = Paint().asFrameworkPaint().apply {
                        color = shadowColor.copy(alpha = 0.15f).toArgb()
                        maskFilter = BlurMaskFilter(
                            20f,
                            BlurMaskFilter.Blur.NORMAL
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        16f,
                        16f,
                        paint
                    )
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 11.dp)

            ) {
                Image(
                    painter = painterResource(R.drawable.globe),
                    contentDescription = stringResource(R.string.year_in_review_globe_icon)
                )
                Text(
                    text = "#WikipediaYearInReview",
                    color = WikipediaTheme.colors.progressiveColor,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
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
private fun processString(resource: Any?): String {
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
fun PreviewContent() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewScreen(
            customBottomBar = { pagerState ->
                MainBottomBar(
                    pagerState = pagerState,
                    totalPages = 1,
                    onNavigationRightClick = { /* No logic, preview only */ },
                    onDonateClick = { /* No logic, preview only */ }
                )
            },
            screenContent = { innerPadding, screenData, pagerState ->
                YearInReviewScreenContent(
                    innerPadding = innerPadding,
                    screenData = screenData,
                    context = LocalContext.current
                )
            },
            navController = NavHostController(LocalContext.current),
            contentData = listOf(nonEnglishCollectiveEditCountData),
        )
    }
}

@Preview
@Composable
fun PreviewScreenShot() {
    val context = LocalContext.current
    BaseTheme(currentTheme = Theme.LIGHT) {
        CreateScreenShotBitmap(
            screenContent = nonEnglishCollectiveEditCountData,
            context = context
        ) { /* No logic, preview only */ }
    }
}
