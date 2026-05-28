package org.wikipedia.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    indicatorSpacing: Dp = 12.dp,
    maxIndicatorSize: Dp = 8.dp,
    activeColor: Color = WikipediaTheme.colors.progressiveColor,
    inactiveColor: Color = WikipediaTheme.colors.inactiveColor
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(indicatorSpacing, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->
            val colorState = animateColorAsState(
                targetValue = if (index == pagerState.currentPage) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
            )
            val sizeState = animateDpAsState(
                targetValue = paginationSizeGradient(
                    totalIndicators = pagerState.pageCount,
                    iteration = index,
                    pagerState = pagerState,
                    max = maxIndicatorSize
                ),
                animationSpec = tween(durationMillis = animationDuration)
            )
            Canvas(modifier = Modifier.size(maxIndicatorSize)) {
                drawCircle(
                    color = colorState.value,
                    radius = sizeState.value.toPx() / 2f
                )
            }
        }
    }
}

private fun paginationSizeGradient(totalIndicators: Int, iteration: Int, pagerState: PagerState, max: Dp): Dp {
    return when {
        totalIndicators <= 5 -> max
        (iteration - pagerState.currentPage).absoluteValue <= 4 -> max
        (iteration - pagerState.currentPage).absoluteValue == 5 -> max / 2
        else -> max / 4
    }
}

@Preview(showBackground = true)
@Composable
private fun PageIndicatorPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        PageIndicator(
            pagerState = rememberPagerState(pageCount = { 8 })
        )
    }
}
