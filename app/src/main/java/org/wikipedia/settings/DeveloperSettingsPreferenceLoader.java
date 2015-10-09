package org.wikipedia.settings;

import org.wikipedia.R;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

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
        setupRestBaseCheckboxes();
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

    private void setupRestBaseCheckboxes() {
        CheckBoxPreference manualPreference = (CheckBoxPreference) findPreference(getManualKey());
        manualPreference.setOnPreferenceChangeListener(setRestBaseManuallyChangeListener);
        setUseRestBasePreference(manualPreference.isChecked());
    }

    private Preference.OnPreferenceChangeListener setRestBaseManuallyChangeListener
            = new Preference.OnPreferenceChangeListener() {
        /**
         * Called when the useRestBaseSetManually preference has been changed by the user. This is
         * called before the state of the Preference is about to be updated and
         * before the state is persisted.
         *
         * @param preference The changed Preference.
         * @param newValue   The new value of the Preference.
         * @return True to update the state of the Preference with the new value.
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setUseRestBasePreference((Boolean) newValue);
            return true;
        }
    };

    protected void setUseRestBasePreference(boolean manualMode) {
        RbSwitch.INSTANCE.update();
        CheckBoxPreference useRestBasePref = getUseRestBasePreference();
        useRestBasePref.setEnabled(manualMode);
        useRestBasePref.setChecked(RbSwitch.INSTANCE.isRestBaseEnabled());
    }

    protected CheckBoxPreference getUseRestBasePreference() {
        return (CheckBoxPreference) findPreference(getUseRestBaseKey());
    }

    private String getManualKey() {
        return context.getString(R.string.preference_key_use_restbase_manual);
    }

    private String getUseRestBaseKey() {
        return context.getString(R.string.preference_key_use_restbase);
    }
}