package org.wikipedia.search.interleave

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.compose.theme.shimmerColors
import org.wikipedia.theme.Theme

@Composable
fun SearchResultCardSkeleton(
    modifier: Modifier = Modifier,
    shimmerColors: List<Color> = WikipediaTheme.colors.shimmerColors(),
    transition: InfiniteTransition = rememberInfiniteTransition(label = "searchResultCardShimmer")
) {
    @Composable
    fun Bar(widthFraction: Float, height: Dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(shimmerColors = shimmerColors, heightMultiplier = 0f, transition = transition)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WikipediaTheme.colors.backgroundColor)
            .clearAndSetSemantics {}
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Bar(widthFraction = 0.6f, height = 16.dp)
                Bar(widthFraction = 0.85f, height = 12.dp)
                Bar(widthFraction = 0.4f, height = 12.dp)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(shimmerColors = shimmerColors, heightMultiplier = 0f, transition = transition)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = WikipediaTheme.colors.borderColor,
            thickness = 1.dp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1f, 1f, 1f, 0.6f).forEach { Bar(widthFraction = it, height = 12.dp) }
            Bar(widthFraction = 0.3f, height = 12.dp)
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            color = WikipediaTheme.colors.borderColor,
            thickness = 1.dp
        )

        Bar(widthFraction = 0.55f, height = 12.dp)
    }
}

@Composable
private fun SearchResultCardSkeletonPreviewList(theme: Theme) {
    BaseTheme(currentTheme = theme) {
        val transition = rememberInfiniteTransition(label = "previewShimmer")
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { SearchResultCardSkeleton(transition = transition) }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchResultCardSkeletonPreview() {
    SearchResultCardSkeletonPreviewList(Theme.LIGHT)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchResultCardSkeletonDarkPreview() {
    SearchResultCardSkeletonPreviewList(Theme.DARK)
}
