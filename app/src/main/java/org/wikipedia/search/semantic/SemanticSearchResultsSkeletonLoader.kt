package org.wikipedia.search.semantic

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.compose.theme.shimmerColors

@Composable
fun SemanticSearchResultsSkeletonLoader(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerColors = WikipediaTheme.colors.shimmerColors()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(count) {
                SemanticSearchResultCardSkeletonLoader(
                    shimmerColors = shimmerColors,
                    transition = transition
                )
            }
        }
    }
}

@Composable
fun SemanticSearchResultCardSkeletonLoader(
    modifier: Modifier = Modifier,
    shimmerColors: List<Color> = WikipediaTheme.colors.shimmerColors(),
    transition: InfiniteTransition = rememberInfiniteTransition(label = "shimmer")
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.backgroundColor,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(
                            shimmerColors = shimmerColors,
                            heightMultiplier = 0f,
                            transition = transition
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(
                            shimmerColors = shimmerColors,
                            heightMultiplier = 0f,
                            transition = transition
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(
                            shimmerColors = shimmerColors,
                            heightMultiplier = 0f,
                            transition = transition
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect(
                            shimmerColors = shimmerColors,
                            heightMultiplier = 0f,
                            transition = transition
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(
                            shimmerColors = shimmerColors,
                            heightMultiplier = 0f,
                            transition = transition
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = WikipediaTheme.colors.borderColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = shimmerColors,
                                heightMultiplier = 0f,
                                transition = transition
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = shimmerColors,
                                heightMultiplier = 0f,
                                transition = transition
                            )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = shimmerColors,
                                heightMultiplier = 0f,
                                transition = transition
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = shimmerColors,
                                heightMultiplier = 0f,
                                transition = transition
                            )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SemanticSearchResultsSkeletonLoaderPreview() {
    BaseTheme {
        SemanticSearchResultsSkeletonLoader()
    }
}
