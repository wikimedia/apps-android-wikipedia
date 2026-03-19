package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
            val state by repository.observeState().collectAsState(initial = ReadingChallengeState.NotLiveYet)

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
    iconTintColor: Color,
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
            colorFilter = ColorFilter.tint(ColorProvider(day = iconTintColor, night = iconTintColor))
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
