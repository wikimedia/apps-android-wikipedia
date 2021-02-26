package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import org.wikipedia.R

open class EditTextAutoSummarizePreference @JvmOverloads constructor(context: Context,
                                                                     attrs: AttributeSet?,
                                                                     defStyleAttr: Int = DEFAULT_STYLE_ATTR,
                                                                     defStyleRes: Int = DEFAULT_STYLE) :
        EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var autoSummarize = DEFAULT_AUTO_SUMMARIZE
    private val isSet: Boolean
        get() = shouldPersist() && sharedPreferences.contains(key)

    init {
        val array = context.obtainStyledAttributes(attrs, DEFAULT_STYLEABLE,
                defStyleAttr, defStyleRes)
        autoSummarize = array.getBoolean(R.styleable.EditTextAutoSummarizePreference_autoSummarize,
                DEFAULT_AUTO_SUMMARIZE)
        array.recycle()
    }

    override fun onAttached() {
        super.onAttached()
        updateAutoSummary()
    }

    override fun persistString(value: String): Boolean {
        val persistent = super.persistString(value)
        updateAutoSummary(value)
        return persistent
    }

    protected fun getString(id: Int, vararg formatArgs: Any?): String {
        return context.getString(id, *formatArgs)
    }

    private fun updateAutoSummary() {
        updateAutoSummary(getPersistedString(null))
    }

    protected open fun updateAutoSummary(value: String?) {
        if (autoSummarize) {
            summary = if (isSet) value else getString(R.string.preference_summary_no_value)
        }
    }

    companion object {
        const val DEFAULT_STYLE_ATTR = R.attr.editTextAutoSummarizePreferenceStyle
        private val DEFAULT_STYLEABLE = R.styleable.EditTextAutoSummarizePreference
        private const val DEFAULT_STYLE = R.style.EditTextAutoSummarizePreference
        private const val DEFAULT_AUTO_SUMMARIZE = true
    }
}
