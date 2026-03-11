package org.wikipedia.widgets.readingchallenge.smallwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetWorker

class ReadingChallengeSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = ReadingChallengeSmallWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ReadingChallengeWidgetWorker.scheduleNextMidnightUpdate(context)
    }
}
