package org.wikipedia.widgets.readingchallenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity

@Composable
fun ReadingChallengeSmallWidgetContent(
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
                    .clickable(onClick = androidx.glance.action.actionStartActivity<MainActivity>()),
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
                        onClick = androidx.glance.action.actionStartActivity<MainActivity>()
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
