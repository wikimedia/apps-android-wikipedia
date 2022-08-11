package org.wikipedia.edit.richtext

import android.graphics.Typeface
import android.text.style.TypefaceSpan

class TypefaceSpanEx(typeface: Typeface, override var start: Int, override var syntaxRule: SyntaxRule) :
        TypefaceSpan(typeface), SpanExtents {
    override var end = 0
}
