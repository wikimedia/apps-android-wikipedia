package org.wikipedia.views

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import kotlin.math.abs

class DontInterceptTouchListener : OnItemTouchListener {

    private var pointerId = Int.MIN_VALUE
    private var x = Float.MIN_VALUE
    private var y = Float.MIN_VALUE
    private var disallowInterception = false

    override fun onInterceptTouchEvent(view: RecyclerView, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                x = event.x
                y = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (disallowInterception) {
                    return false
                }

                val pointerIndex = event.findPointerIndex(pointerId)
                if (pointerIndex < 0) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    return false
                }

                val dy = abs(y - event.getY(pointerIndex))
                val dx = abs(x - event.getX(pointerIndex))
                val slop = ViewConfiguration.get(view.context).scaledTouchSlop

                if (dx > slop) {
                    disallowInterception = true
                } else if (dy > slop) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    return false
                }

                view.parent.requestDisallowInterceptTouchEvent(true)
            }
            else -> {
                pointerId = Int.MIN_VALUE
                x = Float.MIN_VALUE
                y = Float.MIN_VALUE
                disallowInterception = false
            }
        }

        return false
    }

    override fun onTouchEvent(view: RecyclerView, event: MotionEvent) = Unit
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}
