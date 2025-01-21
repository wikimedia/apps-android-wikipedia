package org.wikipedia.compose.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AppAnimation(
    fromScale: Float = 1f,
    toScale: Float = 2.5f,
    durationMillis: Int = 1000,
    pivot: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = toScale,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
        scale.animateTo(
            targetValue = fromScale,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                transformOrigin = TransformOrigin(pivot, pivot)
            )
    ) {
        content()
    }
}
