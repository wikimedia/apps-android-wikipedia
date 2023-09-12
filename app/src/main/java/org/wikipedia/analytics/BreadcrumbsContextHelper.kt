package org.wikipedia.analytics

import android.graphics.Point
import android.view.*
import android.widget.TextView
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.metricsplatform.BreadcrumbLogEvent
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.views.ViewUtil
import kotlin.math.abs

object BreadcrumbsContextHelper {
    private var startTouchX = 0f
    private var startTouchY = 0f
    private var startTouchMillis = 0L
    private var touchSlopPx = 0

    fun dispatchTouchEvent(window: Window, event: MotionEvent) {
        if (touchSlopPx <= 0) {
            touchSlopPx = ViewConfiguration.get(window.context).scaledTouchSlop
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchMillis = System.currentTimeMillis()
                startTouchX = event.x
                startTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val touchMillis = System.currentTimeMillis() - startTouchMillis
                val dx = abs(startTouchX - event.x)
                val dy = abs(startTouchY - event.y)

                if (dx <= touchSlopPx && dy <= touchSlopPx) {
                    val point = Point(startTouchX.toInt(), startTouchY.toInt())
                    ViewUtil.findClickableViewAtPoint(window.decorView, point)?.let {
                        if (it is TextView && it.movementMethod is LinkMovementMethodExt) {
                            // If they clicked a link in a TextView, it will be handled by the
                            // MovementMethod instead of here.
                        } else {
                            if (touchMillis > ViewConfiguration.getLongPressTimeout()) {
                                BreadCrumbLogEvent.logLongClick(window.context, it)
                                BreadcrumbLogEvent().logLongClick(window.context, it)
                            } else {
                                BreadCrumbLogEvent.logClick(window.context, it)
                                BreadcrumbLogEvent().logClick(window.context, it)
                            }
                        }
                    }
                }
            }
        }
    }
}
