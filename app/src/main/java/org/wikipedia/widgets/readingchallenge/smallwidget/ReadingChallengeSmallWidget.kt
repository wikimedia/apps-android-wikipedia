package org.wikipedia.widgets.readingchallenge.smallwidget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.WidgetColors
import org.wikipedia.widgets.readingchallenge.smallwidget.components.SmallWidget
import org.wikipedia.widgets.readingchallenge.smallwidget.components.WidgetBadge
import org.wikipedia.widgets.readingchallenge.smallwidget.components.WidgetButton

class ReadingChallengeSmallWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val repository = ReadingChallengeWidgetRepository(context)

        provideContent {
            val state by repository.observeState().collectAsState(initial = ReadingChallengeState.NotLiveYet)
            GlanceTheme {
                ReadingChallengeSmallContent(state)
            }
        }
    }
}

@Composable
fun ReadingChallengeSmallContent(
    state: ReadingChallengeState
) {
    when (state) {
        ReadingChallengeState.NotLiveYet -> {
            SmallWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground),
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
        ReadingChallengeState.EnrolledNotStarted -> {
            SmallWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = "Explore",
                        action = actionStartActivity(MainActivity.newIntent(WikipediaApp.instance).putExtra("fromWidget", true))
                    )
                }
            )
        }
        ReadingChallengeState.NotEnrolled -> TODO()
        is ReadingChallengeState.StreakOngoingNeedsReading -> TODO()
        is ReadingChallengeState.StreakOngoingReadToday -> {
            SmallWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = "${state.streak} ${if (state.streak == 1) "day" else "days"}",
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 24.dp,
                        iconTintColorProvider = WidgetColors.readingContent,
                        textColorProvider = WidgetColors.readingContent
                    )
                }
            )
        }
    }
}
