package org.wikipedia.settings;

import org.wikipedia.views.ViewUtil;

public class DeveloperSettingsFragment extends PreferenceLoaderFragment {
    public static DeveloperSettingsFragment newInstance() {
        return new DeveloperSettingsFragment();
    }

    @Override
    public void loadPreferences() {
        PreferenceLoader preferenceLoader = new DeveloperSettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
        ViewUtil.setActionBarElevation(getListView(), (DeveloperSettingsActivity) requireActivity());
    }
}
