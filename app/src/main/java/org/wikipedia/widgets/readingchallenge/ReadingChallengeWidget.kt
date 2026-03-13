package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
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
                if (size.width >= fullWidthThreshold) {
                    ReadingChallengeLargeWidgetContent(state)
                } else {
                    ReadingChallengeSmallWidgetContent(state)
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
    textStyle: TextStyle? = null,
    modifier: GlanceModifier = GlanceModifier
) {
    Button(
        text = text,
        onClick = action,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ColorProvider(day = WidgetColors.progressive, night = WidgetColors.progressive),
            contentColor = ColorProvider(day = WidgetColors.white, night = WidgetColors.white)
        ),
        style = textStyle,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun WidgetBadge(
    text: String,
    iconResId: Int,
    iconSize: Dp = 16.dp,
    spacerWidth: Dp = 4.dp,
    iconTintColorProvider: Color,
    textColorProvider: Color,
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
            colorFilter = ColorFilter.tint(ColorProvider(day = iconTintColorProvider, night = iconTintColorProvider))
        )
        Spacer(
            modifier = GlanceModifier.width(spacerWidth)
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = 32.sp,
                color = ColorProvider(day = textColorProvider, night = textColorProvider),
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
