package org.wikipedia.widgets.readingchallenge

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
        ReadingChallengeState.EnrolledNotStarted -> TODO()
        ReadingChallengeState.NotEnrolled -> {
            GeneralLargeWidget(
                modifier = GlanceModifier
                    .fillMaxSize(),
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                textColor = textColor,
                title = context.getString(R.string.reading_challenge_widget_not_opted_in_title),
                titleFontSize = 32.sp,
                subTitle = context.getString(R.string.reading_challenge_widget_not_opted_in_description),
                subTitleFontSize = 16.sp,
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
                    .fillMaxSize(),
                backgroundColor = WidgetColors.challengeNotOptInBackground,
                textColor = textColor,
                title = context.getString(R.string.reading_challenge_widget_not_live_title),
                titleFontSize = 32.sp,
                subTitle = context.getString(R.string.reading_challenge_widget_not_live_description),
                subTitleFontSize = 16.sp,
                mainImageResId = R.drawable.globe, // TODO: update when svg's are provided
                bottomContent = {
                    WidgetButton(
                        text = context.getString(R.string.reading_challenge_widget_explore_wikipedia_button),
                        action = actionStartActivity(MainActivity.newIntent(context))
                    )
                }
            )
        }
        is ReadingChallengeState.StreakOngoingNeedsReading -> {}
        is ReadingChallengeState.StreakOngoingReadToday -> {}
    }
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
                        maxLines = 2,
                        style = TextStyle(
                            fontSize = titleFontSize,
                            color = ColorProvider(day = textColor, night = textColor),
                            fontWeight = FontWeight.Medium,
                        )
                    )
                    Text(
                        text = subTitle,
                        maxLines = 2,
                        style = TextStyle(
                            fontSize = subTitleFontSize,
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
