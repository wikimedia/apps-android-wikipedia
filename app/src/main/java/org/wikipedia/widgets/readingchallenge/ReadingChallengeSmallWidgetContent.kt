package org.wikipedia.widgets.readingchallenge

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
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

@Composable
fun ReadingChallengeSmallWidgetContent(
    state: ReadingChallengeState
) {
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> TODO()
        ReadingChallengeState.NotLiveYet -> {}
        is ReadingChallengeState.StreakOngoingNeedsReading -> {}
        is ReadingChallengeState.StreakOngoingReadToday -> {}
    }
}

@Composable
fun SmallWidget(
    modifier: GlanceModifier = GlanceModifier,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    mainImageResId: Int,
    backgroundColor: Color,
    bottomContent: @Composable () -> Unit = { }
) {
    BaseWidgetContent(
        color = backgroundColor
    ) {
        Box(
            modifier = modifier,
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
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetPreview() {
    SmallWidget(
        modifier = GlanceModifier
            .background(WidgetColors.challengeNotOptInBackground),
        mainImageResId = R.drawable.globe,
        backgroundColor = WidgetColors.challengeNotOptInBackground,
        bottomContent = {
            WidgetButton(
                text = "Explore",
                action = actionStartActivity(Intent())
            )
        }
    )
}
