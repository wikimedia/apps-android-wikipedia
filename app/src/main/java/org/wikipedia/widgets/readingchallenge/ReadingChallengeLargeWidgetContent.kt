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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.widgets.readingchallenge.WidgetCombinations.forToday
import java.time.LocalDate

@Composable
fun ReadingChallengeLargeWidgetContent(
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
            val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak_final_large,
                ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL, ReadingChallengeWidgetRepository.READING_STREAK_GOAL)
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(onClick = actionRunCallback<ChallengeRewardAction>()),
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
                expandMascot = true,
                mainImageResId = R.drawable.wp25_babyglobe_celebration_neutral,
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
                    .fillMaxSize()
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_concluded_incomplete),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
                expandMascot = true,
                bottomContent = {
                    WidgetButton(
                        text = streakText,
                        action = actionRunCallback<HomeAction>(),
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
                    .fillMaxSize()
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_concluded_incomplete),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
                expandMascot = true
            )
        }
        ReadingChallengeState.EnrolledNotStarted -> {
            val combination = WidgetCombinations.enrolledNotStarted.forToday(enrollmentDate = enrollmentDate)
            EnrolledNotStartedLargeWidget(
                mainImageResId = combination.iconResId,
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                titleResId = combination.titleResId ?: R.string.reading_challenge_widget_enrolled_not_started_title,
                subtitleReId = combination.subtitleResId ?: R.string.reading_challenge_widget_enrolled_not_started_subtitle
            )
        }
        ReadingChallengeState.NotEnrolled -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.joinChallengeBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_not_opted_in_title),
                subTitle = context.getString(R.string.reading_challenge_widget_not_opted_in_description),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
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
                    .fillMaxSize()
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = WidgetColors.challengeNotLiveBackground,
                textColor = WidgetColors.primary,
                title = context.getString(R.string.reading_challenge_widget_not_live_title),
                subTitle = context.getString(R.string.reading_challenge_widget_not_live_description),
                mainImageResId = R.drawable.wp25_babyglobe_reading,
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                        action = actionRunCallback<HomeAction>()
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
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                reminderTextResId = combination.titleResId ?: R.string.reading_challenge_widget_reminder_dont_let_today_drift,
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                state = state,
                mascotImageResId = combination.iconResId
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            val combination = WidgetCombinations.streakOngoing.forToday(enrollmentDate = enrollmentDate)
            StreakOngoingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(onClick = actionRunCallback<HomeAction>()),
                backgroundColor = combination.backgroundColor,
                contentColor = combination.contentColor,
                progressColor = combination.progressColor ?: WidgetColors.phoneReadingProgressColor,
                state = state,
                mascotImageResId = combination.iconResId
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
    titleBarIcon: Int = R.drawable.ic_w_logo_shadow
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    val size = LargeWidgetSize.from(LocalSize.current)

    BaseWidgetContent(
        color = backgroundColor
    ) {
        Box(
            modifier = modifier
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Top Row: Trophy, Title, W logo
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_trophy24dp),
                        contentDescription = null,
                        modifier = GlanceModifier.size(size.trophyIconSize),
                        colorFilter = ColorFilter.tint(ColorProvider(day = contentColor, night = contentColor))
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Text(
                        text = context.getString(R.string.reading_challenge_streak_ongoing_title),
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = ColorProvider(day = contentColor, night = contentColor),
                            fontSize = size.titleBarTextSize,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    // W logo (Placeholder)
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(size.titleBarIconSize)
                    )
                }

                // Middle Row: Flame, Days. (With defaultWeight, it dynamically pushes the rows apart to match the design spacing perfectly)
                WidgetBadge(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = streakText,
                    iconResId = R.drawable.ic_flame_24dp,
                    textSize = size.streakBadgeTextSize,
                    iconSize = size.streakBadgeIconSize,
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
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(end = 24.dp, bottom = 32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    provider = ImageProvider(mascotImageResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(size.overlayMascotSize)
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

    val widgetDimension = LocalSize.current
    val size = LargeWidgetSize.from(widgetDimension)
    val availableWidth = widgetDimension.width - 32.dp
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
                        .width(availableWidth * 0.7f)
                ) {
                    WidgetBadge(
                        text = streakText,
                        textSize = size.streakBadgeTextSize,
                        iconResId = R.drawable.ic_streak_warning,
                        iconSize = size.streakBadgeIconSize,
                        textColor = contentColor
                    )
                    Text(
                        text = reminderText,
                        style = TextStyle(
                            fontSize = size.subtitleTextSize,
                            color = ColorProvider(day = contentColor, night = contentColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }

                Column(
                    modifier = GlanceModifier
                        .width(availableWidth * 0.3f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(size.titleBarIconSize)
                    )
                    val mascotSize = minOf(size.sideMascotSize, availableWidth * 0.3f, widgetDimension.height - size.titleBarIconSize)
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(mascotImageResId),
                            contentDescription = null,
                            modifier = GlanceModifier
                                .size(mascotSize)
                        )
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
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
        modifier = GlanceModifier
            .clickable(onClick = actionRunCallback<HomeAction>()),
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
    mainImageResId: Int,
    expandMascot: Boolean = false,
    subTitleContent: @Composable () -> Unit = { },
    bottomContent: @Composable () -> Unit = { }
) {
    val widgetDimension = LocalSize.current
    val size = LargeWidgetSize.from(widgetDimension)
    val availableWidth = widgetDimension.width - 32.dp

    BaseWidgetContent(
        color = backgroundColor
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top row: text column (70%) + mascot/logo column (30%)
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
            ) {
                // Left column: title, optional subtitle content, optional subtitle
                Column(
                    modifier = GlanceModifier
                        .width(availableWidth * 0.7f)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = size.titleTextSize,
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                    subTitleContent()
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    subTitle?.let {
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = size.subtitleTextSize,
                                color = ColorProvider(day = textColor, night = textColor),
                                fontWeight = FontWeight.Medium,
                            )
                        )
                    }
                }

                // Right column: W logo pinned top-right, mascot centered below
                Column(
                    modifier = GlanceModifier
                        .width(availableWidth * 0.3f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(size.titleBarIconSize)
                    )
                    val mascotSize = if (expandMascot) {
                        minOf(size.expandedMascotSize, availableWidth * 0.3f, widgetDimension.height - size.titleBarIconSize)
                    } else {
                        minOf(size.sideMascotSize, availableWidth * 0.3f, widgetDimension.height - size.titleBarIconSize)
                    }
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(mainImageResId),
                            contentDescription = null,
                            modifier = GlanceModifier.size(mascotSize)
                        )
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            // Bottom row: optional CTA / additional content (e.g. "Join the challenge" button)
            bottomContent()
        }
    }
}

 // Loading state
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 224)
@Composable
fun LoadingFullPreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.Loading,
        enrollmentDate = LocalDate.now()
    )
}

// NotEnrolled: button + mascot + title + subtitle
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun NotEnrolledLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.NotEnrolled,
        enrollmentDate = LocalDate.now()
    )
}

// NotLiveYet: button + mascot + title + subtitle
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun NotLiveYetLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.NotLiveYet,
        enrollmentDate = LocalDate.now()
    )
}

// EnrolledNotStarted: two icon buttons + mascot + title + subtitle
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun EnrolledNotStartedLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.EnrolledNotStarted,
        enrollmentDate = LocalDate.now()
    )
}

// StreakOngoingReadToday: title row + badge + progress bar + overlay mascot
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 234) // FULL
@Composable
fun StreakOngoingReadTodayLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.StreakOngoingReadToday(streak = 15),
        enrollmentDate = LocalDate.now()
    )
}

// StreakOngoingNeedsReading: badge + reminder + side mascot + two buttons
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 368, heightDp = 123)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // COMPACT
@Preview(widthDp = 368, heightDp = 234) // FULL
@Composable
fun StreakOngoingNeedsReadingLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.StreakOngoingNeedsReading(streak = 7),
        enrollmentDate = LocalDate.now()
    )
}

// ChallengeCompleted: title + badge + button + expanded mascot
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun ChallengeCompletedLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeCompleted,
        enrollmentDate = LocalDate.now()
    )
}

// ChallengeConcludedIncomplete: button-with-icon + expanded mascot
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun ChallengeConcludedIncompleteLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedIncomplete(streak = 12),
        enrollmentDate = LocalDate.now()
    )
}

// ChallengeConcludedIncomplete: button-with-icon + expanded mascot
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 130) // launcher placed below declared minHeight (rare)
@Preview(widthDp = 320, heightDp = 156) // EXTRA_COMPACT worst-case
@Preview(widthDp = 330, heightDp = 176) // EXTRA_COMPACT worst-case
@Preview(widthDp = 340, heightDp = 200) // COMPACT
@Preview(widthDp = 368, heightDp = 184) // COMPACT, wider
@Preview(widthDp = 368, heightDp = 224) // FULL
@Composable
fun ChallengeConcludedNoStreakLargePreview() {
    ReadingChallengeLargeWidgetContent(
        state = ReadingChallengeState.ChallengeConcludedNoStreak,
        enrollmentDate = LocalDate.now()
    )
}
