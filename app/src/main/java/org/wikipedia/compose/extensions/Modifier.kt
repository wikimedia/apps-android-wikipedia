package org.wikipedia.compose.extensions

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.compose.theme.shimmerColors
import org.wikipedia.theme.Theme

fun Modifier.pulse(
    fromScale: Float = 1f,
    toScale: Float = 2.5f,
    durationMillis: Int = 1000,
    pivot: Float = 0.5f,
    repeatCount: Int = 1,
    easing: Easing = FastOutSlowInEasing
): Modifier = composed {
    val scale = remember { mutableFloatStateOf(fromScale) }
    val targetScale = rememberUpdatedState(toScale)

    LaunchedEffect(Unit) {
        repeat(repeatCount) {
            animate(
                initialValue = fromScale,
                targetValue = targetScale.value,
                animationSpec = tween(durationMillis, easing = easing)
            ) { value, _ ->
                scale.floatValue = value
            }

            animate(
                initialValue = targetScale.value,
                targetValue = fromScale,
                animationSpec = tween(durationMillis, easing = easing)
            ) { value, _ ->
                scale.floatValue = value
            }
        }
    }

    scale(scale.floatValue, scale.floatValue).also {
        TransformOrigin(pivot, pivot)
    }
}

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = clickable(
    enabled = enabled,
    indication = null,
    interactionSource = null,
    onClickLabel = onClickLabel,
    onClick = onClick
)

@Preview
@Composable
private fun PreviewPulse() {
    BaseTheme(currentTheme = Theme.LIGHT) {
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
                    modifier = Modifier.pulse(
                        repeatCount = 5,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(
    shimmerColors: List<Color>? = null,
    durationMs: Int = 1200,
    easing: Easing = LinearEasing
): Modifier = composed {
    val colors = shimmerColors ?: WikipediaTheme.colors.shimmerColors()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition()

    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = easing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(startOffsetX, 0f),
        end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
    )

    this
        .onSizeChanged { size = it }
        .background(brush)
}
