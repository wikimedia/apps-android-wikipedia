package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.wikipedia.R;

public class LongPreference extends EditTextAutoSummarizePreference {
    private static final int[] DEFAULT_STYLEABLE = R.styleable.LongPreference;
    private static final int DEFAULT_STYLE = R.style.LongPreference;
    private static final int DEFAULT_RADIX = 10;
    private static final String DEFAULT_SUMMARY_FORMAT = "%d";

    private int radix = DEFAULT_RADIX;
    private String summaryFormat = DEFAULT_SUMMARY_FORMAT;

    public LongPreference(Context context) {
        this(context, null);
    }

    public LongPreference(Context context, AttributeSet attrs) {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    public LongPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, DEFAULT_STYLE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LongPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
        return longToSummary(getPersistedLong(radixStringToLong(defaultRadixValue)));
    }

    @Override
    protected boolean persistString(String radixValue) {
        boolean persistent = persistRadixString(radixValue);

        updateAutoSummary(radixValue);

        return persistent;
    }

    @Override
    protected void updateAutoSummary(String radixValue) {
        super.updateAutoSummary(sanitizeRadixString(radixValue));
    }

    protected boolean persistRadixString(String radixValue) {
        return persistLong(radixStringToLong(radixValue));
    }

    protected String sanitizeRadixString(String radixValue) {
        return longToSummary(radixStringToLong(radixValue));
    }

    protected long radixStringToLong(String radixValue) {
        return TextUtils.isEmpty(radixValue) ? 0 : Long.valueOf(radixValue, getRadix());
    }

    protected String longToSummary(long value) {
        return String.format(getSummaryFormat(), value);
    }

    private void setAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, DEFAULT_STYLEABLE,
                defStyleAttr, defStyleRes);
        radix = array.getInteger(R.styleable.LongPreference_radix, DEFAULT_RADIX);
        summaryFormat = defaultIfEmpty(array.getString(R.styleable.LongPreference_summaryFormat),
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
