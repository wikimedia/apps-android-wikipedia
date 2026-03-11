package org.wikipedia.widgets.readingchallenge.smallwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.WidgetBadge
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors

class ReadingChallengeSmallWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
    val context = LocalContext.current

    when (state) {
        ReadingChallengeState.NotLiveYet -> {}
        ReadingChallengeState.ChallengeCompleted -> {}
        ReadingChallengeState.ChallengeConcludedIncomplete -> {}
        ReadingChallengeState.ChallengeConcludedNoStreak -> {}
        ReadingChallengeState.ChallengeRemoved -> {}
        ReadingChallengeState.EnrolledNotStarted -> {
            SmallWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.feed),
                        action = actionStartActivity<MainActivity>()
                    )
                }
            )
        }
        ReadingChallengeState.NotEnrolled -> {}
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.streakOngoingNotReadBackground)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
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
                    .background(WidgetColors.streakOngoingNotReadBackground)
                    .clickable(
                        onClick = actionStartActivity<MainActivity>()
                    ),
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
