package org.wikipedia.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.feed.model.Card

@Composable
fun ForYouModulePager(
    modifier: Modifier = Modifier,
    module: ForYouModule,
    onCardInView: (card: Card) -> Unit = {},
    content: @Composable (page: Int) -> Unit,
) {
    var pagerBounds: Rect? = remember { null }
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                pagerBounds = coordinates.parentLayoutCoordinates?.boundsInRoot()
            }
    ) {
        val pagerState = rememberPagerState(pageCount = { module.cards.size })
        val aboveThresholdByPage = remember { mutableStateMapOf<Int, Boolean>() }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val viewport = pagerBounds ?: return@onGloballyPositioned
                    val cardBounds = coordinates.boundsInRoot()
                    val isAboveThreshold = cardBounds.width > viewport.width / 2 && cardBounds.height > viewport.height / 2
                    val wasAboveThreshold = aboveThresholdByPage[page] == true
                    if (isAboveThreshold && !wasAboveThreshold) {
                        onCardInView(module.cards[page])
                    }
                    aboveThresholdByPage[page] = isAboveThreshold
                }
            ) {
                content(page)
            }
        }

        if (module.cards.size > 1) {
            PageIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 16.dp),
                pagerState = pagerState,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
