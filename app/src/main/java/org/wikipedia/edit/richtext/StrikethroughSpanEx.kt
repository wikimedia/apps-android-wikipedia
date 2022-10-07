package org.wikipedia.edit.richtext

import android.text.style.StrikethroughSpan

class StrikethroughSpanEx(override var start: Int, override var syntaxRule: SyntaxRule) :
        StrikethroughSpan(), SpanExtents {
    override var end = 0
}
