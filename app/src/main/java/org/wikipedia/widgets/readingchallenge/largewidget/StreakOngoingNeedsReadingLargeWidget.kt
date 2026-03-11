package org.wikipedia.widgets.readingchallenge.largewidget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.random.RandomActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.WidgetBadge
import org.wikipedia.widgets.readingchallenge.WidgetColors
import org.wikipedia.widgets.readingchallenge.WidgetIconButton

@Composable
fun StreakOngoingNeedsReadingLargeWidget(
    state: ReadingChallengeState.StreakOngoingNeedsReading,
    titleBarIcon: Int = R.drawable.ic_wikipedia_w,
    mainImageResId: Int,
) {
    val context = LocalContext.current
    val streakText = context.resources.getQuantityString(R.plurals.reading_challenge_small_widget_streak, state.streak, state.streak)
    val streakTextColor = WidgetColors.streakOngoingNotReadContent
    Column (
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(WidgetColors.streakOngoingNotReadBackground)
            .padding(16.dp),
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
                    provider = ImageProvider(mainImageResId),
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
