package org.wikipedia.richtext

import android.text.TextPaint
import android.text.style.URLSpan

open class URLSpanNoUnderline(url: String) : URLSpan(url) {
    override fun updateDrawState(paint: TextPaint) {
        super.updateDrawState(paint)
        paint.isUnderlineText = false
    }
}
