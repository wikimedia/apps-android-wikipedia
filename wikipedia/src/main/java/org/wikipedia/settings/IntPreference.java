package org.wikipedia.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.wikipedia.R;

public class IntPreference extends EditTextAutoSummarizePreference {
    private static final int[] DEFAULT_STYLEABLE = R.styleable.IntPreference;
    private static final int DEFAULT_STYLE = R.style.IntPreference;
    private static final int DEFAULT_RADIX = 10;
    private static final String DEFAULT_SUMMARY_FORMAT = "%d";

    private int radix = DEFAULT_RADIX;
    private String summaryFormat = DEFAULT_SUMMARY_FORMAT;

    public IntPreference(Context context) {
        this(context, null);
    }

    public IntPreference(Context context, AttributeSet attrs) {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    public IntPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, DEFAULT_STYLE);
    }

    public IntPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    public int getRadix() {
        return radix;
    }

    public void setRadix(int radix) {
        this.radix = radix;
        updateAutoSummary();
    }

    public String getSummaryFormat() {
        return summaryFormat;
    }

    public void setSummaryFormat(String format) {
        summaryFormat = format;
        updateAutoSummary();
    }

    @Override
    protected String getPersistedString(String defaultRadixValue) {
        return intToSummary(getPersistedInt(radixStringToInt(defaultRadixValue)));
    }

    @Override
    protected boolean persistString(String radixValue) {
        boolean persistent = persistInt(radixStringToInt(radixValue));

        updateAutoSummary(radixValue);

        return persistent;
    }

    @Override
    protected void updateAutoSummary(String radixValue) {
        super.updateAutoSummary(sanitizeRadixString(radixValue));
    }

    private String sanitizeRadixString(String radixValue) {
        return intToSummary(radixStringToInt(radixValue));
    }

    private int radixStringToInt(String radixValue) {
        return TextUtils.isEmpty(radixValue) ? 0 : Long.valueOf(radixValue, getRadix()).intValue();
    }

    private String intToSummary(int value) {
        return String.format(getSummaryFormat(), value);
    }

    private void setAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, DEFAULT_STYLEABLE,
                defStyleAttr, defStyleRes);
        radix = array.getInteger(R.styleable.IntPreference_radix, DEFAULT_RADIX);
        summaryFormat = defaultIfEmpty(array.getString(R.styleable.IntPreference_summaryFormat),
                DEFAULT_SUMMARY_FORMAT);
        array.recycle();
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setAttributes(attrs, defStyleAttr, defStyleRes);
    }

    private <T extends CharSequence> T defaultIfEmpty(T value, T defaultValue) {
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }
}