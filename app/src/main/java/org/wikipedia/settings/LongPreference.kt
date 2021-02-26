package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import org.wikipedia.R

open class LongPreference @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyleAttr: Int,
                                                    defStyleRes: Int = DEFAULT_STYLE) :
        EditTextAutoSummarizePreference(context, attrs, defStyleAttr, defStyleRes) {

    private var radix = DEFAULT_RADIX
    var summaryFormat: String? = DEFAULT_SUMMARY_FORMAT

    init {
        val array = context.obtainStyledAttributes(attrs, DEFAULT_STYLEABLE)
        radix = array.getInteger(R.styleable.LongPreference_radix, DEFAULT_RADIX)
        summaryFormat = array.getString(R.styleable.LongPreference_summaryFormat).toString().ifEmpty { DEFAULT_SUMMARY_FORMAT }
        array.recycle()
    }

    override fun getPersistedString(defaultRadixValue: String): String {
        return longToSummary(getPersistedLong(radixStringToLong(defaultRadixValue)))
    }

    override fun persistString(value: String): Boolean {
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
        return if (radixValue.isNullOrEmpty()) 0 else radixValue.toLong(radix)
    }

    private fun longToSummary(value: Long): String {
        return String.format(summaryFormat.orEmpty(), value)
    }

    companion object {
        private val DEFAULT_STYLEABLE = R.styleable.LongPreference
        private const val DEFAULT_STYLE = R.style.LongPreference
        private const val DEFAULT_RADIX = 10
        private const val DEFAULT_SUMMARY_FORMAT = "%d"
    }
}
