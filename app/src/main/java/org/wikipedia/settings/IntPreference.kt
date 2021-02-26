package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet

open class IntPreference @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyleAttr: Int = DEFAULT_STYLE_ATTR,
                                              defStyleRes: Int) :
        LongPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun getPersistedString(defaultRadixValue: String): String {
        return intToSummary(getPersistedInt(radixStringToInt(defaultRadixValue)))
    }

    override fun persistRadixString(radixValue: String?): Boolean {
        return persistInt(radixStringToInt(radixValue))
    }

    override fun sanitizeRadixString(radixValue: String?): String {
        return intToSummary(radixStringToInt(radixValue))
    }

    private fun radixStringToInt(radixValue: String?): Int {
        return radixStringToLong(radixValue).toInt()
    }

    private fun intToSummary(value: Int): String {
        return String.format(summaryFormat.orEmpty(), value)
    }
}