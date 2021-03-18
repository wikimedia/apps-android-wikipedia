package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View

class GoneIfEmptyTextView constructor(context: Context, attrs: AttributeSet? = null) : AppTextView(context, attrs) {
    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
    }
}
