package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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

class ReadingChallengeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        provideContent {
            GlanceTheme {
                MediumWidgetContent(
                    totalDays = 25,
                    currentStreak = 15
                )
            }
        }
    }
}

@Composable
fun MediumWidgetContent(totalDays: Int, currentStreak: Int) {
    // Colors matching the image theme
    val backgroundColor = Color(0xFFDDE5FA)
    val darkBlueText = Color(0xFF3861B8)
    val bottomBarColor = Color(0xFF3866C9)
    val progressLightColor = Color(0xFF599492)
    val progressWhiteColor = Color(0xFFFFFFFF)

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
                    provider = ImageProvider(R.drawable.outline_trending_up_24),
                    contentDescription = "Trophy",
                    modifier = GlanceModifier.size(24.dp)
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = LocalContext.current.getString(R.string.more_events_text),
                    style = TextStyle(
                        color = ColorProvider(day = darkBlueText, night = darkBlueText),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // W logo (Placeholder)
                Box(
                    modifier = GlanceModifier.size(24.dp).background(Color.White).cornerRadius(4.dp)
                ) {}
            }

            // Middle Row: Flame, Days. (With defaultWeight, it dynamically pushes the rows apart to match the design spacing perfectly)
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Flame
                Image(
                    provider = ImageProvider(R.drawable.baseline_extension_24),
                    contentDescription = "Flame",
                    modifier = GlanceModifier.size(45.dp),
                    colorFilter = ColorFilter.tint(
                        ColorProvider(
                            day = darkBlueText,
                            night = darkBlueText
                        )
                    )
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                Text(
                    text = "$currentStreak days",
                    modifier = GlanceModifier.padding(top = 4.dp), // Adjust this padding to visually align the text baseline with the flame
                    style = TextStyle(
                        color = ColorProvider(day = darkBlueText, night = darkBlueText),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            // Bottom Row: Progress bar
            BoxedStreakProgressBar(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(bottomBarColor)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                currentStreak = currentStreak,
                totalDays = totalDays,
                startIconResId = R.drawable.baseline_event_repeat_24,
                endIconResId = R.drawable.baseline_event_repeat_24
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
                    .size(110.dp)
                    .padding(end = 24.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
fun ReadingChallengeWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .cornerRadius(24.dp)
            .background(Color(0xFFDDE5FA))
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
        ) {
            Text("Reading Challenge", modifier = GlanceModifier.padding(8.dp))
            val dynamicWidth =
                (LocalSize.current.width - 40.dp).coerceAtLeast(0.dp) // 16dp padding on each side
            println("orange widget width: ${LocalSize.current.width}, dynamicWidth: $dynamicWidth")
            StreakProgressBar(
                currentStreak = 15,
                totalDays = 25,
                dynamicWidth = dynamicWidth,
            )
        }
    }
}
