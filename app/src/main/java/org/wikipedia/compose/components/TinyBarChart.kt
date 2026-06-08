package org.wikipedia.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme
import kotlin.math.max

@Composable
fun TinyBarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    minColor: Color = Color.Gray,
    maxColor: Color = Color.Green
) {
    val maxValue = values.maxOrNull() ?: 1
    val minValue = 0
    val valueRange = max(maxValue - minValue, 1)

    Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val spacing = 6.dp.toPx()
        val totalSpacing = spacing * (values.size - 1)
        val barWidth = (canvasWidth - totalSpacing) / values.size

        values.forEachIndexed { index, value ->
            val barHeight = max(if (maxValue > 0) {
                (value.toFloat() / maxValue.toFloat()) * canvasHeight
            } else {
                0f
            }, 4.dp.toPx())

            val colorProgress = if (valueRange > 0) {
                (value - minValue).toFloat() / valueRange.toFloat()
            } else {
                1f
            }

            val barColor = Color(
                red = minColor.red + (maxColor.red - minColor.red) * colorProgress,
                green = minColor.green + (maxColor.green - minColor.green) * colorProgress,
                blue = minColor.blue + (maxColor.blue - minColor.blue) * colorProgress,
                alpha = minColor.alpha + (maxColor.alpha - minColor.alpha) * colorProgress
            )

            // Draw the bar from bottom up
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = index * (barWidth + spacing), y = canvasHeight - barHeight),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TinyBarChartPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SearchEmptyView(
                emptyTexTitle = "No languages found"
            )
        }
    }
}
