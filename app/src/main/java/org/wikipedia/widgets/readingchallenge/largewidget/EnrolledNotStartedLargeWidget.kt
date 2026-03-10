package org.wikipedia.widgets.readingchallenge.largewidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import org.wikipedia.widgets.readingchallenge.smallwidget.components.WidgetIconButton

@Composable
fun EnrolledNotStartedLargeWidget(
    modifier: GlanceModifier = GlanceModifier,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    mainImageResId: Int,
) {
    val context = LocalContext.current
    val textColor = WidgetColors.primary
    Column (
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(WidgetColors.challengeNotOptInBackground)
            .padding(16.dp),
    ) {
        Row (
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxWidth()
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
            ) {
                Text(
                    text = "Ready, set, read!",
                    style = TextStyle(
                        fontSize = 32.sp,
                        color = ColorProvider(day = textColor, night = textColor),
                        fontWeight = FontWeight.Medium,
                    )
                )
                Text(
                    text = "Start working towards a 25-day streak!",
                    style = TextStyle(
                        fontSize = 16.sp,
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
                    modifier = GlanceModifier
                        .size(110.dp)
                )
                Spacer(modifier = GlanceModifier.size(24.dp))
            }
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            WidgetIconButton(
                modifier = GlanceModifier
                    .defaultWeight(),
                text = "Search",
                iconResId = R.drawable.outline_search_24,
                action = actionRunCallback<SearchAction>()
            )
            Spacer(modifier = GlanceModifier.width(16.dp))
            WidgetIconButton(
                modifier = GlanceModifier
                    .defaultWeight(),
                text = "Random",
                iconResId = R.drawable.ic_dice_24,
                action = actionRunCallback<RandomizerAction>()
            )
        }
    }
}
