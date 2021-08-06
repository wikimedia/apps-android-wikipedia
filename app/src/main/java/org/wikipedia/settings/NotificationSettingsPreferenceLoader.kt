package org.wikipedia.settings

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil.getThemedColor

internal class NotificationSettingsPreferenceLoader(fragment: PreferenceFragmentCompat) : BasePreferenceLoader(fragment) {
    override fun loadPreferences() {
        loadPreferences(R.xml.preferences_notifications)
        var drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_speech_bubbles)!!
        drawable.setTint(getThemedColor(activity, R.attr.colorAccent))
        findPreference(R.string.preference_key_notification_system_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_edit_progressive)!!
        drawable.setTint(getThemedColor(activity, R.attr.colorAccent))
        findPreference(R.string.preference_key_notification_milestone_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_user_talk)!!
        drawable.setTint(ContextCompat.getColor(activity, R.color.green50))
        findPreference(R.string.preference_key_notification_thanks_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_revert)!!
        drawable.setTint(getThemedColor(activity, R.attr.material_theme_secondary_color))
        findPreference(R.string.preference_key_notification_revert_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_mention)!!
        drawable.setTint(getThemedColor(activity, R.attr.colorAccent))
        findPreference(R.string.preference_key_notification_mention_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_user_avatar)!!
        drawable.setTint(getThemedColor(activity, R.attr.material_theme_secondary_color))
        findPreference(R.string.preference_key_notification_login_fail_enable).icon = drawable

        drawable = AppCompatResources.getDrawable(activity, R.drawable.ic_edit_user_talk)!!
        drawable.setTint(getThemedColor(activity, R.attr.colorAccent))
        findPreference(R.string.preference_key_notification_user_talk_enable).icon = drawable
    }
}
