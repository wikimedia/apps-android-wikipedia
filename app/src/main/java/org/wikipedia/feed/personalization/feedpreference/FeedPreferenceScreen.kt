package org.wikipedia.feed.personalization.feedpreference

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun FeedPreferenceScreen(
    modifier: Modifier = Modifier,
    selectedType: FeedPreferenceType,
    communityContentState: FeedContentState,
    personalizedContentState: FeedContentState,
    onTypeSelected: (FeedPreferenceType) -> Unit,
    onRetryClick: (FeedPreferenceType) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.explore_feed_preference_selection_screen_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = WikipediaTheme.colors.primaryColor
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                FeedPreferenceSection(
                    state = communityContentState,
                    isSelected = selectedType == FeedPreferenceType.COMMUNITY,
                    feedPreferenceType = FeedPreferenceType.COMMUNITY,
                    onSelected = onTypeSelected,
                    onRetryClick = onRetryClick
                )
            }
            item {
                FeedPreferenceSection(
                    state = personalizedContentState,
                    isSelected = selectedType == FeedPreferenceType.PERSONALIZED,
                    feedPreferenceType = FeedPreferenceType.PERSONALIZED,
                    onSelected = onTypeSelected,
                    onRetryClick = onRetryClick
                )
            }
        }
    }
}

@Composable
fun FeedPreferenceSection(
    state: FeedContentState,
    isSelected: Boolean,
    feedPreferenceType: FeedPreferenceType,
    onRetryClick: (FeedPreferenceType) -> Unit,
    onSelected: (FeedPreferenceType) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "feedPreferenceShimmerTransition")
    val isPersonalizedContentDisabled = state is FeedContentState.Empty
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clickable(onClick = { if (!isPersonalizedContentDisabled) onSelected(feedPreferenceType) }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelected(feedPreferenceType) },
                enabled = !isPersonalizedContentDisabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = WikipediaTheme.colors.primaryColor,
                    unselectedColor = WikipediaTheme.colors.primaryColor,
                    disabledUnselectedColor = WikipediaTheme.colors.inactiveColor,
                    disabledSelectedColor = WikipediaTheme.colors.inactiveColor
                )
            )
            Text(
                text = stringResource(feedPreferenceType.titleRes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isPersonalizedContentDisabled) FontWeight.Normal else FontWeight.Medium
                ),
                color = if (isPersonalizedContentDisabled) WikipediaTheme.colors.inactiveColor else
                    WikipediaTheme.colors.primaryColor
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                is FeedContentState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            WikiErrorView(
                                caught = state.message,
                                errorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = { onRetryClick(feedPreferenceType) }
                                )
                            )
                        }
                    }
                }

                FeedContentState.Loading -> {
                    items(3) {
                        Box(
                            modifier = Modifier
                                .width(185.dp)
                                .height(230.dp)
                                .clip(RoundedCornerShape(size = 12.dp))
                                .shimmerEffect(transition = transition)
                        )
                    }
                }

                FeedContentState.Empty -> {
                    item {
                        Text(
                            modifier = Modifier.fillParentMaxWidth(),
                            text = stringResource(R.string.explore_feed_personalized_preference_empty_state_text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                }

                is FeedContentState.Success -> {
                    items(state.content) { content ->
                        FeedPreferenceArticleCard(
                            content = content,
                            feedPreferenceType = feedPreferenceType
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedPreferenceArticleCard(
    modifier: Modifier = Modifier,
    feedPreferenceType: FeedPreferenceType,
    content: FeedPreferenceContent
) {
    WikiCard(
        modifier = modifier
            .width(185.dp)
            .height(230.dp),
        elevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .height(108.dp)
            ) {
                val request = ImageService.getRequest(
                    LocalContext.current,
                    url = content.imageUrl,
                    detectFace = true
                )
                AsyncImage(
                    model = request,
                    placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                )
                if (!content.tag.isNullOrEmpty()) {
                    ArticleCardTag(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(
                                when (feedPreferenceType) {
                                    FeedPreferenceType.COMMUNITY -> WikipediaTheme.colors.progressiveColor
                                    FeedPreferenceType.PERSONALIZED -> WikipediaTheme.colors.successColor
                                }, shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        text = content.tag
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                if (!content.title.isNullOrEmpty()) {
                    HtmlText(
                        text = content.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        color = WikipediaTheme.colors.primaryColor,
                    )
                }

                if (!content.description.isNullOrEmpty()) {
                    HtmlText(
                        text = content.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.secondaryColor,
                        maxLines = if (!content.title.isNullOrEmpty()) 3 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ArticleCardTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium
        ),
        color = WikipediaTheme.colors.backgroundColor
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedPreferenceScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedPreferenceScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            selectedType = FeedPreferenceType.COMMUNITY,
            communityContentState = FeedContentState.Success(
                content = listOf(
                    FeedPreferenceContent(
                        title = "Winter Paralympics",
                        description = "2026 Winter Olympics Multi-sport event in Italy",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "In the news"
                    ),
                    FeedPreferenceContent(
                        title = "Rosa Parks",
                        description = "American civil rights activist (1913–2005)",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Featured article"
                    ),
                    FeedPreferenceContent(
                        title = "Rosa Parks",
                        description = "American civil rights activist (1913–2005)",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Featured article"
                    )
                )
            ),
            personalizedContentState = FeedContentState.Success(
                content = listOf(
                    FeedPreferenceContent(
                        title = "Personalized Content",
                        description = "See content that’s personalized for you based on your reading history and interests.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Personalized"
                    )
                )
            ),
            onTypeSelected = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, fontScale = 1.5f, device = Devices.PIXEL_9)
@Composable
private fun FeedPreferenceScreenScaledTextPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        FeedPreferenceScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            selectedType = FeedPreferenceType.COMMUNITY,
            communityContentState = FeedContentState.Success(
                content = listOf(
                    FeedPreferenceContent(
                        title = "Winter Paralympics",
                        description = "2026 Winter Olympics Multi-sport event in Italy",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "In the news"
                    ),
                    FeedPreferenceContent(
                        title = "Rosa Parks",
                        description = "American civil rights activist (1913–2005)",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Featured article"
                    ),
                    FeedPreferenceContent(
                        title = "Rosa Parks",
                        description = "American civil rights activist (1913–2005)",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Featured article"
                    )
                )
            ),
            personalizedContentState = FeedContentState.Success(
                content = listOf(
                    FeedPreferenceContent(
                        title = "Post's lattice",
                        description = "Lattice in universal algebra",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Logic"
                    ),
                    FeedPreferenceContent(
                        title = "Ranunculaceae",
                        description = "Family of eudicot flowering plants",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/120px-Wikipedia-logo-v2.svg.png",
                        tag = "Nature"
                    )
                )
            ),
            onTypeSelected = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_9)
@Composable
private fun FeedPreferenceScreenLoadingPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedPreferenceScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            selectedType = FeedPreferenceType.COMMUNITY,
            communityContentState = FeedContentState.Loading,
            personalizedContentState = FeedContentState.Loading,
            onTypeSelected = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_9)
@Composable
private fun FeedPreferenceScreenErrorPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedPreferenceScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            selectedType = FeedPreferenceType.COMMUNITY,
            communityContentState = FeedContentState.Error(Throwable("Failed to load community content")),
            personalizedContentState = FeedContentState.Error(Throwable("Failed to load personalized content")),
            onTypeSelected = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_9)
@Composable
private fun FeedPreferenceScreenEmptyPersonalizedContentPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedPreferenceScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            selectedType = FeedPreferenceType.COMMUNITY,
            communityContentState = FeedContentState.Loading,
            personalizedContentState = FeedContentState.Empty,
            onTypeSelected = {},
            onRetryClick = {}
        )
    }
}
