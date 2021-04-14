package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import org.wikipedia.R

open class LongPreference @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyleAttr: Int = R.attr.longPreferenceStyle,
                                                    defStyleRes: Int = R.style.LongPreference) :
        EditTextAutoSummarizePreference(context, attrs, defStyleAttr, defStyleRes) {

    private var radix = DEFAULT_RADIX
    var summaryFormat: String = DEFAULT_SUMMARY_FORMAT

    init {
        context.withStyledAttributes(attrs, R.styleable.LongPreference) {
            radix = getInteger(R.styleable.LongPreference_radix, DEFAULT_RADIX)
            summaryFormat = getString(R.styleable.LongPreference_summaryFormat).let {
                if (it.isNullOrEmpty()) DEFAULT_SUMMARY_FORMAT else it
            }
        }
    }

    override fun getPersistedString(defaultRadixValue: String?): String {
        return longToSummary(getPersistedLong(radixStringToLong(defaultRadixValue)))
    }

    override fun persistString(value: String?): Boolean {
        val persistent = persistRadixString(value)
        updateAutoSummary(value)
        return persistent
    }

    override fun updateAutoSummary(value: String?) {
        super.updateAutoSummary(sanitizeRadixString(value))
    }

    protected open fun persistRadixString(radixValue: String?): Boolean {
        return persistLong(radixStringToLong(radixValue))
    }

    protected open fun sanitizeRadixString(radixValue: String?): String? {
        return longToSummary(radixStringToLong(radixValue))
    }

    protected fun radixStringToLong(radixValue: String?): Long {
        return if (radixValue.isNullOrEmpty() || radixValue == "null") 0 else radixValue.toLong(radix)
    }

    private fun longToSummary(value: Long): String {
        return String.format(summaryFormat, value)
    }

    companion object {
        private const val DEFAULT_RADIX = 10
        private const val DEFAULT_SUMMARY_FORMAT = "%d"
    }
}
