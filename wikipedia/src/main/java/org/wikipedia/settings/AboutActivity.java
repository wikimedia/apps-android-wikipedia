package org.wikipedia.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public class AboutActivity extends PreferenceActivityWithBack {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);

        Preference pref = this.findPreference(getString(R.string.preference_key_version));
        pref.setSummary(WikipediaApp.APP_VERSION_STRING);
        pref.setSelectable(false);

        pref = this.findPreference(getString(R.string.preference_key_feedback));
        // Will be stripped out in prod builds
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SENDTO);
        // Will be moved to a better email address at some point
        intent.setData(Uri.parse("mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App " + WikipediaApp.APP_VERSION_STRING + " Feedback"));
        pref.setIntent(intent);
    }

}
