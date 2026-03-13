package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.WidgetButton
import org.wikipedia.widgets.readingchallenge.WidgetColors

class ReadingChallengeLargeWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(350.dp, 110.dp)
        )
    )

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
    val context = LocalContext.current
    val size = LocalSize.current
    // TODO: handle font sizes correctly
    val titleFontSize = when {
        size.width <= 110.dp -> 22.sp
        size.width <= 250.dp -> 26.sp
        else -> 34.sp
    }
    val subTitleFontSize = when {
        size.width <= 110.dp -> 12.sp
        size.width <= 250.dp -> 14.sp
        else -> 16.sp
    }
    val textColor = ComposeColors.Gray700
    // each state will have small and large widget content
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground),
                textColor = textColor,
                title = context.getString(R.string.reading_challenge_widget_not_opted_in_title),
                titleFontSize = titleFontSize,
                subTitle = context.getString(R.string.reading_challenge_widget_not_opted_in_description),
                subTitleFontSize = subTitleFontSize,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_join_the_challenge_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        ReadingChallengeState.NotLiveYet -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .background(WidgetColors.challengeNotOptInBackground),
                textColor = textColor,
                title = context.getString(R.string.reading_challenge_widget_not_live_title),
                titleFontSize = titleFontSize,
                subTitle = context.getString(R.string.reading_challenge_widget_not_live_description),
                subTitleFontSize = subTitleFontSize,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> TODO()
        is ReadingChallengeState.StreakOngoingReadToday -> TODO()
    }
}
