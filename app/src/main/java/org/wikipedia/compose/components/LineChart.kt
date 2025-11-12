package org.wikipedia.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import kotlin.math.max

@Composable
fun LineChart(
    map: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    chartSampleSize: Int = 10,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = WikipediaTheme.colors.progressiveColor
) {
    if (map.isEmpty()) {
        return
    }

    val sortedValues = map.entries.sortedBy { it.key }.map { it.value }

    // Downsample the data to avoid the chart being too crowded in a small layout.
    val downsampledList = if (sortedValues.size <= chartSampleSize) {
        sortedValues
    } else {
        val chunkSize = sortedValues.size / chartSampleSize
        sortedValues
            .chunked(chunkSize)
            .map { it.average().toInt() }
    }

    val maxValue = downsampledList.maxOrNull() ?: 1
    val minValue = 0
    val range = max(maxValue - minValue, 1)

    Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val xScale = canvasWidth / (downsampledList.size - 1)
        val yScale = canvasHeight / range

        val path = Path()
        val firstY = canvasHeight - ((downsampledList.first() - minValue) * yScale)
        path.moveTo(0f, firstY)

        var index = 0
        downsampledList.forEach {
            if (index == 0) {
                index++ // Skip the first point since we already moved to it
                return@forEach
            }
            val x = index++ * xScale
            val y = canvasHeight - ((it - minValue) * yScale)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LineChartPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LineChart(
                map = mapOf(
                    LocalDate(2025, 1, 1) to 100,
                    LocalDate(2025, 1, 2) to 122,
                    LocalDate(2025, 1, 3) to 100,
                    LocalDate(2025, 1, 4) to 103,
                    LocalDate(2025, 1, 5) to 120,
                    LocalDate(2025, 1, 6) to 121,
                    LocalDate(2025, 1, 7) to 110,
                    LocalDate(2025, 1, 8) to 153,
                    LocalDate(2025, 1, 9) to 100,
                    LocalDate(2025, 1, 10) to 150,
                    LocalDate(2025, 1, 11) to 160,
                    LocalDate(2025, 1, 12) to 170,
                    LocalDate(2025, 1, 13) to 180,
                    LocalDate(2025, 1, 14) to 140,
                    LocalDate(2025, 1, 15) to 130,
                    LocalDate(2025, 1, 16) to 106,
                    LocalDate(2025, 1, 17) to 102,
                    LocalDate(2025, 1, 18) to 103,
                    LocalDate(2025, 1, 19) to 95,
                    LocalDate(2025, 1, 20) to 76,
                ),
                modifier = Modifier.fillMaxSize(),
                chartSampleSize = 10,
            )
        }
    }
}
