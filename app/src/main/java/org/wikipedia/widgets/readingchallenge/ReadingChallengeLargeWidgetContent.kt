package org.wikipedia.widgets.readingchallenge

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.main.MainActivity

@Composable
fun ReadingChallengeLargeWidgetContent(
    state: ReadingChallengeState
) {
    val context = LocalContext.current
    val textColor = ComposeColors.Gray700

    when (state) {
        ReadingChallengeState.ChallengeCompleted -> TODO()
        ReadingChallengeState.ChallengeConcludedIncomplete -> TODO()
        ReadingChallengeState.ChallengeConcludedNoStreak -> TODO()
        ReadingChallengeState.ChallengeRemoved -> TODO()
        ReadingChallengeState.EnrolledNotStarted -> {
            EnrolledNotStartedLargeWidget(
                mainImageResId = R.drawable.globe,
                backgroundColor = WidgetColors.challengeNotOptInBackground
            )
        }
        ReadingChallengeState.NotEnrolled -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                textColor = textColor,
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
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                textColor = textColor,
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
            StreakOngoingNeedsReadingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = WidgetColors.streakOngoingNotReadBackground,
                state = state,
                mascotImageResId = R.drawable.globe // TODO: update when svg's are provided
            )
        }
        is ReadingChallengeState.StreakOngoingReadToday -> {
            StreakOngoingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(onClick = actionStartActivity(MainActivity.newIntent(context))),
                backgroundColor = WidgetColors.phoneReadingBackground,
                contentColor = WidgetColors.phoneReadingContent,
                progressColor = WidgetColors.phoneReadingProgressColor,
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
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    contentColor: Color,
    progressColor: Color
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
                        modifier = GlanceModifier.size(24.dp)
                    )
                }

                // Middle Row: Flame, Days. (With defaultWeight, it dynamically pushes the rows apart to match the design spacing perfectly)
                WidgetBadge(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = streakText,
                    iconResId = R.drawable.ic_flame_24dp,
                    iconSize = 45.dp,
                    iconTintColorProvider = contentColor,
                    textColorProvider = contentColor
                )

                // Bottom Row: Progress bar
                BoxedStreakProgressBar(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(contentColor)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    currentStreak = state.streak,
                    totalDays = ReadingChallengeWidgetRepository.READING_STREAK_GOAL,
                    startIconResId = R.drawable.ic_calendar_day_1,
                    endIconResId = R.drawable.ic_calendar_day_25,
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
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    backgroundColor: Color,
    mascotImageResId: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    val streakTextColor = WidgetColors.streakOngoingNotReadContent
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
                        iconTintColorProvider = WidgetColors.streakOngoingNotReadContent,
                        textColorProvider = streakTextColor
                    )
                    Text(
                        text = context.getString(R.string.reading_challenge_not_read_today_description),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = ColorProvider(day = streakTextColor, night = streakTextColor),
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
                        modifier = GlanceModifier.size(24.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(mascotImageResId),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(110.dp)
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
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    mainImageResId: Int,
    backgroundColor: Color
) {
    val context = LocalContext.current
    val textColor = WidgetColors.primary
    val title = context.getString(R.string.reading_challenge_widget_enrolled_not_started_title)
    val subtitle = context.getString(R.string.reading_challenge_widget_enrolled_not_started_subtitle)

    GeneralLargeWidget(
        textColor = textColor,
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
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    title: String,
    titleFontSize: TextUnit = 32.sp,
    subTitle: String,
    subTitleFontSize: TextUnit = 16.sp,
    mainImageResId: Int,
    bottomContent: @Composable () -> Unit = { }
) {
    val availableHeight = LocalSize.current.height

    val isCompactHeight = availableHeight < 250.dp
    val adjustedTitleFontSize = if (isCompactHeight) 24.sp else titleFontSize
    val adjustedSubTitleFontSize = if (isCompactHeight) 14.sp else subTitleFontSize

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
                    Text(
                        text = subTitle,
                        style = TextStyle(
                            fontSize = adjustedSubTitleFontSize,
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }
                Column(
                    modifier = GlanceModifier
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        provider = ImageProvider(titleBarIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(mainImageResId),
                        contentDescription = null,
                        modifier = GlanceModifier.size(110.dp)
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
fun GeneralWidgetPreview() {
    GeneralLargeWidget(
        backgroundColor = WidgetColors.challengeNotOptInBackground,
        textColor = ComposeColors.Gray700,
        title = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_title),
        titleFontSize = 34.sp,
        subTitle = LocalContext.current.getString(R.string.reading_challenge_widget_not_live_description),
        subTitleFontSize = 16.sp,
        mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
        bottomContent = {
            WidgetButton(
                text = LocalContext.current.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                action = actionStartActivity(Intent())
            )
        }
    )
}
