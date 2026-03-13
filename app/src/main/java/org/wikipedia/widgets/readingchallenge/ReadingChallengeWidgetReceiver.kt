package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ReadingChallengeWidgetReceiver: GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = ReadingChallengeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ReadingChallengeWidgetWorker.scheduleNextMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        ReadingChallengeWidgetWorker.cancelScheduledUpdates(context)
    }
}