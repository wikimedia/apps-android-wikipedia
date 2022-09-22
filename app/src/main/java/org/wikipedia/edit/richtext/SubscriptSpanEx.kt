package org.wikipedia.edit.richtext

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class SubscriptSpanEx(override var start: Int, override var syntaxRule: SyntaxRule) :
        MetricAffectingSpan(), SpanExtents {
    override var end = 0

    override fun updateDrawState(tp: TextPaint) {
        tp.textSize = tp.textSize * 0.8f
        tp.baselineShift -= (tp.ascent() / 4).toInt()
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.textSize = tp.textSize * 0.8f
        tp.baselineShift -= (tp.ascent() / 4).toInt()
    }
}
