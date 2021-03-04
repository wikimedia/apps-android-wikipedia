package org.wikipedia.richtext

import android.graphics.Typeface
import android.text.TextPaint

class URLSpanBoldNoUnderline(url: String) : URLSpanNoUnderline(url) {
    override fun updateDrawState(paint: TextPaint) {
        super.updateDrawState(paint)
        paint.typeface = Typeface.DEFAULT_BOLD
    }
}
