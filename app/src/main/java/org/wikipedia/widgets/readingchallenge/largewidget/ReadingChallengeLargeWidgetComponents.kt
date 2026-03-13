package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
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
import androidx.glance.layout.fillMaxHeight
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
    textColor: Color,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    title: String,
    titleFontSize: TextUnit,
    subTitle: String,
    subTitleFontSize: TextUnit,
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
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Image(
                provider = ImageProvider(titleBarIcon),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(mainImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(120.dp)
                )
            }
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(end = 100.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = titleFontSize,
                            color = ColorProvider(day = textColor, night = textColor)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Text(
                        text = subTitle,
                        style = TextStyle(
                            fontSize = subTitleFontSize,
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
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
        textColor = ComposeColors.Gray700,
        title = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_title),
        titleFontSize = 34.sp,
        subTitle = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_description),
        subTitleFontSize = 16.sp,
        mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
        bottomContent = {
            WidgetButton(
                text = LocalContext.current.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                action = actionStartActivity(Intent())
            )
        }
    )
}
