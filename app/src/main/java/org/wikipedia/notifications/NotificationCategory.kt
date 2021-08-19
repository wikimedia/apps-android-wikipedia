package org.wikipedia.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap


enum class NotificationCategory constructor(val id: String,
                                            val title: Int,
                                            val description: Int,
                                            val iconResId: Int,
                                            val iconColor: Int = R.color.accent50) : EnumCode {
    SYSTEM("system", R.string.preference_title_notification_system, R.string.preference_summary_notification_system, R.drawable.ic_speech_bubbles),
    SYSTEM_NO_EMAIL("system-noemail", R.string.preference_title_notification_system, R.string.preference_summary_notification_system, R.drawable.ic_speech_bubbles), // default welcome
    MILESTONE_EDIT("thank-you-edit", R.string.preference_title_notification_milestone, R.string.preference_summary_notification_milestone, R.drawable.ic_edit_progressive),  // milestone
    EDIT_USER_TALK("edit-user-talk", R.string.preference_title_notification_user_talk, R.string.preference_summary_notification_user_talk, R.drawable.ic_edit_user_talk),
    EDIT_THANK("edit-thank", R.string.preference_title_notification_thanks, R.string.preference_summary_notification_thanks, R.drawable.ic_user_talk, R.color.green50),
    REVERTED("reverted", R.string.preference_title_notification_revert, R.string.preference_summary_notification_revert, R.drawable.ic_revert, R.color.base20),
    LOGIN_FAIL("login-fail", R.string.preference_title_notification_login_fail, R.string.preference_summary_notification_login_fail, R.drawable.ic_user_avatar, R.color.base0),
    MENTION("mention", R.string.preference_title_notification_mention, R.string.preference_summary_notification_mention, R.drawable.ic_mention);  // combines "mention", "mention-failure" and "mention-success"

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        private const val GROUP_WIKIPEDIA_NOTIFICATIONS = "wikipedia-notifications"
        private const val GROUP_OTHER = "other"

        private val MAP = EnumCodeMap(NotificationCategory::class.java)

        @JvmStatic
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

            notificationManagerCompat.deleteNotificationChannel(context.getString(R.string.notification_echo_channel_description))

            var notificationChannelGroupWikipediaNotifications = notificationManagerCompat.getNotificationChannelGroupCompat(GROUP_WIKIPEDIA_NOTIFICATIONS)
            if (notificationChannelGroupWikipediaNotifications == null) {
                notificationChannelGroupWikipediaNotifications = NotificationChannelGroupCompat.Builder(GROUP_WIKIPEDIA_NOTIFICATIONS)
                    .setName(context.getString(R.string.notifications_channel_group_wikipedia_notifications))
                    .build()
                notificationManagerCompat.createNotificationChannelGroup(notificationChannelGroupWikipediaNotifications)
            }

            var notificationChannelGroupOther = notificationManagerCompat.getNotificationChannelGroupCompat(GROUP_OTHER)
            if (notificationChannelGroupOther == null) {
                notificationChannelGroupOther = NotificationChannelGroupCompat.Builder(GROUP_OTHER)
                    .setName(context.getString(R.string.notifications_channel_group_other))
                    .build()
                notificationManagerCompat.createNotificationChannelGroup(notificationChannelGroupOther)
            }

            for (i in 0 until MAP.size()) {
                val category = MAP[i]
                var notificationChannelCompat = notificationManagerCompat.getNotificationChannelCompat(category.id)
                if (notificationChannelCompat == null) {
                    notificationChannelCompat = NotificationChannelCompat.Builder(category.id, NotificationManagerCompat.IMPORTANCE_HIGH)
                        .setName(context.getString(category.title))
                        .setDescription(context.getString(category.description))
                        .setGroup(GROUP_WIKIPEDIA_NOTIFICATIONS)
                        .setLightColor(ContextCompat.getColor(context, R.color.accent50))
                        .setVibrationEnabled(true)
                        .build()
                    notificationManagerCompat.createNotificationChannel(notificationChannelCompat)
                }
            }

            // TODO: add sync reading list and downloading articles notifications here
        }
    }
}
