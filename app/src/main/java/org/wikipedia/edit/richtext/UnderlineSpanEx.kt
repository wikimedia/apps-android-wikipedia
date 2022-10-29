package org.wikipedia.edit.richtext

import android.text.style.UnderlineSpan

class UnderlineSpanEx(override var start: Int, override var syntaxRule: SyntaxRule) :
        UnderlineSpan(), SpanExtents {
    override var end = 0
}
