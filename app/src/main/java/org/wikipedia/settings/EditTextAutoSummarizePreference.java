package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

import org.wikipedia.R;

public class EditTextAutoSummarizePreference extends EditTextPreference {
    protected static final int DEFAULT_STYLE_ATTR = R.attr.editTextAutoSummarizePreferenceStyle;
    private static final int[] DEFAULT_STYLEABLE = R.styleable.EditTextAutoSummarizePreference;
    private static final int DEFAULT_STYLE = R.style.EditTextAutoSummarizePreference;
    private static final boolean DEFAULT_AUTO_SUMMARIZE = true;

    private boolean autoSummarize = DEFAULT_AUTO_SUMMARIZE;

    public EditTextAutoSummarizePreference(Context context) {
        this(context, null);
    }

    public EditTextAutoSummarizePreference(Context context, AttributeSet attrs) {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    public EditTextAutoSummarizePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, DEFAULT_STYLE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextAutoSummarizePreference(Context context, AttributeSet attrs, int defStyleAttr,
           int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    public boolean isAutoSummarize() {
        return autoSummarize;
    }

    public void setAutoSummarize(boolean summarize) {
        autoSummarize = summarize;
        updateAutoSummary();
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updateAutoSummary();
    }

    @Override
    protected boolean persistString(String value) {
        boolean persistent = super.persistString(value);

        updateAutoSummary(value);

        return persistent;
    }

    protected boolean isSet() {
        return shouldPersist() && getSharedPreferences().contains(getKey());
    }

    protected String getString(int id, Object... formatArgs) {
        return getContext().getString(id, formatArgs);
    }

    protected void updateAutoSummary() {
        updateAutoSummary(getPersistedString(null));
    }

    protected void updateAutoSummary(String value) {
        if (isAutoSummarize()) {
            setSummary(isSet() ? value : getNoValueSummary());
        }
    }

    protected String getNoValueSummary() {
        return getString(R.string.preference_summary_no_value);
    }

    private void setAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, DEFAULT_STYLEABLE,
                defStyleAttr, defStyleRes);
        autoSummarize = array.getBoolean(R.styleable.EditTextAutoSummarizePreference_autoSummarize,
                DEFAULT_AUTO_SUMMARIZE);
        array.recycle();
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setAttributes(attrs, defStyleAttr, defStyleRes);
    }
}
