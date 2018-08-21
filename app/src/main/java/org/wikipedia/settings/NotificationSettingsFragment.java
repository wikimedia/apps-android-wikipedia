package org.wikipedia.settings;

public class NotificationSettingsFragment extends PreferenceLoaderFragment {
    public static NotificationSettingsFragment newInstance() {
        return new NotificationSettingsFragment();
    }

    @Override
    public void loadPreferences() {
        new NotificationSettingsPreferenceLoader(this).loadPreferences();
    }
}
