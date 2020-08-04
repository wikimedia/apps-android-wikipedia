package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible

class GoneIfEmptyTextView constructor(context: Context, attrs: AttributeSet? = null) : AppTextView(context, attrs) {
    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        isVisible = text.isNotEmpty()
    }
}