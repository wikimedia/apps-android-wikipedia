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
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = WikipediaTheme.colors.progressiveColor
) {
    val maxValue = map.values.maxOrNull() ?: 1
    val minValue = 0
    val range = max(maxValue - minValue, 1)

    Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        // Calculate the scaling factors
        val xScale = canvasWidth / (map.size - 1)
        val yScale = canvasHeight / range

        val path = Path()

        // Move to the first point
        val firstY = canvasHeight - ((map.values.first() - minValue) * yScale)
        path.moveTo(0f, firstY)

        // Loop through all points and draw lines
        var index = 0
        map.forEach { (key, value) ->
            if (index == 0) {
                index++ // Skip the first point since we already moved to it
                return@forEach
            }
            val x = index++ * xScale
            val y = canvasHeight - ((value - minValue) * yScale)
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
                    "2025-01-10" to 100,
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
