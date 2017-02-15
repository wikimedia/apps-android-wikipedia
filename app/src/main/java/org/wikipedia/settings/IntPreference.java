package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

public class IntPreference extends LongPreference {
    public IntPreference(Context context) {
        this(context, null);
    }

    public IntPreference(Context context, AttributeSet attrs) {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    public IntPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IntPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected String getPersistedString(String defaultRadixValue) {
        return intToSummary(getPersistedInt(radixStringToInt(defaultRadixValue)));
    }

    @Override
    protected boolean persistRadixString(String radixValue) {
        return persistInt(radixStringToInt(radixValue));
    }

    @Override
    protected String sanitizeRadixString(String radixValue) {
        return intToSummary(radixStringToInt(radixValue));
    }

    private int radixStringToInt(String radixValue) {
        return Long.valueOf(radixStringToLong(radixValue)).intValue();
    }

    private String intToSummary(int value) {
        return String.format(getSummaryFormat(), value);
    }
}
