package org.wikipedia.edit.richtext

import android.text.style.SubscriptSpan

class SubscriptSpanEx(override var start: Int, override var syntaxRule: SyntaxRule) :
        SubscriptSpan(), SpanExtents {
    override var end = 0
}
