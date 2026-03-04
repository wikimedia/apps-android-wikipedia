package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

class ReadingChallengeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val repository = ReadingChallengeWidgetRepository(context)

        provideContent {
            val state by repository.observeState().collectAsState(initial = ReadingChallengeState.NotLiveYet)
            GlanceTheme {
                ReadingChallengeContent(state)
            }
        }
    }
}

@Composable
fun ReadingChallengeContent(
    state: ReadingChallengeState,
    modifier: Modifier = Modifier
) {
    // each state will have small and large widget content
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> TODO()
        ReadingChallengeState.NotLiveYet -> TODO()
        is ReadingChallengeState.StreakOngoingNeedsReading -> TODO()
        is ReadingChallengeState.StreakOngoingReadToday -> TODO()
    }
}
