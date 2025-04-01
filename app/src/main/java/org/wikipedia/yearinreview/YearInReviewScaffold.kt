package org.wikipedia.yearinreview

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewScreen(
    customBottomBar: @Composable (PagerState) -> Unit,
    screenContent: @Composable (PaddingValues, YearInReviewScreenData) -> Unit,
    navController: NavHostController,
    contentData: List<YearInReviewScreenData>,
    totalPages: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { totalPages })
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
                        if (totalPages > 1 && pagerState.currentPage != 0) {
                            coroutineScope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
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
                    if (totalPages > 1) {
                        IconButton(onClick = { /* TODO() */ }) {
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
        if (totalPages > 1) {
            HorizontalPager(
                verticalAlignment = Alignment.Top,
                state = pagerState,
                contentPadding = PaddingValues(0.dp),
            ) { page ->
                screenContent(innerPadding, contentData[page])
            }
        } else {
            screenContent(innerPadding, contentData[0])
        }
    }
}

@Composable
fun MainBottomBar(
    onNavigationRightClick: () -> Unit,
    pagerState: PagerState,
    totalPages: Int
) {
    val topBorderColor = WikipediaTheme.colors.inactiveColor
    BottomAppBar(
        modifier = Modifier
            .drawBehind {
                val screenWidth = size.width
                drawLine(
                    start = Offset(x = 0f, 0f),
                    end = Offset(x = screenWidth, 0f),
                    strokeWidth = 3f,
                    color = topBorderColor,
                )
            },
        containerColor = WikipediaTheme.colors.paperColor,
        content = {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val (donateRow, pagination, navigateRight) = createRefs()
                Row(
                    modifier = Modifier
                        .clickable(onClick = { /* TODO() */ })
                        .padding(start = 15.dp)
                        .wrapContentWidth()
                        .constrainAs(donateRow) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart_24),
                        tint = WikipediaTheme.colors.destructiveColor,
                        contentDescription = stringResource(R.string.year_in_review_heart_icon),
                    )

                    Text(text = stringResource(R.string.year_in_review_donate),
                        style = WikipediaTheme.typography.h3,
                        color = WikipediaTheme.colors.destructiveColor
                    )
                }

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .wrapContentWidth()
                        .constrainAs(pagination) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        },
                    horizontalArrangement = Arrangement.Center
                ) {
                    val totalPaginationIndicators = totalPages
                    repeat(totalPaginationIndicators) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            WikipediaTheme.colors.progressiveColor
                        } else {
                            WikipediaTheme.colors.inactiveColor
                        }
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .align(Alignment.CenterVertically)
                                .size(paginationSizeGradient(
                                    indicatorTotal = totalPaginationIndicators,
                                    iteration = iteration,
                                    pagerState = pagerState).dp)
                        )
                    }
                }
                if (pagerState.currentPage + 1 < totalPages) {
                    IconButton(
                        onClick = { onNavigationRightClick() },
                        modifier = Modifier
                            .padding(0.dp)
                            .constrainAs(navigateRight) {
                                end.linkTo(parent.end)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
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

@Composable
fun OnboardingBottomBar(
    onGetStartedClick: () -> Unit
) {
    BottomAppBar(
        containerColor = WikipediaTheme.colors.paperColor,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        contentColor = WikipediaTheme.colors.progressiveColor),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = { /* TODO() */ }
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
) {
    val scrollState = rememberScrollState()
    val gifAspectRatio = 3f / 2f

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        Glide.with(context)
                            .asGif()
                            .load(screenData.imageResource)
                            .centerCrop()
                            .into(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(gifAspectRatio)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(IntrinsicSize.Min)
                        .weight(1f),
                    text = String.format(stringResource(screenData.headLineText), 350),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(
                    onClick = { /* TODO() */ }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_info_24),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(R.string.year_in_review_information_icon)
                    )
                }
            }

            Text(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(IntrinsicSize.Min),
                text = String.format(stringResource(screenData.bodyText), 350),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun paginationSizeGradient(indicatorTotal: Int, iteration: Int, pagerState: PagerState): Int {
    var paginationSize = 8
    if (indicatorTotal > 3) {
        paginationSize = when {
            (iteration - pagerState.currentPage).absoluteValue <= 2 -> 8
            (iteration - pagerState.currentPage).absoluteValue == 3 -> 4
            else -> 2
        }
    }
    return paginationSize
}
