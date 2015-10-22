package org.wikipedia.settings;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.util.log.L;

/*package*/ class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    @NonNull private final Context context;

    @NonNull private final Preference.OnPreferenceChangeListener setRestBaseManuallyChangeListener
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
        setupRemoteLogButton(findPreference(R.string.preference_key_remote_log));
    }

    private void setupRestBaseCheckboxes() {
        CheckBoxPreference manualPreference = (CheckBoxPreference) findPreference(getManualKey());
        manualPreference.setOnPreferenceChangeListener(setRestBaseManuallyChangeListener);
        setUseRestBasePreference(manualPreference.isChecked());
    }

    private String getManualKey() {
        return context.getString(R.string.preference_key_use_restbase_manual);
    }

    private void setUseRestBasePreference(boolean manualMode) {
        RbSwitch.INSTANCE.update();
        CheckBoxPreference useRestBasePref = getUseRestBasePreference();
        useRestBasePref.setEnabled(manualMode);
        useRestBasePref.setChecked(RbSwitch.INSTANCE.isRestBaseEnabled());
    }

    private CheckBoxPreference getUseRestBasePreference() {
        return (CheckBoxPreference) findPreference(getUseRestBaseKey());
    }

    private String getUseRestBaseKey() {
        return context.getString(R.string.preference_key_use_restbase);
    }

    private String getCrashButtonKey() {
        return context.getString(R.string.preferences_developer_crash_key);
    }

    private void setupCrashButton(Preference button) {
        button.setOnPreferenceClickListener(buildCrashButtonClickListener());
    }

    private Preference.OnPreferenceClickListener buildCrashButtonClickListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                throw new TestException("User tested crash functionality.");
            }
        };
    }

    private void setupRemoteLogButton(Preference button) {
        button.setOnPreferenceChangeListener(buildRemoteLogPreferenceChangeListener());
    }

    private Preference.OnPreferenceChangeListener buildRemoteLogPreferenceChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                L.logRemoteError(new RemoteLogException(newValue.toString()));
                WikipediaApp.getInstance().checkCrashes(getActivity());
                return true;
            }
        };
    }

    private static class TestException extends RuntimeException {
        TestException(String message) {
            super(message);
        }
    }
}
