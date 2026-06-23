package org.wikipedia.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

private const val SHORT_TILE_WEIGHT = 100f
private const val TALL_TILE_WEIGHT = 171f
@Composable
fun FeedFeatureTeaserModule(
    title: String,
    description: String,
    buttonText: String,
    buttonIcon: Painter,
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    onButtonClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        PlaceholderImageGrid(
            modifier = Modifier.weight(1f),
            imageUrls = imageUrls
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = ComposeColors.Gray100
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.25.sp),
            color = ComposeColors.Gray100
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onButtonClick,
            border = BorderStroke(1.dp, ComposeColors.Gray100)
        ) {
            Icon(
                painter = buttonIcon,
                contentDescription = null,
                tint = ComposeColors.Gray100,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                color = ComposeColors.Gray100
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlaceholderImageGrid(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlaceholderTile(modifier = Modifier.weight(SHORT_TILE_WEIGHT), imageUrl = imageUrls.getOrNull(0))
            PlaceholderTile(modifier = Modifier.weight(TALL_TILE_WEIGHT), imageUrl = imageUrls.getOrNull(1))
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlaceholderTile(modifier = Modifier.weight(TALL_TILE_WEIGHT), imageUrl = imageUrls.getOrNull(2))
            PlaceholderTile(modifier = Modifier.weight(SHORT_TILE_WEIGHT), imageUrl = imageUrls.getOrNull(3))
        }
    }
}

@Composable
private fun PlaceholderTile(
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun FeedFeatureTeaserModulePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        FeedFeatureTeaserModule(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800)
                .padding(horizontal = 16.dp),
            title = "From Discover",
            description = "Learn about new topics, picked just for you. Choose between daily, weekly or monthly updates.",
            buttonText = "Enable Discover reading list",
            buttonIcon = painterResource(R.drawable.ic_light_bulb),
            imageUrls = emptyList()
        )
    }
}
