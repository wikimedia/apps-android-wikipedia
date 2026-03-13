package org.wikipedia.widgets.readingchallenge

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.widgets.utils.FontUtils

@Composable
fun ReadingChallengeLargeWidgetContent(
    state: ReadingChallengeState
) {
    // each state will have small and large widget content
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
        ReadingChallengeState.NotEnrolled -> TODO()
        ReadingChallengeState.NotLiveYet -> {}
        is ReadingChallengeState.StreakOngoingNeedsReading -> {
            StreakOngoingNeedsReadingLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
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
                    .clickable(onClick = actionStartActivity<MainActivity>()),
                backgroundColor = WidgetColors.normalReadingBackground,
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
    contentColor: Color = WidgetColors.readingContent,
    progressColor: Color = WidgetColors.progressColor
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
                        contentDescription = "Trophy",
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(day = contentColor, night = contentColor))
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Text(
                        text = context.getString(R.string.reading_challenge_streak_ongoing_title, ReadingChallengeWidgetRepository.READING_STREAK_GOAL),
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
                    startIconResId = R.drawable.baseline_event_repeat_24,
                    endIconResId = R.drawable.baseline_event_repeat_24,
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
                    contentDescription = "Mascot",
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
    backgroundColor: androidx.compose.ui.graphics.Color,
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
                    text = "Search",
                    iconResId = R.drawable.outline_search_24,
                    action = actionRunCallback<SearchAction>()
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                WidgetIconButton(
                    modifier = GlanceModifier
                        .defaultWeight(),
                    text = "Random",
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

    val contentSize = ReadingWidgetDimensions.contentSize

    // (widget width minus image column ~110dp, padding, spacing)
    val textColumnWidth = contentSize.width - 110.dp - ReadingWidgetDimensions.contentSpacing

    val (titleFontSize, _) = FontUtils.calculateFontSizeAndMaxLines(
        context = context,
        text = title,
        availableWidth = textColumnWidth,
        availableHeight = contentSize.height * 0.40f,
        maxFontSize = 32.sp,
        minFontSize = 18.sp,
    )

    val (subtitleFontSize, _) = FontUtils.calculateFontSizeAndMaxLines(
        context = context,
        text = subtitle,
        availableWidth = textColumnWidth,
        availableHeight = contentSize.height * 0.35f,
        maxFontSize = 16.sp,
        minFontSize = 12.sp,
    )

    GeneralLargeWidget(
        textColor = textColor,
        backgroundColor = backgroundColor,
        titleBarIcon = titleBarIcon,
        title = title,
        titleFontSize = titleFontSize,
        subTitle = subtitle,
        subTitleFontSize = subtitleFontSize,
        mainImageResId = mainImageResId,
        bottomContent = {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetIconButton(
                    modifier = GlanceModifier.defaultWeight(),
                    text = "Search",
                    iconResId = R.drawable.outline_search_24,
                    action = actionRunCallback<SearchAction>()
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                WidgetIconButton(
                    modifier = GlanceModifier.defaultWeight(),
                    text = "Random",
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
    titleFontSize: TextUnit,
    subTitle: String,
    subTitleFontSize: TextUnit,
    mainImageResId: Int,
    bottomContent: @Composable () -> Unit = { }
) {
    BaseWidgetContent(
        color = backgroundColor
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    provider = ImageProvider(titleBarIcon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(mainImageResId),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(120.dp)
                    )
                }
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(end = 100.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontSize = titleFontSize,
                                color = ColorProvider(day = textColor, night = textColor),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = subTitle,
                            style = TextStyle(
                                fontSize = subTitleFontSize,
                                color = ColorProvider(day = textColor, night = textColor),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                bottomContent()
            }
        }
    }
}

class SearchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = SearchActivity.newIntent(context, InvokeSource.WIDGET, null).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class RandomizerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = RandomActivity.newIntent(context, WikipediaApp.instance.wikiSite, InvokeSource.WIDGET).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private object ReadingWidgetDimensions {
    val widgetPadding = 16.dp
    val contentSpacing = 12.dp

    val contentSize: DpSize
        @Composable get() {
            val size = LocalSize.current
            return DpSize(
                width = size.width - (2f * widgetPadding),
                // subtract button row height (~48dp) and spacing
                height = size.height - widgetPadding - 48.dp - contentSpacing
            )
        }
}
