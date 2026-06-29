package org.wikipedia.yir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme

@Composable
fun YirProgressIndicator(
    pageCount: Int,
    currentPage: Int,
    orientation: YirPagerOrientation,
    modifier: Modifier = Modifier
) {
    val filled = Color.White
    val empty = Color.White.copy(alpha = 0.3f)
    val thickness = 3.dp
    val corner = RoundedCornerShape(thickness / 2)

    if (orientation == YirPagerOrientation.HORIZONTAL) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(thickness)
                        .clip(corner)
                        .background(if (index <= currentPage) filled else empty)
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxHeight(0.5f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(thickness)
                        .clip(corner)
                        .background(if (index <= currentPage) filled else empty)
                )
            }
        }
    }
}

@Preview
@Composable
private fun YirProgressIndicatorHorizontalPreview() {
    BaseTheme {
        Box(Modifier.background(Color(0xFF0A3D3A)).fillMaxWidth()) {
            YirProgressIndicator(
                pageCount = 8,
                currentPage = 1,
                orientation = YirPagerOrientation.HORIZONTAL,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
