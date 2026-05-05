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
import org.wikipedia.widgets.readingchallenge.WidgetCombinations.forToday
import java.time.LocalDate

@Composable
fun ReadingChallengeSmallWidgetContent(
    state: ReadingChallengeState,
    enrollmentDate: LocalDate
) {
    val context = LocalContext.current
    when (state) {
        ReadingChallengeState.Loading -> {
            ReadingChallengeWidgetLoading(
                modifier = GlanceModifier
                    .fillMaxSize()
            )
        }

        ReadingChallengeState.ChallengeCompleted -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionRunCallback<ChallengeRewardAction>()),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.wp25_babyglobe_celebration_neutral,
                usCompactMascotSize = true,
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
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.wp25_babyglobe_reading,
                bottomContent = {
                    WidgetButton(
                        text = streakText,
                        action = actionRunCallback<HomeAction>(),
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
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.challengeCompletedBackground,
                mainImageResId = R.drawable.wp25_babyglobe_reading
            )
        }
        ReadingChallengeState.EnrolledNotStarted -> {
            val combination = WidgetCombinations.enrolledNotStarted.forToday(enrollmentDate = enrollmentDate)
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                mainImageResId = combination.iconResId,
                backgroundColor = combination.backgroundColor,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.feed),
                        action = actionRunCallback<HomeAction>()
                    )
                }
            )
        }
        ReadingChallengeState.NotEnrolled -> {
            SmallWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionRunCallback<JoinChallengeAction>()),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
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
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
                backgroundColor = WidgetColors.challengeNotLiveBackground,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_button),
                        action = actionRunCallback<HomeAction>()
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
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = combination.backgroundColor,
                mainImageResId = combination.iconResId,
                bottomContent = {
                    val size = SmallWidgetSize.from(LocalSize.current)
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_streak_warning,
                        iconSize = size.badgeIconSize,
                        textSize = size.badgeTextSize,
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
                        onClick = actionRunCallback<HomeAction>()
                    ),
                backgroundColor = combination.backgroundColor,
                mainImageResId = combination.iconResId,
                bottomContent = {
                    val size = SmallWidgetSize.from(LocalSize.current)
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = size.badgeIconSize,
                        textSize = size.badgeTextSize,
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
    usCompactMascotSize: Boolean = false,
    bottomContent: @Composable () -> Unit = { }
) {
    val size = SmallWidgetSize.from(LocalSize.current)
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
                    modifier = GlanceModifier.size(size.titleBarIconSize)
                )
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = size.paddingBetweenMascotAndTitleIcon),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(mainImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(if (usCompactMascotSize) size.compactMascotSize else size.mascotSize)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                bottomContent()
            }
        }
    }
}

// ChallengeCompleted: layout has (mascot + badge + spacer + button)
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130) // TINY
@Preview(widthDp = 200, heightDp = 141) // boundary: first EXTRA_COMPACT
@Preview(widthDp = 184, heightDp = 160) // EXTRA_COMPACT
@Preview(widthDp = 176, heightDp = 176) // boundary: last EXTRA_COMPACT
@Preview(widthDp = 180, heightDp = 177) // boundary: first COMPACT
@Preview(widthDp = 120, heightDp = 200) // COMPACT
@Preview(widthDp = 230, heightDp = 230) // boundary: last COMPACT
@Preview(widthDp = 230, heightDp = 231) // boundary: first FULL
@Preview(widthDp = 200, heightDp = 280) // FULL
@Composable
fun SmallWidgetChallengeCompletedTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.ChallengeCompleted,
        enrollmentDate = LocalDate.now()
    )
}

// StreakOngoingReadToday: mascot + badge
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130)
@Preview(widthDp = 130, heightDp = 200)
@Preview(widthDp = 184, heightDp = 160)
@Preview(widthDp = 200, heightDp = 200)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetStreakOngoingReadTodayTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.StreakOngoingReadToday(streak = 15),
        enrollmentDate = LocalDate.now()
    )
}

// StreakOngoingNeedsReading: mascot + badge
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130)
@Preview(widthDp = 130, heightDp = 200)
@Preview(widthDp = 184, heightDp = 160)
@Preview(widthDp = 200, heightDp = 200)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetStreakOngoingNeedsReadingTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.StreakOngoingNeedsReading(streak = 2),
        enrollmentDate = LocalDate.now()
    )
}

// ChallengeConcludedIncomplete: button-with-icon (longest button text)
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130)
@Preview(widthDp = 130, heightDp = 200)
@Preview(widthDp = 184, heightDp = 160)
@Preview(widthDp = 200, heightDp = 200)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetChallengeIncompleteTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedIncomplete(streak = 12),
        enrollmentDate = LocalDate.now()
    )
}

// NotEnrolled: button + mascot, simpler layout but worth checking
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130)
@Preview(widthDp = 130, heightDp = 200)
@Preview(widthDp = 184, heightDp = 160)
@Preview(widthDp = 200, heightDp = 200)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetNotEnrolledTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}

// EnrolledNotStarted: button + mascot, similar density to NotEnrolled
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 184, heightDp = 130)
@Preview(widthDp = 130, heightDp = 200)
@Preview(widthDp = 184, heightDp = 160)
@Preview(widthDp = 200, heightDp = 200)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SmallWidgetEnrolledNotStartedTierBoundariesPreview() {
    ReadingChallengeSmallWidgetContent(
        state = ReadingChallengeState.EnrolledNotStarted,
        enrollmentDate = LocalDate.now()
    )
}
