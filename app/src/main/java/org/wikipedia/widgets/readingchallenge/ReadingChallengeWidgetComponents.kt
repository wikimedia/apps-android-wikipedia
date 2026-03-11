package org.wikipedia.widgets.readingchallenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
fun WidgetButton(
    text: String,
    action: Action,
    modifier: GlanceModifier = GlanceModifier
) {
    Button(
        text = text,
        onClick = action,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ColorProvider(day = WidgetColors.progressive, night = WidgetColors.progressive),
            contentColor = ColorProvider(day = WidgetColors.white, night = WidgetColors.white)
        ),
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
