package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import org.wikipedia.util.L10nUtil

open class ConfigurableTextView constructor(context: Context, attrs: AttributeSet? = null) : AppCompatTextView(context, attrs) {
    fun setText(text: CharSequence?, languageCode: String?) {
        super.setText(text)
        L10nUtil.setConditionalLayoutDirection(this, languageCode!!)
    }
}
