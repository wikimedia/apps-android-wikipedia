package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.util.L10nUtil

open class ConfigurableTextView constructor(context: Context, attrs: AttributeSet? = null) : MaterialTextView(context, attrs) {
    fun setText(text: CharSequence?, languageCode: String?) {
        super.setText(text)
        if (!isInEditMode) {
            L10nUtil.setConditionalLayoutDirection(this, languageCode!!)
        }
    }
}
