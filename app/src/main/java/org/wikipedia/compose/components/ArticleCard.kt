package org.wikipedia.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.PageTitle
import org.wikipedia.views.imageservice.ImageService

@Composable
fun ArticleCard(
    modifier: Modifier,
    item: PageTitle,
    isSelected: Boolean = false,
    onItemClick: (PageTitle) -> Unit = {},
) {
    WikiCard(
        modifier = modifier
            .fillMaxWidth(),
        elevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WikipediaTheme.colors.additionColor else WikipediaTheme.colors.paperColor
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onItemClick(item)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (!item.thumbUrl.isNullOrEmpty()) {
                val request = ImageService.getRequest(LocalContext.current, url = item.thumbUrl, detectFace = true)
                AsyncImage(
                    model = request,
                    placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                HtmlText(
                    text = item.displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    if (!item.description.isNullOrEmpty()) {
                        HtmlText(
                            text = item.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WikipediaTheme.colors.secondaryColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            modifier = Modifier.size(24.dp).align(Alignment.Bottom),
                            painter = painterResource(R.drawable.check_circle_24px),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                    } else {
                        Spacer(modifier = Modifier.width(32.dp).height(24.dp))
                    }
                }
            }
        }
    }
}
