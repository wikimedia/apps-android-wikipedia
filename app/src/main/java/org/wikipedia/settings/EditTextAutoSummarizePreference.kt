package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.preference.EditTextPreference
import org.wikipedia.R

open class EditTextAutoSummarizePreference @JvmOverloads constructor(context: Context,
                                                                     attrs: AttributeSet?,
                                                                     defStyleAttr: Int = R.attr.editTextAutoSummarizePreferenceStyle,
                                                                     defStyleRes: Int = R.style.EditTextAutoSummarizePreference) :
        EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var autoSummarize = DEFAULT_AUTO_SUMMARIZE

    init {
        context.withStyledAttributes(attrs, R.styleable.EditTextAutoSummarizePreference, defStyleAttr, defStyleRes) {
            autoSummarize = getBoolean(R.styleable.EditTextAutoSummarizePreference_autoSummarize, DEFAULT_AUTO_SUMMARIZE)
        }
    }

    override fun onAttached() {
        super.onAttached()
        updateAutoSummary(getPersistedString(null))
    }

    override fun persistString(value: String?): Boolean {
        val persistent = super.persistString(value)
        updateAutoSummary(value)
        return persistent
    }

    protected open fun updateAutoSummary(value: String?) {
        if (autoSummarize && sharedPreferences != null) {
            summary = if (shouldPersist() && sharedPreferences!!.contains(key)) value
            else context.getString(R.string.preference_summary_no_value)
        }
    }

    companion object {
        private const val DEFAULT_AUTO_SUMMARIZE = true
    }
}
