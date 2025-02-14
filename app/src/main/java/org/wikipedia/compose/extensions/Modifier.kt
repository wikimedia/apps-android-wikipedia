package org.wikipedia.compose.extensions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    repeatCount: Int = 1
): Modifier = composed {
    val scale = remember { mutableFloatStateOf(fromScale) }
    val targetScale = rememberUpdatedState(toScale)

    LaunchedEffect(repeatCount) {
        repeat(repeatCount) {
            animate(
                initialValue = fromScale,
                targetValue = targetScale.value,
                animationSpec = tween(durationMillis, easing = LinearEasing)
            ) { value, _ ->
                scale.floatValue = value
            }

            animate(
                initialValue = targetScale.value,
                targetValue = fromScale,
                animationSpec = tween(durationMillis, easing = LinearEasing)
            ) { value, _ ->
                scale.floatValue = value
            }
        }
    }

    scale(scale.floatValue, scale.floatValue).also {
        TransformOrigin(pivot, pivot)
    }
}

@Preview
@Composable
private fun PreviewPulse() {
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
                    text = "Pulse",
                    color = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.pulse(repeatCount = 5)
                )
            }
        }
    }
}
