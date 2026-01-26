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
    showSemanticSkeletonLoader: Boolean = false
) {
    val semanticBoxHeight = 500
    val semanticBoxIndividualContentHeight = 16
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        items(3) {
            ListItemLoader(
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
                                shimmerColors()
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(
                                shimmerColors()
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
                    items(3) {
                        Column(
                            modifier = Modifier
                                .size(width = 292.dp, height = semanticBoxHeight.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect()
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
                                            shimmerColors()
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
fun ListItemLoader(modifier: Modifier = Modifier) {
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
                        shimmerColors()
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(
                        shimmerColors()
                    )

            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(
                    shimmerColors()
                )
        )
    }
}

private fun shimmerColors(): List<Color> {
    val baseColor = Color(0xFFAEAEAE)
    val highlightColor = Color(0xFFEAEEF1)

    return listOf(
        baseColor.copy(alpha = 0.7f),
        highlightColor,
        baseColor.copy(alpha = 0.7f),
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
