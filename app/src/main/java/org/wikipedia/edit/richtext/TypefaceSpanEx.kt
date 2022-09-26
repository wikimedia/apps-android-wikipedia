package org.wikipedia.edit.richtext

import android.graphics.Typeface
import android.os.Build
import android.text.style.TypefaceSpan
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class TypefaceSpanEx(typeface: Typeface, override var start: Int, override var syntaxRule: SyntaxRule) :
        TypefaceSpan(typeface), SpanExtents {
    override var end = 0
}
