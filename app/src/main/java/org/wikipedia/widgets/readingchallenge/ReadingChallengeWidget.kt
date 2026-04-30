package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import org.wikipedia.settings.Prefs
import java.time.LocalDate

class ReadingChallengeWidget : GlanceAppWidget() {
    companion object {
        private val fullWidthThreshold = 320.dp
    }

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val repository = ReadingChallengeWidgetRepository(context)

        provideContent {
            val state by repository.observeState().collectAsState(initial = ReadingChallengeState.Loading)

            GlanceTheme {
                val size = LocalSize.current
                val enrollmentDate = if (Prefs.readingChallengeEnrollmentDate.isNotEmpty()) LocalDate.parse(Prefs.readingChallengeEnrollmentDate) else LocalDate.now()

                if (size.width >= fullWidthThreshold) {
                    ReadingChallengeLargeWidgetContent(state, enrollmentDate)
                } else {
                    ReadingChallengeSmallWidgetContent(state, enrollmentDate)
                }
            }
        }
    }
}

/**
 * Should be used as the base wrapper for all content in the widget to ensure a consistent corner radius background
 * across all api level
 */
@Composable
fun BaseWidgetContent(
    color: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
    ) {
        // base background color for widget
        Image(
            provider = ImageProvider(R.drawable.widget_shape_background),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(
                ColorProvider(day = color, night = color)
            )
        )
        content()
    }
}

@Composable
fun WidgetButton(
    text: String,
    action: Action,
    backgroundColor: Color = WidgetColors.progressive,
    contentColor: Color = WidgetColors.white,
    icon: ImageProvider? = null,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth()
) {
    FilledButton(
        text = text,
        onClick = action,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ColorProvider(day = backgroundColor, night = backgroundColor),
            contentColor = ColorProvider(day = contentColor, night = contentColor)
        ),
        icon = icon,
        modifier = modifier
    )
}

@Composable
fun WidgetBadge(
    text: String,
    textSize: TextUnit = 32.sp,
    iconResId: Int,
    iconSize: Dp = 16.dp,
    spacerWidth: Dp = 4.dp,
    iconTintColor: Color? = null,
    textColor: Color,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(iconResId),
            contentDescription = null,
            modifier = GlanceModifier
                .size(iconSize),
            colorFilter = iconTintColor?.let { ColorFilter.tint(ColorProvider(day = iconTintColor, night = iconTintColor)) }
        )
        Spacer(
            modifier = GlanceModifier.width(spacerWidth)
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = textSize,
                color = ColorProvider(day = textColor, night = textColor),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun WidgetIconButton(
    text: String,
    action: Action,
    iconResId: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    FilledButton(
        text = text,
        onClick = action,
        icon = ImageProvider(iconResId),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ColorProvider(day = WidgetColors.progressive, night = WidgetColors.progressive),
            contentColor = ColorProvider(day = WidgetColors.white, night = WidgetColors.white)
        ),
        modifier = modifier
    )
}

@Composable
fun ReadingChallengeWidgetLoading(
    modifier: GlanceModifier = GlanceModifier,
    backgroundColorResId: Int = WidgetBackground.challengeNotOptInRadialGradient
) {
    Box(
        modifier = modifier
    ) {
        // base background color for widget
        Image(
            provider = ImageProvider(backgroundColorResId),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize()
        )
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_w_logo_shadow),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp)
            )
        }
    }
}

enum class SmallWidgetSize {
    TINY,
    EXTRA_COMPACT,
    COMPACT,
    FULL;

    val mascotSize: Dp
        get() = when (this) {
            TINY -> 46.dp
            EXTRA_COMPACT -> 56.dp
            COMPACT -> 80.dp
            FULL -> 120.dp
        }

    val compactMascotSize: Dp
        get() = when (this) {
            TINY -> 36.dp
            EXTRA_COMPACT -> 46.dp
            COMPACT -> 70.dp
            FULL -> 120.dp
        }

    val badgeTextSize: TextUnit
        get() = when (this) {
            TINY -> 18.sp
            EXTRA_COMPACT -> 22.sp
            COMPACT -> 26.sp
            FULL -> 32.sp
        }

    val badgeIconSize: Dp
        get() = when (this) {
            TINY -> 22.dp
            EXTRA_COMPACT -> 28.dp
            COMPACT -> 36.dp
            FULL -> 40.dp
        }

    val paddingBetweenMascotAndTitleIcon: Dp
        get() = when (this) {
            TINY, EXTRA_COMPACT -> 0.dp
            COMPACT, FULL -> 16.dp
        }

    val titleBarIconSize: Dp
        get() = when (this) {
            TINY -> 22.dp
            EXTRA_COMPACT -> 28.dp
            COMPACT -> 32.dp
            FULL -> 36.dp
        }

    companion object {
        fun from(size: DpSize): SmallWidgetSize {
            val constraint = minOf(size.width, size.height)
            return when {
                constraint <= 140.dp -> TINY
                constraint <= 176.dp -> EXTRA_COMPACT
                constraint <= 230.dp -> COMPACT
                else -> FULL
            }
        }
    }
}

enum class LargeWidgetSize {
    TINY,
    EXTRA_COMPACT,
    COMPACT,
    FULL;

    val titleTextSize: TextUnit
        get() = when (this) {
            TINY -> 16.sp
            EXTRA_COMPACT -> 18.sp
            COMPACT -> 24.sp
            FULL -> 32.sp
        }

    val subtitleTextSize: TextUnit
        get() = when (this) {
            TINY -> 12.sp
            EXTRA_COMPACT, COMPACT -> 14.sp
            FULL -> 16.sp
        }

    val trophyIconSize: Dp
        get() = when (this) {
            TINY -> 18.dp
            EXTRA_COMPACT -> 20.dp
            COMPACT -> 22.dp
            FULL -> 24.dp
        }

    val titleBarTextSize: TextUnit
        get() = when (this) {
            TINY -> 12.sp
            EXTRA_COMPACT -> 13.sp
            COMPACT -> 14.sp
            FULL -> 16.sp
        }

    val streakBadgeTextSize: TextUnit
        get() = when (this) {
            TINY -> 18.sp
            EXTRA_COMPACT -> 20.sp
            COMPACT -> 26.sp
            FULL -> 32.sp
        }

    val streakBadgeIconSize: Dp
        get() = when (this) {
            TINY -> 22.dp
            EXTRA_COMPACT -> 28.dp
            COMPACT -> 36.dp
            FULL -> 40.dp
        }

    val titleBarIconSize: Dp
        get() = when (this) {
            TINY -> 22.dp
            EXTRA_COMPACT -> 24.dp
            COMPACT -> 32.dp
            FULL -> 36.dp
        }

    // Mascot size when bottom content is not empty.
    val sideMascotSize: Dp
        get() = when (this) {
            TINY -> 40.dp
            EXTRA_COMPACT -> 60.dp
            COMPACT -> 80.dp
            FULL -> 120.dp
        }

    // Mascot size when bottom content is empty/spacer-only. Can expand down further.
    val expandedMascotSize: Dp
        get() = when (this) {
            TINY -> 48.dp
            EXTRA_COMPACT -> 64.dp
            COMPACT -> 95.dp
            FULL -> 120.dp
        }

    val overlayMascotSize: Dp
        get() = when (this) {
            TINY -> 40.dp
            EXTRA_COMPACT -> 60.dp
            COMPACT -> 80.dp
            FULL -> 100.dp
        }

    companion object {
        fun from(size: DpSize): LargeWidgetSize = when {
            size.height <= 140.dp -> TINY
            size.height <= 176.dp -> EXTRA_COMPACT
            size.height <= 230.dp -> COMPACT
            else -> FULL
        }
    }
}
