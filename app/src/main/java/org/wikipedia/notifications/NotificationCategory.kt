package org.wikipedia.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

private const val GROUP_WIKIPEDIA_NOTIFICATIONS: String = "WIKIPEDIA_NOTIFICATIONS"

enum class NotificationCategory constructor(val id: String,
                                            val title: Int,
                                            val description: Int,
                                            val iconResId: Int,
                                            val iconColor: Int = R.color.accent50,
                                            val importance: Int = NotificationManagerCompat.IMPORTANCE_HIGH,
                                            val group: String? = GROUP_WIKIPEDIA_NOTIFICATIONS) : EnumCode {
    SYSTEM("system", R.string.preference_title_notification_system, R.string.preference_summary_notification_system, R.drawable.ic_speech_bubbles),
    SYSTEM_NO_EMAIL("system-noemail", R.string.preference_title_notification_system, R.string.preference_summary_notification_system, R.drawable.ic_speech_bubbles), // default welcome
    MILESTONE_EDIT("thank-you-edit", R.string.preference_title_notification_milestone, R.string.preference_summary_notification_milestone, R.drawable.ic_edit_progressive), // milestone
    EDIT_USER_TALK("edit-user-talk", R.string.preference_title_notification_user_talk, R.string.preference_summary_notification_user_talk, R.drawable.ic_edit_user_talk),
    EDIT_THANK("edit-thank", R.string.preference_title_notification_thanks, R.string.preference_summary_notification_thanks, R.drawable.ic_user_talk, R.color.green50),
    REVERTED("reverted", R.string.preference_title_notification_revert, R.string.preference_summary_notification_revert, R.drawable.ic_revert, R.color.base20),
    LOGIN_FAIL("login-fail", R.string.preference_title_notification_login_fail, R.string.preference_summary_notification_login_fail, R.drawable.ic_user_avatar, R.color.base0),
    MENTION("mention", R.string.preference_title_notification_mention, R.string.preference_summary_notification_mention, R.drawable.ic_mention), // combines "mention", "mention-failure" and "mention-success"
    ALPHA_BUILD_CHECKER("alpha-builder-checker", R.string.alpha_update_notification_title, R.string.alpha_update_notification_text, R.drawable.ic_w_transparent, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = null),
    READING_LIST_SYNCING("reading-list-syncing", R.string.notification_syncing_reading_list_channel_title, R.string.notification_syncing_reading_list_channel_description, android.R.drawable.ic_popup_sync, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = null),
    SYNCING("syncing", R.string.notification_channel_title, R.string.notification_channel_description, android.R.drawable.stat_sys_download, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = null);

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {

        private val MAP = EnumCodeMap(NotificationCategory::class.java)

        fun find(id: String): NotificationCategory {
            for (i in 0 until MAP.size()) {
                if (id == MAP[i].id) {
                    return MAP[i]
                }
            }
            return MAP[0]
        }

        fun createNotificationChannels(context: Context) {
            // Notification channel ( >= API 26 )
            val notificationManagerCompat = NotificationManagerCompat.from(context)

            // Remove old channels
            if (notificationManagerCompat.getNotificationChannelGroupCompat("MEDIAWIKI_ECHO_CHANNEL") != null) {
                notificationManagerCompat.deleteNotificationChannel("MEDIAWIKI_ECHO_CHANNEL")
                notificationManagerCompat.deleteNotificationChannel("ALPHA_UPDATE_CHECKER_CHANNEL")
                notificationManagerCompat.deleteNotificationChannel("READING_LIST_SYNCING_CHANNEL")
                notificationManagerCompat.deleteNotificationChannel("SYNCING_CHANNEL")
            }

            var notificationChannelGroupWikipediaNotifications = notificationManagerCompat.getNotificationChannelGroupCompat(GROUP_WIKIPEDIA_NOTIFICATIONS)
            if (notificationChannelGroupWikipediaNotifications == null) {
                notificationChannelGroupWikipediaNotifications = NotificationChannelGroupCompat.Builder(GROUP_WIKIPEDIA_NOTIFICATIONS)
                    .setName(context.getString(R.string.notifications_channel_group_wikipedia_notifications))
                    .build()
                notificationManagerCompat.createNotificationChannelGroup(notificationChannelGroupWikipediaNotifications)
            }

            for (i in 0 until MAP.size()) {
                val category = MAP[i]
                var notificationChannelCompat = notificationManagerCompat.getNotificationChannelCompat(category.id)
                if (notificationChannelCompat == null) {
                    notificationChannelCompat = NotificationChannelCompat.Builder(category.id, category.importance)
                        .setName(context.getString(category.title))
                        .setDescription(context.getString(category.description))
                        .setGroup(category.group) //
                        .setLightColor(ContextCompat.getColor(context, R.color.accent50))
                        .setVibrationEnabled(true)
                        .build()
                    notificationManagerCompat.createNotificationChannel(notificationChannelCompat)
                }
            }
        }
    }
}
