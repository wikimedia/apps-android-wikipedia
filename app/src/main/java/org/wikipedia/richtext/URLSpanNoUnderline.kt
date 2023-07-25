package org.wikipedia.richtext

import android.text.TextPaint
import android.text.style.URLSpan
import androidx.annotation.ColorInt

open class URLSpanNoUnderline(url: String, @ColorInt private val color: Int = -1) : URLSpan(url) {
    override fun updateDrawState(paint: TextPaint) {
        super.updateDrawState(paint)
        paint.isUnderlineText = false
        if (color != -1) {
            paint.color = color
        }
    }
}
