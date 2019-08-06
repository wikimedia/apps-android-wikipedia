package org.wikipedia.settings;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;
import org.wikipedia.util.ResourceUtil;

class NotificationSettingsPreferenceLoader extends BasePreferenceLoader {

    NotificationSettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences_notifications);

        Preference pref = findPreference(R.string.preference_key_notification_poll_enable);
        pref.setOnPreferenceChangeListener(new PollPreferenceListener());

        pref = findPreference(R.string.preference_key_notification_welcome_enable);
        Drawable drawable = AppCompatResources.getDrawable(getActivity(), R.drawable.ic_wikipedia_w);
        DrawableCompat.setTint(drawable, ResourceUtil.getThemedColor(getActivity(), R.attr.primary_text_color));
        pref.setIcon(drawable);

        pref = findPreference(R.string.preference_key_notification_milestone_enable);
        drawable = AppCompatResources.getDrawable(getActivity(), R.drawable.ic_mode_edit_white_24dp);
        DrawableCompat.setTint(drawable, ResourceUtil.getThemedColor(getActivity(), R.attr.colorAccent));
        pref.setIcon(drawable);

        pref = findPreference(R.string.preference_key_notification_thanks_enable);
        drawable = AppCompatResources.getDrawable(getActivity(), R.drawable.ic_usertalk_constructive);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(getActivity(), R.color.green50));
        pref.setIcon(drawable);

        pref = findPreference(R.string.preference_key_notification_revert_enable);
        drawable = AppCompatResources.getDrawable(getActivity(), R.drawable.ic_rotate_left_white_24dp);
        DrawableCompat.setTint(drawable, ResourceUtil.getThemedColor(getActivity(), R.attr.colorError));
        pref.setIcon(drawable);
    }

    private final class PollPreferenceListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(final Preference preference, Object newValue) {
            if ((Boolean) newValue) {
                NotificationPollBroadcastReceiver.startPollTask(WikipediaApp.getInstance());
            } else {
                NotificationPollBroadcastReceiver.stopPollTask(WikipediaApp.getInstance());
            }
            return true;
        }
    }
}
