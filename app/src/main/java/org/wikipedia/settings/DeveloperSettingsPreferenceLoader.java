package org.wikipedia.settings;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import org.wikipedia.R;

/*package*/ class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    @NonNull private final Context context;

    /*package*/
    DeveloperSettingsPreferenceLoader(@NonNull PreferenceFragment fragment) {
        super(fragment);
        this.context = fragment.getActivity().getApplicationContext();
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.developer_preferences);
        setupCrashButton(findPreference(getCrashButtonKey()));
    }

    private void setupCrashButton(Preference crashButton) {
        crashButton.setOnPreferenceClickListener(buildCrashButtonClickListener());
    }

    private Preference.OnPreferenceClickListener buildCrashButtonClickListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                throw new NullPointerException(buildCrashMessage());
            }
        };
    }

    private String buildCrashMessage() {
        return "Crash test from " + getClass().getName();
    }

    private String getCrashButtonKey() {
        return context.getString(R.string.preferences_developer_crash_key);
    }
}