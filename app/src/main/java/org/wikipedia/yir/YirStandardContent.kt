package org.wikipedia.yir

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import org.wikipedia.compose.theme.BaseTheme

/**
 * Standard "content" card: a single insight, vertically centered, that fades in once the card's
 * one-shot background animation has finished ([YirCardPhase.REVEALED]).
 *
 * Besides the full-bleed background, a standard card may also show a Lottie *in the content* via
 * [foregroundAnimationAsset]: a square hero animation that sits above the text and loops
 * continuously (like the looping gifs in the current Year in Review). This proves that animations
 * can live in the content slot, not only as the full-bleed background. When the card has a hero
 * loop, give it a plain solid/gradient background so the two don't fight for attention.
 *
 * @param foregroundAnimationAsset Lottie asset path under `assets/` (e.g. "lottie/reading_book.lottie"),
 *   or null for a text-only card. It loops while shown and is independent of [phase].
 * @param textColor the color of the insight text. Defaults to white for cards over a dark
 *   background; pass a dark color for cards whose background is light where the text sits.
 */
@Composable
fun YirStandardContent(
    headline: String,
    supportingText: String,
    phase: YirCardPhase,
    modifier: Modifier = Modifier,
    foregroundAnimationAsset: String? = null,
    textColor: Color = Color.White
) {
    val revealAlpha by animateFloatAsState(
        targetValue = if (phase == YirCardPhase.REVEALED) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "yirTextReveal"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(revealAlpha)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        foregroundAnimationAsset?.let { asset ->
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset(asset))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(220.dp)
                    .padding(bottom = 24.dp)
            )
        }
        Text(
            text = headline,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = supportingText,
            color = textColor.copy(alpha = 0.85f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Preview
@Composable
private fun YirStandardContentPreview() {
    BaseTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF12B36B))) {
            YirStandardContent(
                headline = "924 minutes reading",
                supportingText = "Not all screen time is created equal. Your longest session: March 7, 43 minutes.",
                phase = YirCardPhase.REVEALED
            )
        }
    }
}
