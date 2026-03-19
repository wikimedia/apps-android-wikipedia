package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import org.wikipedia.settings.Prefs

class ReadingChallengeWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = ReadingChallengeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // If the user has already added the widget, we should not display the install dialog
        Prefs.readingChallengeInstallPromptShown = true
        ReadingChallengeWidgetWorker.scheduleNextMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Prefs.readingChallengeInstallPromptShown = false
        ReadingChallengeWidgetWorker.cancelScheduledUpdates(context)
    }
}
