package org.wikipedia.edit.richtext

import android.text.style.RelativeSizeSpan

class RelativeSizeSpanEx(size: Float, override var start: Int, override var syntaxRule: SyntaxRule) :
        RelativeSizeSpan(size), SpanExtents {
    override var end = 0
}
