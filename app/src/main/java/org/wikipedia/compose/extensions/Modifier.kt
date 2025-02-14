package org.wikipedia.compose.extensions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

fun Modifier.pulse(
    fromScale: Float = 1f,
    toScale: Float = 2.5f,
    durationMillis: Int = 1000,
    pivot: Float = 0.5f,
): Modifier = composed {
    val scale = remember { mutableFloatStateOf(fromScale) }
    val targetScale = rememberUpdatedState(toScale)
    val infiniteTransition = rememberInfiniteTransition()

    scale.floatValue = infiniteTransition.animateFloat(
        initialValue = fromScale,
        targetValue = targetScale.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    scale(scale.floatValue, scale.floatValue).also {
        TransformOrigin(pivot, pivot)
    }
}


@Preview
@Composable
private fun PlusePreview() {
    BaseTheme {
        Scaffold(
            topBar = {
                WikiTopAppBar(
                    title = "Preview Content",
                    onNavigationClick = { }
                )
            },
            containerColor = WikipediaTheme.colors.paperColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Test",
                    color = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.pulse()
                )
            }
        }
    }
}
