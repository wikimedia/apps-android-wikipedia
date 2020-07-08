package org.wikipedia.settings;

import org.wikipedia.views.ViewUtil;

public class NotificationSettingsFragment extends PreferenceLoaderFragment {
    public static NotificationSettingsFragment newInstance() {
        return new NotificationSettingsFragment();
    }

    @Override
    public void loadPreferences() {
        new NotificationSettingsPreferenceLoader(this).loadPreferences();
        ViewUtil.setActionBarElevation(getListView(), (NotificationSettingsActivity) requireActivity());
    }
}
