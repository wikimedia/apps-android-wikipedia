package org.wikipedia.readinglist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun RecommendedReadingListDiscoverCardView(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes subtitleIcon: Int,
    subtitle: String,
    description: String,
    images: List<String>,
    isNewListGenerated: Boolean = false,
    isUserLoggedIn: Boolean = false,
) {
    WikiCard(
        elevation = 4.dp
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = WikipediaTheme.typography.h3,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    if (isNewListGenerated) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(WikipediaTheme.colors.destructiveColor)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp),
                        painter = painterResource(subtitleIcon),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = null
                    )
                    HtmlText(
                        modifier = Modifier
                            .padding(top = 2.dp),
                        text = subtitle,
                        style = TextStyle(
                            color = WikipediaTheme.colors.primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                Text(
                    text = description,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (images.isNotEmpty()) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .weight(0.9f)
                        .height(140.dp),
                    columns = GridCells.Fixed(2)
                ) {
                    items(4) { index ->
                        val imageUrl = images.getOrNull(index) ?: ""
                        if (imageUrl.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(WikipediaTheme.colors.borderColor)
                            )
                        } else {
                            AsyncImage(
                                model = ImageService.getRequest(
                                    context = LocalContext.current,
                                    url = imageUrl
                                ),
                                modifier = Modifier
                                    .size(70.dp),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                placeholder = ColorPainter(WikipediaTheme.colors.borderColor),
                                error = ColorPainter(WikipediaTheme.colors.borderColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecommendedReadingListDiscoverCardViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        RecommendedReadingListDiscoverCardView(
            modifier = Modifier
                .padding(16.dp),
            title = "Discover",
            subtitle = "Made for you",
            subtitleIcon = R.drawable.ic_wikipedia_w,
            description = "Your weekly reading list. Learn about new topics,  picked just for you. Updates at midnight.",
            images = listOf()
        )
    }
}
