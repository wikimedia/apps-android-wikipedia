package org.wikipedia.compose.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * A wrapper around AsyncImage that fades in the image when it successfully loads,
 * but only if the load takes longer than the threshold. Cached/fast-loading images
 * appear immediately without animation to avoid unnecessary visual transitions.
 *
 * @param model The image URL or request object.
 * @param contentDescription Accessibility description for the image.
 * @param modifier Modifier to apply to the image.
 * @param contentScale How to scale the image content.
 * @param placeholder Painter to display while loading.
 * @param error Painter to display on load failure.
 * @param fadeDurationMillis Duration of the fade-in animation.
 * @param loadTimeThresholdMillis Only fade in if load takes longer than this threshold.
 */
@Composable
fun FadeInAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter? = null,
    error: Painter? = null,
    fadeDurationMillis: Int = 500,
    loadTimeThresholdMillis: Int = 250,
) {
    var alpha by remember { mutableFloatStateOf(0f) }
    var loadStartMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var shouldFadeIn by remember { mutableStateOf(false) }
    var imageLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(imageLoaded) {
        if (imageLoaded) {
            if (shouldFadeIn) {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(fadeDurationMillis),
                ) { value, _ ->
                    alpha = value
                }
            } else {
                alpha = 1f
            }
        }
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        modifier = modifier.alpha(alpha),
        onLoading = {
            imageLoaded = false
            shouldFadeIn = false
        },
        onSuccess = {
            imageLoaded = true
            shouldFadeIn = System.currentTimeMillis() - loadStartMillis > loadTimeThresholdMillis
        }
    )
}
