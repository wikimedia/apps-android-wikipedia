package org.wikipedia.views

import android.view.GestureDetector
import android.view.MotionEvent
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.onboarding.InitialOnboardingActivity

class ActivityGestureListener(private val activity: BaseActivity) :
    GestureDetector.SimpleOnGestureListener() {

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
        return
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?,
                          e2: MotionEvent?,
                          distanceX: Float,
                          distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        return
    }

    override fun onFling(event1: MotionEvent,
                         event2: MotionEvent,
                         velocityX: Float,
                         velocityY: Float): Boolean {
        if (activity is InitialOnboardingActivity) {
            try {
                val diffY = event1.y - event2.y
                val diffX = event1.x - event2.x
                if (diffY <= SwipeableListView.SWIPE_MAX_DISTANCE && diffX > SwipeableListView.SWIPE_MIN_DISTANCE) {
                    BreadCrumbLogEvent.logSwipe(activity, false)
                } else {
                    BreadCrumbLogEvent.logSwipe(activity, true)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return true
    }
}
