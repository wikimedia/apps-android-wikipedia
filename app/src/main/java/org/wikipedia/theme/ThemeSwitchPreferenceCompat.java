package org.wikipedia.theme;

import android.content.Context;
import android.util.AttributeSet;

import org.wikipedia.views.IntSwitchPreferenceCompat;

public class ThemeSwitchPreferenceCompat extends IntSwitchPreferenceCompat {

    public ThemeSwitchPreferenceCompat(Context context) {
        super(context);
    }

    public ThemeSwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemeSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThemeSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override protected int intValueForCheckedState() {
        return Theme.DARK.code();
    }

    @Override protected int intValueForUncheckedState() {
        return Theme.LIGHT.code();
    }
}
