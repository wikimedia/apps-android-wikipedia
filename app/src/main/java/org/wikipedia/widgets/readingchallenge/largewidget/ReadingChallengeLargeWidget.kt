package org.wikipedia.widgets.readingchallenge.largewidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors
import org.wikipedia.widgets.readingchallenge.smallwidget.SmallWidget

@Composable
fun ReadingChallengeLargeContent(
    state: ReadingChallengeState
) {
    // each state will have small and large widget content
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> TODO()
        ReadingChallengeState.NotLiveYet -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = "Large Widget",
                        action = actionStartActivity(MainActivity.newIntent(WikipediaApp.instance).putExtra("fromWidget", true))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            StreakOngoingNeedsReadingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(WidgetColors.streakOngoingNotReadBackground)
                    .padding(16.dp)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                state = state,
                mascotImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            StreakOngoingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.normalReadingBackground)
                    .padding(16.dp)
                    .cornerRadius(24.dp)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                state = state,
                mascotImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
    }
}
