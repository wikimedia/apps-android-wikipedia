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

@Composable
private fun YirCard(
    page: YirPage,
    isActive: Boolean
) {
    var phase by remember(page) { mutableStateOf(page.background.initialPhase()) }

    Box(modifier = Modifier.fillMaxSize()) {
        YirBackgroundLayer(
            background = page.background,
            isActive = isActive,
            onAnimationFinished = { phase = YirCardPhase.REVEALED }
        )

        page.content(phase)
    }
}
