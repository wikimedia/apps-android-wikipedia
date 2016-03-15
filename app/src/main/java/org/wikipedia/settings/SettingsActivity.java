package org.wikipedia.settings;

import org.wikipedia.activity.SingleFragmentActivity;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }
}
