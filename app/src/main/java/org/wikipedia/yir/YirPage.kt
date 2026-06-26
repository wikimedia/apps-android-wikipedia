package org.wikipedia.yir

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A single Year in Review story card.
 *
 * The scaffold owns everything fixed: the top bar slot, the swipe navigation, the full-bleed
 * background layer and the progress indicator. The only thing that varies per card is
 * [content].
 *
 * Navigation is a vertical swipe pager (like the "For You" feed), not tap-zones. We landed on
 * this because tap-zones fight interactive cards' option buttons: if the right half means
 * "next card", it collides with the user tapping an option. Making card-to-card movement a
 * swipe (and never a tap) removes that collision. Tap is reserved for intra-card actions only
 * (e.g. skipping a standard card's animation early).
 *
 * Caveat: holding a `@Composable` lambda in a data model is not "pure data" (it can't be
 * serialized or previewed as data). We accept this for the spike because it keeps the scaffold
 * completely agnostic to what the middle content is. If cards ever need to come from a backend,
 * this would become a sealed type rendered by a `when` instead.
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

/**
 * The full-bleed background layer is source-agnostic: a card may use a solid color, an image,
 * or a Lottie animation. The scaffold renders whichever variant a card supplies.
 */
sealed interface YirBackground {
    data class Solid(val color: Color) : YirBackground

    /**
     * A vertical gradient, e.g. the teal -> green -> pale look from the current Year in Review.
     * Colors are drawn top-to-bottom in the order given.
     */
    data class Gradient(val colors: List<Color>) : YirBackground

    data class Image(@DrawableRes val resId: Int) : YirBackground

    /**
     * @param assetPath the Lottie asset to play full-bleed, relative to `assets/`, e.g.
     *   "lottie/yir_minutes_read.lottie". Matches the app convention (see
     *   ExploreFeedBuildingActivity): dotLottie/JSON files live in `app/src/main/assets/lottie/`
     *   and load via LottieCompositionSpec.Asset.
     * @param loop false for standard cards (play once, then hold/transition while text reveals);
     *   true for framing cards (subtle ambient loop behind foreground content).
     * @param reducedMotionFallbackResId shown instead of the animation when the OS reduced-motion
     *   setting is on. Required for looping framing animations; optional otherwise.
     */
    data class Animation(
        val assetPath: String,
        val loop: Boolean = false,
        @DrawableRes val reducedMotionFallbackResId: Int? = null
    ) : YirBackground
}

/**
 * Where a standard card is in its play sequence. Driven by the card's one-shot background animation;
 * cards without one (solid/gradient/image, or a looping animation) start already [REVEALED].
 *
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
