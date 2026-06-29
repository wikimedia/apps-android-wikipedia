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
