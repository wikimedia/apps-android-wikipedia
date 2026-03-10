package org.wikipedia.widgets.readingchallenge.largewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetWorker

class ReadingChallengeLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = ReadingChallengeLargeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ReadingChallengeWidgetWorker.scheduleNextMidnightUpdate(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ReadingChallengeWidgetWorker.scheduleNextMidnightUpdate(context)
    }
}
