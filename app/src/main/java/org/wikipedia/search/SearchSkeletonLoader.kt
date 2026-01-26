package org.wikipedia.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices.PIXEL_9
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun SearchSkeletonLoader(
    modifier: Modifier = Modifier,
    showSemanticSkeletonLoader: Boolean = false,
    itemsCount: Int = 3,
    semanticItemsCount: Int = 3,
) {
    val semanticBoxHeight = 500
    val semanticBoxIndividualContentHeight = 16
    val semanticShimmerColors = semanticShimmerColors()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        items(itemsCount) {
            ListItemLoader(
                shimmerColors = semanticShimmerColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }

        if (showSemanticSkeletonLoader) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.5f)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = semanticShimmerColors,
                                heightMultiplier = 0f
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors = semanticShimmerColors,
                                heightMultiplier = 0f
                            )

                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(semanticItemsCount) {
                        Column(
                            modifier = Modifier
                                .size(width = 292.dp, height = semanticBoxHeight.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color = WikipediaTheme.colors.backgroundColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(semanticBoxHeight / semanticBoxIndividualContentHeight) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(semanticBoxIndividualContentHeight.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect(
                                            shimmerColors = semanticShimmerColors,
                                            heightMultiplier = 0f
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListItemLoader(
    modifier: Modifier = Modifier,
    shimmerColors: List<Color>
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.5f)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(
                        shimmerColors = shimmerColors,
                        heightMultiplier = 0f
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(
                        shimmerColors = shimmerColors,
                        heightMultiplier = 0f
                    )

            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(
                    shimmerColors = shimmerColors,
                    heightMultiplier = 0f
                )
        )
    }
}

@Composable
fun semanticShimmerColors(): List<Color> {
    val colors = WikipediaTheme.colors
    return listOf(
        colors.inactiveColor.copy(alpha = 0.7f),
        colors.borderColor.copy(alpha = 0.5f),
        colors.inactiveColor.copy(alpha = 0.7f)
    )
}

@Preview(showBackground = true, device = PIXEL_9)
@Composable
private fun SearchSkeletonLoaderPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SearchSkeletonLoader(
            modifier = Modifier
                .fillMaxSize()
        )
    }
}
