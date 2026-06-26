package org.wikipedia.yir

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.wikipedia.compose.theme.BaseTheme

/**
 * Standard "content" card: a single insight, vertically centered, that fades in once the card's
 * one-shot background animation has finished ([YirCardPhase.REVEALED]). The full-bleed animation
 * lives in the background layer; this is just the text that reveals over it.
 */
@Composable
fun YirStandardContent(
    headline: String,
    supportingText: String,
    phase: YirCardPhase,
    modifier: Modifier = Modifier
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
        Text(
            text = headline,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = supportingText,
            color = Color.White.copy(alpha = 0.85f),
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
