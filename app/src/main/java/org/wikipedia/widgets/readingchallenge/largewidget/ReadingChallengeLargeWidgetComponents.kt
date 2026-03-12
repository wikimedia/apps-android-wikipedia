package org.wikipedia.widgets.readingchallenge.largewidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R

@Composable
fun GeneralLargeWidget(
    modifier: GlanceModifier = GlanceModifier,
    textColor: Color,
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
            .padding(vertical = 12.dp, horizontal = 16.dp),
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
                .fillMaxSize()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                    Text(
                        text = subTitle,
                        style = TextStyle(
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
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
