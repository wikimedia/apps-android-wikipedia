package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.WidgetColors

class ReadingChallengeLargeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val repository = ReadingChallengeWidgetRepository(context)

        provideContent {
            val state by repository.observeState().collectAsState(initial = ReadingChallengeState.NotLiveYet)
            GlanceTheme {
                ReadingChallengeLargeContent(state)
            }
        }
    }
}

@Composable
fun ReadingChallengeLargeContent(
    state: ReadingChallengeState
) {
    // each state will have small and large widget content
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> {}
        ReadingChallengeState.ChallengeConcludedIncomplete -> {}
        ReadingChallengeState.ChallengeConcludedNoStreak -> {}
        ReadingChallengeState.ChallengeRemoved -> {}
        ReadingChallengeState.EnrolledNotStarted -> {}
        ReadingChallengeState.NotEnrolled -> {}
        ReadingChallengeState.NotLiveYet -> {}
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
