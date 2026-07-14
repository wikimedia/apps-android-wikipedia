package org.wikipedia.feed.personalization.interest

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.ArticleCard
import org.wikipedia.compose.components.SearchBarCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.L10nUtil

@Composable
fun InterestOnboardingScreen(
    modifier: Modifier = Modifier,
    articlesState: ArticlesState,
    topicsList: List<OnboardingTopic>,
    totalSelectedCount: Int,
    languageCode: String,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onTopicSelected: (OnboardingTopic) -> Unit,
    onItemClick: (PageTitle) -> Unit = {},
    onSearchClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    retryLoading: () -> Unit,
    onBackButtonClick: (() -> Unit)? = null
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    Box(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackButtonClick != null) {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = onBackButtonClick),
                        painter = painterResource(id = R.drawable.ic_arrow_back_black_24dp),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.primaryColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = stringResource(id = R.string.recommended_reading_list_interest_pick_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val layoutDirection = if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(140.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    content = {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            SearchBarCard(
                                onSearchClick = onSearchClick,
                                text = stringResource(R.string.recommended_reading_list_interest_pick_search_hint)
                            )
                        }
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier.layout { measurable, constraints ->
                                    val extra = 16.dp.roundToPx() * 2
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = constraints.minWidth + extra,
                                            maxWidth = constraints.maxWidth + extra
                                        )
                                    )
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                }
                            ) {
                                TopicFilterChipRow(
                                    topics = topicsList,
                                    languageCode = languageCode,
                                    onTopicSelected = { onTopicSelected(it) }
                                )
                            }
                        }

                        when (articlesState) {
                            is ArticlesState.Error -> {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Box(
                                        modifier = modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        WikiErrorView(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            caught = articlesState.message,
                                            errorClickEvents = WikiErrorClickEvents(
                                                retryClickListener = retryLoading
                                            ),
                                            retryForGenericError = true
                                        )
                                    }
                                }
                            }
                            ArticlesState.Loading -> {
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

                            is ArticlesState.Success -> {
                                items(articlesState.articles) { item ->
                                    ArticleCard(
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
                        }
                    }
                )
            }
        }

        SelectionBottomBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(WikipediaTheme.colors.paperColor)
                .clickable(enabled = false, onClick = {}),
            selectedItemsCount = totalSelectedCount,
            onDeselectAllClick = onDeselectAllClick
        )
    }
}

@Composable
fun TopicFilterChipRow(
    topics: List<OnboardingTopic>,
    modifier: Modifier = Modifier,
    languageCode: String,
    onTopicSelected: (OnboardingTopic) -> Unit
) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(items = topics, key = { it.topic.topicId }) { item ->
            FilterChip(
                label = { Text(
                    text = context.getString(languageCode, item.topic.msgKey)
                ) },
                selected = item.isSelected,
                onClick = { onTopicSelected(item) },
                leadingIcon = {
                    AnimatedContent(
                        targetState = item.isSelected,
                        label = "topicSelectionIcon"
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

@Composable
fun SelectionBottomBar(
    selectedItemsCount: Int,
    modifier: Modifier = Modifier,
    onDeselectAllClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = WikipediaTheme.colors.borderColor
        )
        AnimatedContent(
            targetState = selectedItemsCount > 0
        ) { isSelected ->
            if (isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.multi_select_items_selected,
                            selectedItemsCount
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    )

                    Button(
                        onClick = onDeselectAllClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WikipediaTheme.colors.backgroundColor,
                            contentColor = WikipediaTheme.colors.secondaryColor
                        )
                    ) {
                        Text(
                            modifier = Modifier
                                .clip(RoundedCornerShape(size = 8.dp)),
                            text = stringResource(R.string.explore_feed_deselect_all_button_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.recommended_reading_list_interest_select_minimum),
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InterestOnboardingScreenPreview() {
    val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
    val titles = listOf(
        PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
        PageTitle(text = "Industrial design", wiki = site, thumbUrl = "foo.jpg", description = "Process of design applied to physical products", displayText = null),
        PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
        PageTitle(text = "Sample title without description", wiki = site, thumbUrl = "foo.jpg", description = "", displayText = null),
        PageTitle(text = "Sample title without thumbnail", wiki = site, thumbUrl = "", description = "Sample description", displayText = null),
        PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null),
        PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
    )
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InterestOnboardingScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor)
                .padding(top = 40.dp),
            totalSelectedCount = 0,
            topicsList = ArticleTopics.all.map { OnboardingTopic(it) }.map {
                    it.copy(isSelected = it.topic.topicId == "art")
                },
            articlesState = ArticlesState.Success(
                articles = titles,
                selectedArticles = setOf()
            ),
            languageCode = "",
            onTopicSelected = {},
            onSearchClick = {},
            onDeselectAllClick = {},
            retryLoading = {},
            onBackButtonClick = {}
        )
    }
}
