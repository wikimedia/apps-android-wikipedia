package org.wikipedia.edit.richtext

import android.text.style.StyleSpan

class StyleSpanEx(style: Int, override var start: Int, override var syntaxRule: SyntaxRule) :
        StyleSpan(style), SpanExtents {
    override var end = 0
}
