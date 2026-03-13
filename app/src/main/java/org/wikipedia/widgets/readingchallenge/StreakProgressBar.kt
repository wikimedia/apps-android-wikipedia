package org.wikipedia.widgets.readingchallenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import org.wikipedia.R

@Composable
fun StreakProgressBar(
    currentStreak: Int,
    totalDays: Int,
    dynamicWidth: Dp,
    modifier: GlanceModifier = GlanceModifier,
    progressColor: Color = Color(0xFF8DA6DD),
    progressBarColor: Color = Color(0xFFFFFFFF)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStreak !in 1..<totalDays) {
            // 100% completed state - show full progress bar
            Box(modifier = GlanceModifier
                .height(24.dp)
                .width(dynamicWidth)
                .background(if (currentStreak >= totalDays) progressColor else progressBarColor)
                .cornerRadius(12.dp)
            ) { }
        } else {
            // progressed math
            val progressPercent = (currentStreak.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

            // the max allowable width for the completed segment minus the dot and space
            val completedWidth = dynamicWidth * progressPercent

            // Completed portion
            Box(
                modifier = GlanceModifier
                    .height(24.dp)
                    .width(completedWidth)
            ) {
                Image(
                    provider = ImageProvider(R.drawable.progress_bar_start_bg),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(ColorProvider(day = progressColor, night = progressColor))
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Current position dot
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .background(progressColor)
                    .cornerRadius(12.dp)
            ) { }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Remaining portion
            val remainingWidth = (dynamicWidth - completedWidth).coerceAtLeast(0.dp)
            Box(
                modifier = GlanceModifier
                    .height(24.dp)
                    .width(remainingWidth)
            ) {
                Image(
                    provider = ImageProvider(R.drawable.progress_bar_end_bg),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(ColorProvider(day = progressBarColor, night = progressBarColor))
                )
            }
        }
    }
}

@Composable
fun BoxedStreakProgressBar(
    modifier: GlanceModifier = GlanceModifier,
    currentStreak: Int,
    totalDays: Int,
    startIconResId: Int,
    endIconResId: Int
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Calendar icon 1
        Image(
            provider = ImageProvider(startIconResId),
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(
                    day = Color.White,
                    night = Color.White
                )
            )
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Calculate precise DP width depending on current widget boundaries
        val paddingBuffer = 176.dp
        val dynamicWidth = (LocalSize.current.width - paddingBuffer).coerceAtLeast(0.dp)

        StreakProgressBar(
            currentStreak = currentStreak,
            totalDays = totalDays,
            dynamicWidth = dynamicWidth
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Calendar icon 25
        Image(
            provider = ImageProvider(endIconResId),
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(
                    day = Color.White,
                    night = Color.White
                )
            )
        )
    }
}
