package org.wikipedia.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import kotlin.math.absoluteValue

@Composable
fun PageIndicator(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    animationDuration: Int = 500,
    indicatorSpacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(indicatorSpacing, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->
            val colorTransition by animateColorAsState(
                targetValue = if (index == pagerState.currentPage) {
                    WikipediaTheme.colors.progressiveColor
                } else {
                    WikipediaTheme.colors.inactiveColor
                },
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                )
            )
            val sizeTransition by animateDpAsState(
                targetValue = paginationSizeGradient(
                    totalIndicators = pagerState.pageCount,
                    iteration = index,
                    pagerState = pagerState
                ).dp,
                animationSpec = tween(durationMillis = animationDuration)
            )
            Box(
                modifier = Modifier
                    .background(colorTransition, CircleShape)
                    .size(sizeTransition)
            )
        }
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

@Preview(showBackground = true)
@Composable
private fun PageIndicatorPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        PageIndicator(
            pagerState = rememberPagerState(pageCount = { 3 })
        )
    }
}
