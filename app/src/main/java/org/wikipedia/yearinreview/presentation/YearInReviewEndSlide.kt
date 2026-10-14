package org.wikipedia.yearinreview.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme

private const val STORE_DISCOUNT_PERCENT = 15

private val EndSlideBackgroundColor = Color(0xFF1B223D)
private val EndCardPagerHorizontalPadding = 32.dp
private val EndCardPageSpacing = 20.dp

private val EndSlideSkyColorStops = arrayOf(
    0f to Color(0xFFF8F9FA),
    0.25f to Color(0xFF44D3F0),
    0.5f to Color(0xFF0099DA),
    0.85f to Color(0xFF0E2651),
    1f to EndSlideBackgroundColor
)

enum class YearInReviewEndCard(
    @StringRes val headlineRes: Int,
    @StringRes val actionRes: Int
) {
    STORE_DISCOUNT(R.string.yir_store_discount_headline, R.string.yir_store_discount_action),
    GAMES(R.string.yir_games_headline, R.string.yir_games_action),
    CUSTOMIZE_HOME(R.string.yir_customize_home_headline, R.string.yir_customize_home_action),
    QUIZ(R.string.yir_quiz_headline, R.string.yir_quiz_action)
}

@Composable
private fun YearInReviewEndCard.supportingText(): String = when (this) {
    YearInReviewEndCard.STORE_DISCOUNT -> pluralStringResource(R.plurals.yir_store_discount_supporting, STORE_DISCOUNT_PERCENT, STORE_DISCOUNT_PERCENT)
    YearInReviewEndCard.GAMES -> stringResource(R.string.yir_games_supporting)
    YearInReviewEndCard.CUSTOMIZE_HOME -> stringResource(R.string.yir_customize_home_supporting)
    YearInReviewEndCard.QUIZ -> stringResource(R.string.yir_quiz_supporting)
}

@Composable
fun YearInReviewEndSlide(
    modifier: Modifier = Modifier,
    isDonor: Boolean = false,
    isTestGroupUser: Boolean = true,
    onCardActionClick: (YearInReviewEndCard) -> Unit = {}
) {
    val cards = buildList {
        if (isDonor) {
            add(YearInReviewEndCard.STORE_DISCOUNT)
        }
        if (isTestGroupUser) {
            add(YearInReviewEndCard.GAMES)
            add(YearInReviewEndCard.CUSTOMIZE_HOME)
        }
        add(YearInReviewEndCard.QUIZ)
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(EndSlideBackgroundColor)
    ) {
        // The sky ends at the top of the headline, which moves down when a tall screen leaves extra room above the puzzle
        var skyBottom by remember { mutableFloatStateOf(0f) }
        // Fills the slide on tall screens, and scrolls when the content doesn't fit, e.g. with a large font scale
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .drawWithCache {
                    val skyBrush = Brush.verticalGradient(colorStops = EndSlideSkyColorStops, endY = skyBottom)
                    onDrawBehind {
                        drawRect(brush = skyBrush, size = Size(size.width, skyBottom))
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopAppBarHeight))
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.w_puzzle),
                contentDescription = null
            )
            PuzzleSpotlight(
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    .onPlaced { coordinates ->
                        skyBottom = coordinates.positionInParent().y
                    },
                text = stringResource(R.string.yir_closing_headline).uppercase(),
                color = ComposeColors.White,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                text = stringResource(R.string.yir_closing_supporting),
                color = ComposeColors.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 20.sp, fontSize = 15.sp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))
            YearInReviewEndCardPager(
                cards = cards,
                onCardActionClick = onCardActionClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// The glowing ellipse that the puzzle piece floats above
@Composable
private fun PuzzleSpotlight(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(width = 120.dp, height = 8.dp)
    ) {
        val radius = size.width / 2
        scale(scaleX = 1f, scaleY = size.height / size.width) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.White,
                        0.6f to Color.White.copy(alpha = 0.7f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )
        }
    }
}

@Composable
private fun YearInReviewEndCardPager(
    cards: List<YearInReviewEndCard>,
    onCardActionClick: (YearInReviewEndCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { cards.size }
    val cardsToMeasure: @Composable () -> Unit = {
        cards.forEach { card ->
            YearInReviewEndCardItem(
                modifier = Modifier.clearAndSetSemantics {},
                card = card,
                onActionClick = {}
            )
        }
    }
    val pager: @Composable () -> Unit = {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = EndCardPagerHorizontalPadding),
            pageSpacing = EndCardPageSpacing,
            key = { page -> cards[page] }
        ) { page ->
            YearInReviewEndCardItem(
                modifier = Modifier.fillMaxHeight(),
                card = cards[page],
                onActionClick = { onCardActionClick(cards[page]) }
            )
        }
    }
    // Every card gets the height of the tallest one, so that the buttons stay in line while swiping between them.
    // The cards measured for that are never placed, so they aren't drawn or reachable.
    SubcomposeLayout(
        modifier = modifier.fillMaxWidth()
    ) { constraints ->
        val cardWidth = (constraints.maxWidth - (EndCardPagerHorizontalPadding * 2).roundToPx()).coerceAtLeast(0)
        val cardHeight = subcompose(EndCardPagerSlot.MEASURED_CARDS, cardsToMeasure)
            .maxOfOrNull { it.measure(Constraints.fixedWidth(cardWidth)).height } ?: 0
        val pagerPlaceable = subcompose(EndCardPagerSlot.PAGER, pager).first()
            .measure(constraints.copy(minHeight = cardHeight, maxHeight = cardHeight))
        layout(pagerPlaceable.width, pagerPlaceable.height) {
            pagerPlaceable.place(0, 0)
        }
    }
}

private enum class EndCardPagerSlot {
    MEASURED_CARDS,
    PAGER
}

@Composable
private fun YearInReviewEndCardItem(
    card: YearInReviewEndCard,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = ComposeColors.White, shape = RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(card.headlineRes).uppercase(),
                color = ComposeColors.Black,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp, fontSize = 24.sp)
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = card.supportingText(),
                color = ComposeColors.Gray700,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 20.sp, fontSize = 15.sp)
            )
        }
        Button(
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ComposeColors.Blue600,
                contentColor = ComposeColors.White
            ),
            onClick = onActionClick
        ) {
            Text(
                text = stringResource(card.actionRes),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun YearInReviewEndSlidePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        YearInReviewEndSlide()
    }
}

@Preview(widthDp = 300)
@Composable
private fun YearInReviewEndCardItemPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        YearInReviewEndCardItem(
            card = YearInReviewEndCard.STORE_DISCOUNT,
            onActionClick = {}
        )
    }
}
