package org.wikipedia.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;

public abstract class IntSwitchPreferenceCompat extends SwitchPreferenceCompat {

    public IntSwitchPreferenceCompat(Context context) {
        super(context);
    }

    public IntSwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IntSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override public void setChecked(boolean checked) {
        boolean changed = mChecked != checked;
        if (changed) {
            mChecked = checked;
            persistInt(booleanToInt(checked));
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public int getValue() {
        return getPersistedValue();
    }

    protected abstract int intValueForCheckedState();

    protected abstract int intValueForUncheckedState();

    @Override protected void onClick() {
        int newValue = booleanToInt(!mChecked);
        if (callChangeListener(newValue)) {
            setChecked(intToBoolean(newValue));
        }
    }

    @Override protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setChecked(restoreValue
                ? intToBoolean(getPersistedValue())
                : intToBoolean((int) defaultValue));
    }

    private int booleanToInt(boolean checked) {
        return checked ? intValueForCheckedState() : intValueForUncheckedState();
    }

    private boolean intToBoolean(int val) {
        return val == intValueForCheckedState();
    }

    private int getPersistedValue() {
        return getPersistedInt(booleanToInt(mChecked));
    }
}
