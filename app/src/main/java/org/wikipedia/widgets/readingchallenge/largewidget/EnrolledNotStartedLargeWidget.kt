package org.wikipedia.widgets.readingchallenge.largewidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.widgets.readingchallenge.WidgetColors
import org.wikipedia.widgets.readingchallenge.WidgetIconButton
import org.wikipedia.widgets.utils.FontUtils

@Composable
fun EnrolledNotStartedLargeWidget(
    modifier: GlanceModifier = GlanceModifier,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    mainImageResId: Int,
) {
    val context = LocalContext.current
    val textColor = WidgetColors.primary

    // Accurately available space for the text column
    // (widget width minus image column ~110dp, padding, spacing)
    val contentSize = ReadingWidgetDimensions.contentSize
    val textColumnWidth = contentSize.width - 110.dp - ReadingWidgetDimensions.contentSpacing

    // Title: fits in ~40% of vertical text area
    val (titleFontSize, titleMaxLines) = FontUtils.calculateFontSizeAndMaxLines(
        context = context,
        text = "Ready, set, read!",
        availableWidth = textColumnWidth,
        availableHeight = contentSize.height * 0.40f,
        maxFontSize = 32.sp,
        minFontSize = 18.sp,
    )

    // Subtitle: fits in ~35% of vertical text area
    val (subtitleFontSize, subtitleMaxLines) = FontUtils.calculateFontSizeAndMaxLines(
        context = context,
        text = "Start working towards a 25-day streak!",
        availableWidth = textColumnWidth,
        availableHeight = contentSize.height * 0.35f,
        maxFontSize = 16.sp,
        minFontSize = 12.sp,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(WidgetColors.challengeNotOptInBackground)
            .padding(ReadingWidgetDimensions.widgetPadding),
    ) {
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxWidth()
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()) {
                Text(
                    text = "Ready, set, read!",
                    style = TextStyle(
                        fontSize = titleFontSize,
                        color = ColorProvider(day = textColor, night = textColor),
                        fontWeight = FontWeight.Medium,
                    )
                )
                Text(
                    text = "Start working towards a 25-day streak!",
                    maxLines = subtitleMaxLines,
                    style = TextStyle(
                        fontSize = subtitleFontSize,
                        color = ColorProvider(day = textColor, night = textColor),
                        fontWeight = FontWeight.Medium,
                    )
                )
            }
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End
            ) {
                Image(
                    provider = ImageProvider(titleBarIcon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(mainImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(110.dp)
                )
                Spacer(modifier = GlanceModifier.size(24.dp))
            }
        }

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetIconButton(
                modifier = GlanceModifier.defaultWeight(),
                text = "Search",
                iconResId = R.drawable.outline_search_24,
                action = actionRunCallback<SearchAction>()
            )
            Spacer(modifier = GlanceModifier.width(16.dp))
            WidgetIconButton(
                modifier = GlanceModifier.defaultWeight(),
                text = "Random",
                iconResId = R.drawable.ic_dice_24,
                action = actionRunCallback<RandomizerAction>()
            )
        }
    }
}

private object ReadingWidgetDimensions {
    val widgetPadding = 16.dp
    val contentSpacing = 12.dp

    val contentSize: DpSize
        @Composable get() {
            val size = LocalSize.current
            return DpSize(
                width = size.width - (2f * widgetPadding),
                // subtract button row height (~48dp) and spacing
                height = size.height - widgetPadding - 48.dp - contentSpacing
            )
        }
}
