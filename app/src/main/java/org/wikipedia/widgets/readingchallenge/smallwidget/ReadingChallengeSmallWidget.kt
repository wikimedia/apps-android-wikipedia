package org.wikipedia.widgets.readingchallenge.smallwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.WidgetBadge
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors

@Composable
fun ReadingChallengeSmallContent(
    state: ReadingChallengeState
) {
    val context = LocalContext.current

    when (state) {
        ReadingChallengeState.NotLiveYet -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = "Join Challenge",
                        action = actionStartActivity(MainActivity.newIntent(WikipediaApp.instance).putExtra("fromWidget", true))
                    )
                }
            )
        }
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> TODO()
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                backgroundColor = WidgetColors.streakOngoingNotReadBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColorProvider = WidgetColors.streakOngoingNotReadContent,
                        textColorProvider = WidgetColors.streakOngoingNotReadContent
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(
                        onClick = actionStartActivity<MainActivity>()
                    ),
                backgroundColor = WidgetColors.streakOngoingNotReadBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColorProvider = WidgetColors.streakOngoingNotReadContent,
                        textColorProvider = WidgetColors.streakOngoingNotReadContent
                    )
                }
            )
        }
    }
}
