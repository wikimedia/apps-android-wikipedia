package org.wikipedia.feed.discover

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.FeedFeatureTeaserModule
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.DiscoverCard
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.SeeAllRecommendationCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.theme.Theme

private val discoverPromptImageUrls = listOf(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Red_eyed_tree_frog_edit2.jpg/960px-Red_eyed_tree_frog_edit2.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg/1920px-Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/1/17/Monet_w861.jpg?_=20230407213842",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Mercury_in_color_-_Prockter07_centered.jpg/1280px-Mercury_in_color_-_Prockter07_centered.jpg"
)
private const val IMAGE_EARTH = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0d/Africa_and_Europe_from_a_Million_Miles_Away.png/960px-Africa_and_Europe_from_a_Million_Miles_Away.png"
private const val IMAGE_CATHERINE_CHURCH = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/Edele_Tronier_Ruins_of_St_Catherine%27s_Church_in_Visby%2C_Gotland.jpg/960px-Edele_Tronier_Ruins_of_St_Catherine%27s_Church_in_Visby%2C_Gotland.jpg?_=20220126092935"

@Composable
fun DiscoverEnablePromptModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    onEnableDiscoverClick: () -> Unit = {}
) {
    val context = LocalContext.current
    FeedFeatureTeaserModule(
        modifier = modifier,
        title = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_title),
        description = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_description),
        buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_button),
        buttonIcon = painterResource(R.drawable.ic_lightbulb_24dp),
        imageUrls = discoverPromptImageUrls,
        onButtonClick = onEnableDiscoverClick
    )
}

@Composable
fun DiscoverArticlesModule(
    modifier: Modifier = Modifier,
    topInset: Int,
    wikiSite: WikiSite,
    module: ForYouModule.Discover,
    @StringRes updateFrequency: Int,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeClick: (card: Card) -> Unit = {},
    onSeeAllRecommendationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { page ->
        when (val card = module.cards[page]) {
            is SeeAllRecommendationCard -> {
                SeeAllRecommendationsSlide(
                    topInset = topInset,
                    wikiSite = wikiSite,
                    onSeeAllRecommendationsClick = onSeeAllRecommendationsClick
                )
            }
            is DiscoverCard -> {
                val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_RECOMMENDED_READING_LIST)
                val updateFrequencyText = context.getString(wikiSite.languageCode, updateFrequency)
                ForYouCardContent(
                    wikiSite = wikiSite,
                    title = card.title,
                    module = module,
                    card = card,
                    footerIcon = painterResource(R.drawable.ic_lightbulb_24dp),
                    footerText = context.getString(wikiSite.languageCode, R.string.home_feed_discover_card_footer, updateFrequencyText),
                    onPageClick = { onPageClick(card, historyEntry) },
                    onShareClick = { onPageShareClick(card, historyEntry) },
                    onSaveClick = { onPageBookmarkClick(card, historyEntry) },
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = onHideModuleClick,
                    customizeMenuText = R.string.explore_feed_customize_discover,
                    onCustomizeClick = { onCustomizeClick(card) }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun SeeAllRecommendationsSlide(
    modifier: Modifier = Modifier,
    topInset: Int,
    wikiSite: WikiSite,
    onSeeAllRecommendationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColors.Green800)
            .padding(horizontal = 16.dp)
            .padding(top = (topInset * 2 + 64).dp),
        verticalArrangement = Arrangement.Center
    ) {
        SkeletonArticlePreviewRow(imageUrl = IMAGE_EARTH)
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonArticlePreviewRow(imageUrl = IMAGE_CATHERINE_CHURCH)

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, R.string.home_feed_discover_see_all_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, R.string.home_feed_discover_see_all_description),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.25.sp),
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onSeeAllRecommendationsClick,
            border = BorderStroke(1.dp, Color.White)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lightbulb_24dp),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = context.getString(wikiSite.languageCode, R.string.home_feed_discover_see_all_button),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SkeletonArticlePreviewRow(
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    val placeholderColor = Color.White.copy(alpha = 0.12f)
    val skeletonLineBrush = Brush.linearGradient(
        0.0f to ComposeColors.Green600,
        0.6f to ComposeColors.Green600,
        1.0f to ComposeColors.Green600.copy(alpha = 0f)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            placeholder = BrushPainter(SolidColor(placeholderColor)),
            error = BrushPainter(SolidColor(placeholderColor)),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(skeletonLineBrush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(skeletonLineBrush)
            )
        }
    }
}

@Preview
@Composable
private fun SeeAllRecommendationSlidePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        SeeAllRecommendationsSlide(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800)
                .padding(horizontal = 16.dp),
            topInset = 0,
            wikiSite = WikiSite.preview()
        )
    }
}

@Preview
@Composable
private fun DiscoverEnablePromptModulePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        DiscoverEnablePromptModule(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800)
                .padding(horizontal = 16.dp),
            wikiSite = WikiSite.preview()
        )
    }
}
