package org.wikipedia.edit.richtext

import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt

class ColorSpanEx(@ColorInt foreColor: Int, @ColorInt private val backColor: Int,
                  override var start: Int, override var syntaxRule: SyntaxRule) :
        ForegroundColorSpan(foreColor), SpanExtents {
    override var end = 0

    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = backColor
        super.updateDrawState(tp)
    }
}
