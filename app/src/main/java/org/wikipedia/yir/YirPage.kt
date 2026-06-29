package org.wikipedia.yir

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A single Year in Review story card. The scaffold owns everything fixed; only [content] varies.
 */
data class YirPage(
    val background: YirBackground,
    val content: YirContent
)

/**
 * The middle content of a card. Receives the current [YirCardPhase] so it can react to the
 * "animation playing -> text revealed" transition (e.g. fade the insight text in when
 * [YirCardPhase.REVEALED]).
 *
 * Phase lives with the card, not in a scaffold-wide state machine, because card types have
 * different internal lifecycles: standard cards go ANIMATING -> REVEALED, interactive cards go
 * pre-selection -> submitted -> reveal, and framing cards have no phase at all. A single shared
 * phase machine wouldn't generalize, so the scaffold stays out of it. Cards that don't care
 * about the phase (interactive, framing) simply ignore this argument.
 */
typealias YirContent = @Composable (phase: YirCardPhase) -> Unit

sealed interface YirBackground {
    data class Solid(val color: Color) : YirBackground

    data class Gradient(val colorStops: List<Pair<Float, Color>>) : YirBackground

    data class Image(@DrawableRes val resId: Int) : YirBackground

    /**
     * @param assetPath the Lottie asset to play full-bleed, relative to `assets/`, e.g. "lottie/yir_minutes_read.lottie".
     * @param loop false for standard cards (play once, then hold/transition while text reveals);
     *  true for framing cards (subtle ambient loop behind foreground content).
     * @param reducedMotionFallbackResId shown instead of the animation when the OS reduced-motion
     *  setting is on. Required for looping framing animations; optional otherwise.
     */
    data class Animation(
        val assetPath: String,
        val loop: Boolean = false,
        @DrawableRes val reducedMotionFallbackResId: Int? = null
    ) : YirBackground
}

/**
 * A standard card's play state.
 * [ANIMATING] - the one-shot background animation is playing.
 * [REVEALED]  - the animation has finished (and holds on its last frame) and the text shows on top.
 */
enum class YirCardPhase {
    ANIMATING,
    REVEALED
}

/** The starting phase for a card: ANIMATING only if its background is a one-shot animation. */
fun YirBackground.initialPhase(): YirCardPhase {
    return if (this is YirBackground.Animation && !loop) YirCardPhase.ANIMATING else YirCardPhase.REVEALED
}
