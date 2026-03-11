package org.wikipedia.widgets.readingchallenge.largewidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.widgets.readingchallenge.BoxedStreakProgressBar
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.WidgetBadge
import org.wikipedia.widgets.readingchallenge.WidgetColors

@Composable
fun StreakOngoingLargeWidget(
    state: ReadingChallengeState.StreakOngoingReadToday,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    backgroundColor: Color = WidgetColors.normalReadingBackground,
    contentColor: Color = WidgetColors.readingContent,
    progressColor: Color = WidgetColors.progressColor
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
            .cornerRadius(24.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top Row: Trophy, Title, W logo
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_trophy24dp),
                    contentDescription = "Trophy",
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(day = contentColor, night = contentColor))
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Text(
                    text = "25-day reading challenge",
                    style = TextStyle(
                        color = ColorProvider(day = contentColor, night = contentColor),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // W logo (Placeholder)
                Image(
                    provider = ImageProvider(titleBarIcon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
            }

            // Middle Row: Flame, Days. (With defaultWeight, it dynamically pushes the rows apart to match the design spacing perfectly)
            WidgetBadge(
                modifier = GlanceModifier
                    .defaultWeight(),
                text = streakText,
                iconResId = R.drawable.ic_flame_24dp,
                iconSize = 45.dp,
                iconTintColorProvider = contentColor,
                textColorProvider = contentColor
            )

            // Bottom Row: Progress bar
            BoxedStreakProgressBar(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(contentColor)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                currentStreak = state.streak,
                totalDays = ReadingChallengeWidgetRepository.READING_STREAK_GOAL,
                startIconResId = R.drawable.baseline_event_repeat_24,
                endIconResId = R.drawable.baseline_event_repeat_24,
                progressColor = progressColor,
                progressBarColor = WidgetColors.white
            )
        }

        // Mascot overlay positioned absolutely inside the Box bounds
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Image(
                provider = ImageProvider(R.drawable.globe),
                contentDescription = "Mascot",
                modifier = GlanceModifier
                    .size(120.dp)
                    .padding(end = 24.dp, bottom = 32.dp)
            )
        }
    }
}
