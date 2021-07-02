package org.wikipedia.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class PageScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    interface Callback {
        fun onClick()
        fun onScrollStart()
        fun onScrollStop()
        fun onVerticalScroll(dy: Float)
    }

    private var dragging = false
    private var prevX = 0f
    private var prevY = 0f
    private var startMillis: Long = 0
    var callback: Callback? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.rawX
                prevY = event.rawY
                if (!dragging) {
                    dragging = true
                    callback?.onScrollStart()
                }
                startMillis = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> if (dragging) {
                dragging = false
                if (System.currentTimeMillis() - startMillis < CLICK_MILLIS) {
                    callback?.onClick()
                    // TODO: enable if we want the swipe-out action.
                    // } else if (Math.abs(event.getRawX() - startX) > SLIDE_OUT_SLOP_WIDTH) {
                    //    if (callback != null) {
                    //        callback.onSwipeOut();
                    //    }
                } else {
                    callback?.onScrollStop()
                }
            }
            MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                callback?.onScrollStop()
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                val dy = event.rawY - prevY
                callback?.onVerticalScroll(dy)
                prevX = event.rawX
                prevY = event.rawY
            }
            else -> { }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val CLICK_MILLIS = 250
    }
}
