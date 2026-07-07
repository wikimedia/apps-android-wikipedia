package org.wikipedia.widgets.readingchallenge

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.random.RandomActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.WidgetTypes

class JoinChallengeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Prefs.readingChallengeOnboardingShown = false
        context.startActivity(
            MainActivity.newIntent(context)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.WIDGET)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.READING_CHALLENGE.value)
                .putExtra(ReadingChallengeWidgetRepository.INTENT_EXTRA_READING_CHALLENGE_JOIN, true)
        )
    }
}

class SearchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            SearchActivity.newIntent(context, InvokeSource.WIDGET, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.READING_CHALLENGE.value)
        )
    }
}

class RandomizerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            RandomActivity.newIntent(context, WikipediaApp.instance.wikiSite, InvokeSource.WIDGET)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.READING_CHALLENGE.value)
        )
    }
}

// Separate from RandomizerAction so this can report the Random Article widget type.
// Avoids extra Glance action parameter plumbing and keeps the future standalone Random Article widget transition simpler.
class RandomArticleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            RandomActivity.newIntent(context, WikipediaApp.instance.wikiSite, InvokeSource.WIDGET)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.RANDOM_ARTICLE.value)
        )
    }
}

class ChallengeRewardAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            MainActivity.newIntent(context)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.WIDGET)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.READING_CHALLENGE.value)
                .putExtra(ReadingChallengeWidgetRepository.INTENT_EXTRA_READING_CHALLENGE_REWARD, true)
        )
    }
}

class HomeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            MainActivity.newIntent(context)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.WIDGET)
                .putExtra(Constants.INTENT_WIDGET_TYPE, WidgetTypes.READING_CHALLENGE.value)
        )
    }
}
