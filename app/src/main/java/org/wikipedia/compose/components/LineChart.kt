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
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import kotlin.math.max

@Composable
fun LineChart(
    map: Map<String, Int>,
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
                    "2025-01-01" to 100,
                    "2025-01-02" to 122,
                    "2025-01-03" to 100,
                    "2025-01-04" to 103,
                    "2025-01-05" to 120,
                    "2025-01-06" to 121,
                    "2025-01-07" to 110,
                    "2025-01-08" to 153,
                    "2025-01-09" to 100,
                    "2025-01-10" to 150,
                    "2025-01-11" to 160,
                    "2025-01-12" to 170,
                    "2025-01-13" to 180,
                    "2025-01-14" to 140,
                    "2025-01-15" to 130,
                    "2025-01-16" to 106,
                    "2025-01-17" to 102,
                    "2025-01-18" to 103,
                    "2025-01-19" to 95,
                    "2025-01-20" to 76,
                ),
                modifier = Modifier.fillMaxSize(),
                chartSampleSize = 10,
            )
        }
    }
}
