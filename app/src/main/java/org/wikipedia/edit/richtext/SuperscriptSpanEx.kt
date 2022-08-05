package org.wikipedia.edit.richtext

import android.text.style.SuperscriptSpan

class SuperscriptSpanEx(override var start: Int, override var syntaxRule: SyntaxRule) :
        SuperscriptSpan(), SpanExtents {
    override var end = 0
}
