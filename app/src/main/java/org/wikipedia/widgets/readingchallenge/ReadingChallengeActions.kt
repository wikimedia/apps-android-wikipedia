package org.wikipedia.widgets.readingchallenge

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.settings.Prefs

class JoinChallengeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Prefs.readingChallengeOnboardingShown = false
        context.startActivity(
            MainActivity.newIntent(context)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            SearchActivity.newIntent(context, InvokeSource.WIDGET, null).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
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
            RandomActivity.newIntent(context, WikipediaApp.instance.wikiSite, InvokeSource.WIDGET).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}
