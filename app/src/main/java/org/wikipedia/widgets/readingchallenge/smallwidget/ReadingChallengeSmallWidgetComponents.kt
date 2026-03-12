package org.wikipedia.widgets.readingchallenge.smallwidget

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors

@SuppressLint("RestrictedApi")
@Composable
fun SmallWidget(
    modifier: GlanceModifier = GlanceModifier,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
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
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(titleBarIcon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())

            Image(
                provider = ImageProvider(mainImageResId),
                contentDescription = null,
                modifier = GlanceModifier.size(120.dp)
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            bottomContent()
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetPreview() {
    SmallWidget(
        modifier = GlanceModifier
            .background(WidgetColors.challengeNotOptInBackground),
        mainImageResId = R.drawable.globe,
        bottomContent = {
            WidgetButton(
                text = "Explore",
                action = actionStartActivity(Intent())
            )
        }
    )
}
