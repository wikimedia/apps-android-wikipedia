package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors

@Composable
fun GeneralLargeWidget(
    modifier: GlanceModifier = GlanceModifier,
    dayTextColor: Color,
    nightTextColor: Color,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    title: String,
    subTitle: String,
    mainImageResId: Int,
    bottomContent: @Composable () -> Unit = { }
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .then(modifier)
            .padding(16.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Image(
                provider = ImageProvider(titleBarIcon),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 32.sp,
                            color = ColorProvider(day = dayTextColor, night = nightTextColor)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Text(
                        text = subTitle,
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = ColorProvider(day = dayTextColor, night = nightTextColor),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Image(
                    provider = ImageProvider(mainImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(120.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            bottomContent()
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun GeneralWidgetPreview() {
    GeneralLargeWidget(
        modifier = GlanceModifier
            .background(WidgetColors.challengeNotOptInBackground),
        dayTextColor = ComposeColors.Gray700,
        nightTextColor = ComposeColors.Gray200,
        title = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_title),
        subTitle = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_description),
        mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
        bottomContent = {
            WidgetButton(
                text = LocalContext.current.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                action = actionStartActivity(Intent())
            )
        }
    )
}
