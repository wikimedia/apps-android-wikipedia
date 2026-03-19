package org.wikipedia.widgets.readingchallenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.WidgetCombinations.forToday
import java.time.LocalDate

@Composable
fun ReadingChallengeSmallWidgetContent(
    state: ReadingChallengeState,
    enrollmentDate: LocalDate
) {
    val context = LocalContext.current
    when (state) {
        ReadingChallengeState.ChallengeCompleted -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionRunCallback<ChallengeRewardAction>()),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        textSize = 16.sp,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 24.dp,
                        iconTintColor = WidgetColors.primary,
                        textColor = WidgetColors.primary
                    )
                    Spacer(
                        modifier = GlanceModifier.height(8.dp)
                    )
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_collect_prize_button),
                        action = actionRunCallback<ChallengeRewardAction>()
                    )
                }
            )
        }
        is ReadingChallengeState.ChallengeConcludedIncomplete -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, state.streak, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = streakText,
                        action = actionStartActivity(MainActivity.newIntent(context)),
                        backgroundColor = WidgetColors.challengeConcludedIncompleteButtonBackground,
                        contentColor = WidgetColors.primary,
                        icon = ImageProvider(R.drawable.ic_flame_24dp)
                    )
                }
            )
        }
        ReadingChallengeState.ChallengeConcludedNoStreak, ReadingChallengeState.ChallengeRemoved -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                }
            )
        }
        ReadingChallengeState.EnrolledNotStarted -> {
            val combination = WidgetCombinations.enrolledNotStarted.forToday(enrollmentDate = enrollmentDate)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                backgroundColor = combination.backgroundColor,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.feed),
                        action = actionStartActivity(MainActivity.newIntent(context))
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
                backgroundColor = WidgetColors.joinChallengeBackground,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_join_challenge_button),
                        action = actionRunCallback<JoinChallengeAction>()
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
                backgroundColor = WidgetColors.challengeNotLiveBackground,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            val combination = WidgetCombinations.streakNeedsReading.forToday(enrollmentDate = enrollmentDate)
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = combination.backgroundColor,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColor = combination.contentColor,
                        textColor = combination.contentColor
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            val combination = WidgetCombinations.streakOngoing.forToday(enrollmentDate = enrollmentDate)
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(
                        onClick = actionStartActivity(MainActivity.newIntent(context))
                    ),
                backgroundColor = combination.backgroundColor,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColor = combination.contentColor,
                        textColor = combination.contentColor
                    )
                }
            )
        }
    }
}

@Composable
fun SmallWidget(
    modifier: GlanceModifier = GlanceModifier,
    titleBarIcon: Int = R.drawable.ic_w_logo_shadow,
    mainImageResId: Int,
    backgroundColor: Color,
    bottomContent: @Composable () -> Unit = { }
) {
    println("orange size: ${LocalSize.current}")
    val size = LocalSize.current
    val mascotSize = if (size.height <= 200.dp) 80.dp else 120.dp
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
                    modifier = GlanceModifier.size(36.dp)
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
                    modifier = GlanceModifier.size(mascotSize)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                bottomContent()
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetNotEnrolledPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetNotLiveYetPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.NotLiveYet,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetStreakOngoingNeedsReadingPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.StreakOngoingNeedsReading(10),
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetChallengeCompletedPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.ChallengeCompleted,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetChallengeConcludedIncompletePreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedIncomplete(5),
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 250)
@Composable
fun SmallWidgetChallengeConcludedNoStreakPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedNoStreak,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 214, heightDp = 176)
@Composable
fun SmallWidgetNotEnrolledSmallHeightPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}
