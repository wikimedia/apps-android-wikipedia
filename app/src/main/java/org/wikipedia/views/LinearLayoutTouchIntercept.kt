package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class LinearLayoutTouchIntercept @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {
    private var onInterceptTouchListener: OnTouchListener? = null

    fun setOnInterceptTouchListener(listener: OnTouchListener?) {
        onInterceptTouchListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onInterceptTouchListener?.onTouch(this, ev)
        return super.onInterceptTouchEvent(ev)
    }
}