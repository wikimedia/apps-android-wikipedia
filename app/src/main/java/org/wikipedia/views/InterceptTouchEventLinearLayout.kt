package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class InterceptTouchEventLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean = true
}
