package org.wikipedia.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun PageIndicator(
    count: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val colorTransition by animateColorAsState(
                targetValue = if (index == currentPage) {
                    WikipediaTheme.colors.progressiveColor
                } else {
                    WikipediaTheme.colors.inactiveColor
                },
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                )
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colorTransition, CircleShape)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PageIndicatorPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        PageIndicator(
            count = 5,
            currentPage = 2
        )
    }
}
