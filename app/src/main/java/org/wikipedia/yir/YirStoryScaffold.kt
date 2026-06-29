package org.wikipedia.yir

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** How the story pages advance. Vertical = swipe up (For You). Horizontal = swipe left/right (carousel). */
enum class YirPagerOrientation { VERTICAL, HORIZONTAL }

/**
 * The fixed Year in Review story shell. Everything in here is the same for every card; the only
 * thing that varies is each [YirPage]'s content.
 *
 * Responsibilities:
 *  - Swipe paging between cards (vertical = For You style, horizontal = carousel style), chosen via
 *    [orientation]. Switching is a one-line difference (VerticalPager vs HorizontalPager); we expose
 *    it as a flag so design can feel both. Swipe is the only way between cards — tap never navigates.
 *  - The full-bleed background per card (solid / gradient / image / animation), via [YirBackgroundLayer].
 *  - The fixed top bar overlay (close, donate, open slot), via [YirTopBar].
 *  - The stories-style progress indicator: a horizontal bar under the top bar in horizontal mode, or
 *    a vertical bar down the right edge in vertical mode.
 *  - Per-card phase wiring: a one-shot background animation flips the card to REVEALED and the text
 *    auto-fades in. No tap-to-skip, no timer, no auto-advance — swipe only. (See NOTES.)
 */
@Composable
fun YirStoryScaffold(
    pages: List<YirPage>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    orientation: YirPagerOrientation = YirPagerOrientation.VERTICAL,
    onDonate: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = modifier.fillMaxSize()) {
        when (orientation) {
            YirPagerOrientation.VERTICAL ->
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    YirCard(page = pages[page], isActive = pagerState.settledPage == page)
                }
            YirPagerOrientation.HORIZONTAL ->
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    YirCard(page = pages[page], isActive = pagerState.settledPage == page)
                }
        }

        // Top overlay: the top bar, with the horizontal progress bar right under it (horizontal mode).
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            YirTopBar(onClose = onClose, onDonate = onDonate)
            if (orientation == YirPagerOrientation.HORIZONTAL) {
                YirProgressIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    orientation = orientation,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Vertical mode: the progress runs down the right edge.
        if (orientation == YirPagerOrientation.VERTICAL) {
            YirProgressIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                orientation = orientation,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            )
        }
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
