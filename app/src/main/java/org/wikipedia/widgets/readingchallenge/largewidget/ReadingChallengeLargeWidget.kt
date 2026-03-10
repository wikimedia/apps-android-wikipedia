package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import org.wikipedia.R
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository

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
        ReadingChallengeState.EnrolledNotStarted -> {
            EnrolledNotStartedLargeWidget(
                mainImageResId = R.drawable.globe
            )
        }
        ReadingChallengeState.NotEnrolled -> {}
        ReadingChallengeState.NotLiveYet -> {}
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            StreakOngoingNeedsReadingLargeWidget(
                state = state,
                mainImageResId = R.drawable.globe
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            StreakOngoingLargeWidget(
                state = state
            )
        }
    }
}
