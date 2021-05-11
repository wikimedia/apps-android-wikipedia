package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ListView
import kotlin.math.abs

class SwipeableListView constructor(context: Context, attrs: AttributeSet? = null) : ListView(context, attrs) {

    fun interface OnSwipeOutListener {
        fun onSwipeOut()
    }

    var listener: OnSwipeOutListener? = null
    var rtl: Boolean = false

    init {
        // use GestureDetector to take over the onTouchEvent
        val gestureDetector = GestureDetector(context, ViewGestureListener())
        setOnTouchListener { _, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }
    }

    private fun swipeDetected(event1: MotionEvent, event2: MotionEvent): Boolean {
        return if (rtl) {
            event1.y - event2.y <= SWIPE_MAX_DISTANCE && event1.x - event2.x > SWIPE_MIN_DISTANCE
        } else {
            event2.y - event1.y <= SWIPE_MAX_DISTANCE && event2.x - event1.x > SWIPE_MIN_DISTANCE
        }
    }

    private inner class ViewGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(event1: MotionEvent, event2: MotionEvent,
                             velocityX: Float, velocityY: Float): Boolean {
            if (swipeDetected(event1, event2) && abs(velocityX) > SWIPE_MIN_X_VELOCITY &&
                    abs(velocityY) < SWIPE_MAX_Y_VELOCITY) {
                listener?.onSwipeOut()
            }
            return false
        }
    }

    companion object {
        const val SWIPE_MIN_DISTANCE = 200
        const val SWIPE_MAX_DISTANCE = 300
        const val SWIPE_MIN_X_VELOCITY = 100
        const val SWIPE_MAX_Y_VELOCITY = 2000
    }
}
