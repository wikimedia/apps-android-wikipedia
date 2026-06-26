package org.wikipedia.yir

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The fixed Year in Review story shell. Everything in here is the same for every card; the only
 * thing that varies is each [YirPage]'s content.
 *
 * Responsibilities:
 *  - Vertical swipe paging between cards (the one, uniform way to move between cards — like the
 *    "For You" feed). We use swipe, not tap-zones, so paging never collides with interactive
 *    cards' option buttons.
 *  - The full-bleed background per card (image / animation / color), via [YirBackgroundLayer].
 *  - The fixed top bar overlay (close, donate, open slot), via [YirTopBar].
 *  - Per-card phase wiring: a one-shot background animation flips the card to REVEALED and the
 *    text auto-fades in. There is no tap-to-skip and no timer / no auto-advance to the next card —
 *    swipe is the only way between cards. (Deliberate limitations; see NOTES.)
 *
 * The progress indicator is intentionally not here yet (Step 4).
 */
@Composable
fun YirStoryScaffold(
    pages: List<YirPage>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDonate: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            YirCard(
                page = pages[page],
                isActive = pagerState.settledPage == page
            )
        }

        YirTopBar(
            onClose = onClose,
            onDonate = onDonate,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * A single card: full-bleed background + content, with the phase shared between them.
 *
 * Phase lives here (per card), not in a scaffold-wide machine, because card types have different
 * lifecycles. A standard card's one-shot animation finishing moves it from ANIMATING to REVEALED;
 * the text then auto-fades in. There is intentionally no tap-to-skip: tap never navigates in this
 * experience (swipe is the only way between cards), so tap is left entirely to real controls
 * (CTA buttons, interactive options). The trade-off — you can't skip a long animation mid-play —
 * is accepted to keep one consistent gesture model across all card types.
 */
@Composable
private fun YirCard(
    page: YirPage,
    isActive: Boolean
) {
    var phase by remember(page) { mutableStateOf(page.background.initialPhase()) }

    Box(modifier = Modifier.fillMaxSize()) {
        // A one-shot animation plays then freezes on its last frame; the content is drawn on top and
        // fades in once the animation finishes.
        YirBackgroundLayer(
            background = page.background,
            isActive = isActive,
            onAnimationFinished = { phase = YirCardPhase.REVEALED }
        )

        page.content(phase)
    }
}
