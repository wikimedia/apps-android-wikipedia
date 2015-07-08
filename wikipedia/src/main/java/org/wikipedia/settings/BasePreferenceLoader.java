package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

/*package*/ abstract class BasePreferenceLoader implements PreferenceLoader {
    @NonNull private final PreferenceHostCompat preferenceHost;

    /*package*/ BasePreferenceLoader(@NonNull PreferenceActivity activity) {
        this(new PreferenceHostCompat(activity));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    /*package*/ BasePreferenceLoader(@NonNull PreferenceFragment fragment) {
        this(new PreferenceHostCompat(fragment));
    }

    /*package*/ BasePreferenceLoader(@NonNull PreferenceHostCompat preferenceHost) {
        this.preferenceHost = preferenceHost;
    }

    protected Preference findPreference(CharSequence key) {
        return preferenceHost.findPreference(key);
    }

    protected void loadPreferences(@XmlRes int id) {
        preferenceHost.addPreferencesFromResource(id);
    }
}