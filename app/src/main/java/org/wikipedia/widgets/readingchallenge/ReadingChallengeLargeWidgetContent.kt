package org.wikipedia.widgets.readingchallenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.widgets.readingchallenge.WidgetCombinations.forToday
import java.time.LocalDate

@Composable
fun ReadingChallengeLargeWidgetContent(
    state: ReadingChallengeState,
    enrollmentDate: LocalDate
) {
    val context = LocalContext.current

    when (state) {
        ReadingChallengeState.ChallengeCompleted -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final_large,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_concluded_complete),
                subTitleContent = {
                    WidgetBadge(
                        text = streakText,
                        textSize = 16.sp,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 24.dp,
                        iconTintColor = WidgetColors.primary,
                        textColor = WidgetColors.primary
                    )
                },
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_collect_your_prize_button),
                        action = actionRunCallback<ChallengeRewardAction>(),
                        modifier = GlanceModifier
                    )
                }
            )
        }
        is ReadingChallengeState.ChallengeConcludedIncomplete -> {
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final_large,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, state.streak, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_concluded_incomplete),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = streakText,
                        action = actionStartActivity(MainActivity.newIntent(context)),
                        backgroundColor = WidgetColors.challengeConcludedIncompleteButtonBackground,
                        contentColor = WidgetColors.primary,
                        icon = ImageProvider(R.drawable.ic_flame_24dp),
                        modifier = GlanceModifier
                    )
                }
            )
        }
        ReadingChallengeState.ChallengeConcludedNoStreak, ReadingChallengeState.ChallengeRemoved -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_concluded_incomplete),
                mainImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
        ReadingChallengeState.EnrolledNotStarted -> {
            val combination = WidgetCombinations.enrolledNotStarted.forToday(enrollmentDate = enrollmentDate)
            EnrolledNotStartedLargeWidget(
                mainImageResId = R.drawable.globe,
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                titleResId = combination.titleResId ?: R.string.reading_challenge_widget_enrolled_not_started_title,
                subtitleReId = combination.subtitleResId ?: R.string.reading_challenge_widget_enrolled_not_started_subtitle
            )
        }
        ReadingChallengeState.NotEnrolled -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_not_opted_in_title),
                subTitle = context.getString(R.string.reading_challenge_widget_not_opted_in_description),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_join_the_challenge_button),
                        action = actionRunCallback<JoinChallengeAction>()
                    )
                }
            )
        }
        ReadingChallengeState.NotLiveYet -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.challengeNotLiveBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_not_live_title),
                subTitle = context.getString(R.string.reading_challenge_widget_not_live_description),
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            val combination = WidgetCombinations.streakNeedsReading.forToday(enrollmentDate = enrollmentDate)
            StreakOngoingNeedsReadingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                reminderTextResId = combination.titleResId ?: R.string.reading_challenge_widget_reminder_dont_let_today_drift,
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                state = state,
                mascotImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            val combination = WidgetCombinations.streakOngoing.forToday(enrollmentDate = enrollmentDate)
            StreakOngoingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                progressColor = combination.progressColor ?: WidgetColors.phoneReadingProgressColor,
                state = state,
                mascotImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
    }
}

@Composable
fun StreakOngoingLargeWidget(
    state: ReadingChallengeState.StreakOngoingReadToday,
    mascotImageResId: Int,
    modifier: GlanceModifier = GlanceModifier,
    backgroundColor: Color,
    contentColor: Color,
    progressColor: Color,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    BaseWidgetContent(
        color = backgroundColor
    ) {
        Box(
            modifier = modifier
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Top Row: Trophy, Title, W logo
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_trophy24dp),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(day = contentColor, night = contentColor))
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Text(
                        text = context.getString(R.string.reading_challenge_streak_ongoing_title),
                        style = TextStyle(
                            color = ColorProvider(day = contentColor, night = contentColor),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // W logo (Placeholder)
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp)
                    )
                }

                // Middle Row: Flame, Days. (With defaultWeight, it dynamically pushes the rows apart to match the design spacing perfectly)
                WidgetBadge(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = streakText,
                    iconResId = R.drawable.ic_flame_24dp,
                    iconSize = 45.dp,
                    iconTintColor = contentColor,
                    textColor = contentColor
                )

                // Bottom Row: Progress bar
                BoxedStreakProgressBar(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    currentStreak = state.streak,
                    totalDays = ReadingChallengeWidgetRepository.READING_STREAK_GOAL,
                    startIconResId = R.drawable.ic_calendar_day_1,
                    endIconResId = R.drawable.ic_calendar_day_25,
                    backgroundColor = contentColor,
                    progressColor = progressColor,
                    progressBarColor = WidgetColors.white
                )
            }

            // Mascot overlay positioned absolutely inside the Box bounds
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    provider = ImageProvider(mascotImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(120.dp)
                        .padding(end = 24.dp, bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun StreakOngoingNeedsReadingLargeWidget(
    state: ReadingChallengeState.StreakOngoingNeedsReading,
    titleBarIcon: Int = R.drawable.ic_w_logo_shadow,
    reminderTextResId: Int,
    backgroundColor: Color,
    mascotImageResId: Int,
    modifier: GlanceModifier = GlanceModifier,
    contentColor: Color
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    val reminderText = context.getString(reminderTextResId)

    val size = LargeWidgetSize.from(LocalSize.current)
    val adjustedTitleFontSize = if (size == LargeWidgetSize.COMPACT) 14.sp else 16.sp
    val mascotSize = if (size == LargeWidgetSize.COMPACT) 80.dp else 110.dp

    BaseWidgetContent(
        color = backgroundColor
    ) {
        Column (
            modifier = modifier
        ) {
            Row (
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                ) {
                    WidgetBadge(
                        text = streakText,
                        iconResId = R.drawable.ic_flame_24dp,
                        iconSize = 40.dp,
                        iconTintColor = contentColor,
                        textColor = contentColor
                    )
                    Text(
                        text = reminderText,
                        style = TextStyle(
                            fontSize = adjustedTitleFontSize,
                            color = ColorProvider(day = contentColor, night = contentColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(mascotImageResId),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(mascotSize)
                    )
                    Spacer(modifier = GlanceModifier.size(24.dp))
                }
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                WidgetIconButton(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = context.getString(R.string.reading_challenge_widget_search_button),
                    iconResId = R.drawable.outline_search_24,
                    action = actionRunCallback<SearchAction>()
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                WidgetIconButton(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = context.getString(R.string.reading_challenge_widget_random_button),
                    iconResId = R.drawable.ic_dice_24,
                    action = actionRunCallback<RandomizerAction>()
                )
            }
        }
    }
}

@Composable
fun EnrolledNotStartedLargeWidget(
    titleBarIcon: Int = R.drawable.ic_w_logo_shadow,
    mainImageResId: Int,
    backgroundColor: Color,
    contentColor: Color,
    titleResId: Int,
    subtitleReId: Int
) {
    val context = LocalContext.current

    val title = context.getString(titleResId)
    val subtitle = context.getString(subtitleReId)

    GeneralLargeWidget(
        textColor = contentColor,
        backgroundColor = backgroundColor,
        titleBarIcon = titleBarIcon,
        title = title,
        subTitle = subtitle,
        mainImageResId = mainImageResId,
        bottomContent = {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetIconButton(
                    modifier = GlanceModifier.defaultWeight(),
                    text = context.getString(R.string.reading_challenge_widget_search_button),
                    iconResId = R.drawable.outline_search_24,
                    action = actionRunCallback<SearchAction>()
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                WidgetIconButton(
                    modifier = GlanceModifier.defaultWeight(),
                    text = context.getString(R.string.reading_challenge_widget_random_button),
                    iconResId = R.drawable.ic_dice_24,
                    action = actionRunCallback<RandomizerAction>()
                )
            }
        }
    )
}

@Composable
fun GeneralLargeWidget(
    modifier: GlanceModifier = GlanceModifier,
    textColor: Color,
    backgroundColor: Color,
    titleBarIcon: Int = R.drawable.ic_w_logo_shadow,
    title: String,
    subTitle: String? = null,
    subTitleContent: @Composable () -> Unit = { },
    mainImageResId: Int,
    bottomContent: @Composable () -> Unit = { }
) {
    val size = LargeWidgetSize.from(LocalSize.current)
    val adjustedTitleFontSize = if (size == LargeWidgetSize.COMPACT) 24.sp else 32.sp
    val adjustedSubTitleFontSize = if (size == LargeWidgetSize.COMPACT) 14.sp else 16.sp
    val mascotSize = if (size == LargeWidgetSize.COMPACT) 80.dp else 110.dp

    BaseWidgetContent(
        color = backgroundColor
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = adjustedTitleFontSize,
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                    subTitleContent()
                    subTitle?.let {
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = adjustedSubTitleFontSize,
                                color = ColorProvider(day = textColor, night = textColor),
                                fontWeight = FontWeight.Medium,
                            )
                        )
                    }
                }
                Column(
                    modifier = GlanceModifier
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(mainImageResId),
                        contentDescription = null,
                        modifier = GlanceModifier.size(mascotSize)
                    )
                    Spacer(modifier = GlanceModifier.size(24.dp))
                }
            }

            bottomContent()
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LargeWidgetEnrolledNotStartedPreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.EnrolledNotStarted,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LargeWidgetCompletedPreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeCompleted,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LargeWidgetConcludedIncompletePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedIncomplete(5),
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LargeWidgetConcludedNoStreakPreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedNoStreak,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LargeWidgetNotEnrolledPreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 176)
@Composable
fun LargeWidgetNotEnrolledCompactSizePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 176)
@Composable
fun LargeWidgetEnrolledNotStartedCompactSizePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.EnrolledNotStarted,
        enrollmentDate = LocalDate.now()
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 176)
@Composable
fun LargeWidgetOngoingNeedsReadingCompactSizePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.StreakOngoingNeedsReading(
            streak = 2
        ),
        enrollmentDate = LocalDate.now()
    )
}
