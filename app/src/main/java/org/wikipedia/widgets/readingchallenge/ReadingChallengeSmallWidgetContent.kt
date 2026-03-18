package org.wikipedia.widgets.readingchallenge

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
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
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.readingchallenge.WidgetCombinations.forToday
import java.time.LocalDate

@Composable
fun ReadingChallengeSmallWidgetContent(
    state: ReadingChallengeState
) {
    val context = LocalContext.current
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> {
            val enrollmentDate = LocalDate.parse(Prefs.readingChallengeEnrollmentDate)
            val combination = WidgetCombinations.enrolledNotStarted.forToday(enrollmentDate = enrollmentDate)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = androidx.glance.action.actionStartActivity<MainActivity>()),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                backgroundColor = combination.backgroundColor,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.feed),
                        action = androidx.glance.action.actionStartActivity<MainActivity>()
                    )
                }
            )
        }
        ReadingChallengeState.NotEnrolled -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_join_challenge_button),
                        action = actionStartActivity(MainActivity.newIntent(context)).also {
                            Prefs.readingChallengeOnboardingShown = false
                        }
                    )
                }
            )
        }
        ReadingChallengeState.NotLiveYet -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            val enrollmentDate = LocalDate.parse(Prefs.readingChallengeEnrollmentDate)
            val combination = WidgetCombinations.streakNeedsReading.forToday(enrollmentDate = enrollmentDate)
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                backgroundColor = combination.backgroundColor,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColorProvider = combination.contentColor,
                        textColorProvider = combination.contentColor
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            val enrollmentDate = LocalDate.parse(Prefs.readingChallengeEnrollmentDate)
            val combination = WidgetCombinations.streakOngoing.forToday(enrollmentDate = enrollmentDate)
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(
                        onClick = actionStartActivity<MainActivity>()
                    ),
                backgroundColor = combination.backgroundColor,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColorProvider = combination.contentColor,
                        textColorProvider = combination.contentColor
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

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetPreview() {
    SmallWidget(
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
