package org.wikipedia.yir

import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * The target duration we want a one-shot background animation to take before it holds its last
 * frame. A longer asset is sped up to roughly this so the insight text doesn't wait too long; a
 * shorter asset plays at its natural speed (we never slow one down).
 */
private const val ANIMATION_TARGET_MS = 2500f

/**
 * The full-bleed background of a card: a solid color, a gradient, an image, or an animation.
 *
 * A one-shot animation (loop = false) plays once, holds on its last frame as the background, and
 * calls [onAnimationFinished] so the card can fade its text in on top. A looping animation
 * (framing cards) loops while [isActive] and never finishes.
 *
 * @param isActive whether this is the settled/visible page. Animations only play while active so
 *   off-screen pages don't burn frames; a looping animation restarts from the start when the page
 *   becomes active again (matches the framing-card spec).
 */
@Composable
fun YirBackgroundLayer(
    background: YirBackground,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (background) {
            is YirBackground.Solid -> {
                Box(Modifier.fillMaxSize().background(background.color))
            }
            is YirBackground.Gradient -> {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(*background.colorStops.toTypedArray())))
            }
            is YirBackground.Image -> {
                Image(
                    painter = painterResource(background.resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is YirBackground.Animation -> {
                if (reducedMotionEnabled() && background.reducedMotionFallbackResId != null) {
                    Image(
                        painter = painterResource(background.reducedMotionFallbackResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    LaunchedEffect(Unit) { onAnimationFinished() }
                } else {
                    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(background.assetPath))
                    val iterations = if (background.loop) LottieConstants.IterateForever else 1
                    val speed = if (background.loop) {
                        1f
                    } else {
                        composition?.let { (it.duration / ANIMATION_TARGET_MS).coerceAtLeast(1f) } ?: 1f
                    }
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = iterations,
                        isPlaying = isActive,
                        speed = speed,
                        restartOnPlay = background.loop
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!background.loop) {
                        LaunchedEffect(progress, composition) {
                            if (composition != null && progress >= 1f) {
                                onAnimationFinished()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun reducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
