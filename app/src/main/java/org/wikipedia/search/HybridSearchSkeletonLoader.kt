package org.wikipedia.search

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import org.wikipedia.compose.components.ListItemSkeletonLoader
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun HybridSearchSkeletonLoader(
    abTestGroupName: String,
    modifier: Modifier = Modifier
) {
    val semanticShimmerColors = semanticShimmerColors()
    val transition = rememberInfiniteTransition()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        when (abTestGroupName) {
            HybridSearchAbCTest.GROUP_CONTROL -> {
                items(15) {
                    ListItemSkeletonLoader(
                        shimmerColors = semanticShimmerColors,
                        transition = transition,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
            HybridSearchAbCTest.GROUP_LEXICAL_SEMANTIC -> {
                items(3) {
                    ListItemSkeletonLoader(
                        shimmerColors = semanticShimmerColors,
                        transition = transition,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
                item {
                    HorizontalHybridSearchListIemLoader(
                        semanticItemsCount = 3,
                        shimmerColors = semanticShimmerColors,
                        transition = transition,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                }
            }
            HybridSearchAbCTest.GROUP_SEMANTIC_LEXICAL -> {
                item {
                    HorizontalHybridSearchListIemLoader(
                        semanticItemsCount = 3,
                        shimmerColors = semanticShimmerColors,
                        transition = transition,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(3) {
                    ListItemSkeletonLoader(
                        shimmerColors = semanticShimmerColors,
                        transition = transition,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalHybridSearchListIemLoader(
    modifier: Modifier = Modifier,
    semanticItemsCount: Int = 3,
    shimmerColors: List<Color>,
    transition: InfiniteTransition
) {
    val semanticBoxHeight = 500
    val semanticBoxIndividualContentHeight = 16

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(
                    shimmerColors = shimmerColors,
                    heightMultiplier = 0f,
                    transition = transition
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(
                    shimmerColors = shimmerColors,
                    heightMultiplier = 0f,
                    transition = transition
                )

        )

        LazyRow(
            modifier = Modifier
                .padding(top = 8.dp),
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
private fun HybridSearchSkeletonLoaderVariantAPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchSkeletonLoader(
            abTestGroupName = "a",
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Preview(showBackground = true, device = PIXEL_9)
@Composable
private fun HybridSearchSkeletonLoaderVariantBPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchSkeletonLoader(
            abTestGroupName = "b",
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Preview(showBackground = true, device = PIXEL_9)
@Composable
private fun HybridSearchSkeletonLoaderVariantCPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchSkeletonLoader(
            abTestGroupName = "c",
            modifier = Modifier
                .fillMaxSize()
        )
    }
}
