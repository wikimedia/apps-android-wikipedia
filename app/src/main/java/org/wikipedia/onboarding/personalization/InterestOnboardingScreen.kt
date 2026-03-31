package org.wikipedia.onboarding.personalization

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.recommended.ReadingListInterestCard
import org.wikipedia.readinglist.recommended.ReadingListInterestSearchCard

// TODO: add actual UI
@Composable
fun InterestOnboardingScreen(
    modifier: Modifier = Modifier,
    topicsState: TopicsState,
    articlesState: ArticlesState,
    onCategorySelected: (OnboardingTopic) -> Unit,
    onItemClick: (PageTitle) -> Unit = {},
    onSearchClick: () -> Unit
) {
    val listState = rememberLazyStaggeredGridState()
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.recommended_reading_list_interest_pick_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )

        ReadingListInterestSearchCard(
            onSearchClick = onSearchClick
        )

        when (topicsState) {
            is TopicsState.Error -> {}
            TopicsState.Loading -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(5) { index ->
                        val width = remember { listOf(80, 100, 70, 90, 85)[index].dp }
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(32.dp)
                                .clip(RoundedCornerShape(size = 8.dp))
                                .shimmerEffect(transition = transition)
                        )
                    }
                }
            }
            is TopicsState.Success -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topicsState.topics) { item ->
                        FilterChip(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(item.title) },
                            selected = item.isSelected,
                            onClick = { onCategorySelected(item) },
                            leadingIcon = {
                                AnimatedContent(
                                    targetState = item.isSelected
                                ) { isSelected ->
                                    Icon(
                                        modifier = Modifier
                                            .size(16.dp),
                                        painter = if (isSelected) {
                                            R.drawable.ic_check_black_24dp
                                        } else {
                                            R.drawable.ic_add_gray_white_24dp
                                        }.let { painterResource(id = it) },
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = WikipediaTheme.colors.backgroundColor,
                                labelColor = WikipediaTheme.colors.primaryColor,
                                iconColor = WikipediaTheme.colors.primaryColor,
                                selectedLeadingIconColor = WikipediaTheme.colors.progressiveColor,
                                selectedContainerColor = WikipediaTheme.colors.additionColor,
                                selectedLabelColor = WikipediaTheme.colors.progressiveColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = item.isSelected,
                                borderColor = WikipediaTheme.colors.borderColor,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        when (articlesState) {
            is ArticlesState.Error -> {}
            ArticlesState.Loading -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(140.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    content = {
                        items(10) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(size = 16.dp))
                                    .shimmerEffect(transition = transition)
                            )
                        }
                    }
                )
            }
            is ArticlesState.Success -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(140.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                    state = listState,
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    content = {
                        items(articlesState.articles) { item ->
                            ReadingListInterestCard(
                                modifier = Modifier.animateItem(),
                                item = item,
                                isSelected = articlesState.selectedArticles.contains(item),
                                onItemClick = { onItemClick(item) }
                            )
                        }
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Spacer(
                                modifier = Modifier.height(64.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
