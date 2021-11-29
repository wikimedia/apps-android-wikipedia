package org.wikipedia.notifications

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap
import org.wikipedia.util.log.L

private const val GROUP_WIKIPEDIA_NOTIFICATIONS: String = "WIKIPEDIA_NOTIFICATIONS"
private const val GROUP_OTHER: String = "WIKIPEDIA_NOTIFICATIONS_OTHER"

@Suppress("unused")
enum class NotificationCategory constructor(val id: String,
                                            @StringRes val title: Int,
                                            @StringRes val description: Int,
                                            @DrawableRes val iconResId: Int = R.drawable.ic_settings_black_24dp,
                                            @AttrRes val iconColor: Int = R.attr.colorAccent,
                                            val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
                                            val group: String? = GROUP_WIKIPEDIA_NOTIFICATIONS) : EnumCode {
    SYSTEM("system", R.string.preference_title_notification_system, R.string.preference_summary_notification_system, R.drawable.ic_settings_black_24dp),
    MILESTONE_EDIT("thank-you-edit", R.string.preference_title_notification_milestone, R.string.preference_summary_notification_milestone, R.drawable.ic_notification_milestone), // milestone
    EDIT_USER_TALK("edit-user-talk", R.string.preference_title_notification_user_talk, R.string.preference_summary_notification_user_talk, R.drawable.ic_notification_user_talk, importance = NotificationManagerCompat.IMPORTANCE_HIGH),
    EDIT_THANK("edit-thank", R.string.preference_title_notification_thanks, R.string.preference_summary_notification_thanks, R.drawable.ic_notification_thanks),
    REVERTED("reverted", R.string.preference_title_notification_revert, R.string.preference_summary_notification_revert, R.drawable.ic_notification_reverted_edit, R.attr.colorError, importance = NotificationManagerCompat.IMPORTANCE_HIGH),
    LOGIN_FAIL("login-fail", R.string.preference_title_notification_login_fail, R.string.preference_summary_notification_login_fail, R.drawable.ic_notification_alert, R.attr.colorError),
    MENTION("mention", R.string.preference_title_notification_mention, R.string.preference_summary_notification_mention, R.drawable.ic_notification_mention, importance = NotificationManagerCompat.IMPORTANCE_HIGH), // combines "mention", "mention-failure" and "mention-success"
    EMAIL_USER("emailuser", R.string.preference_title_notification_email_user, R.string.preference_summary_notification_email_user, R.drawable.ic_notification_email, importance = NotificationManagerCompat.IMPORTANCE_HIGH),
    USER_RIGHTS("user-rights", R.string.preference_title_notification_user_rights, R.string.preference_summary_notification_user_rights, R.drawable.ic_notification_user_rights, importance = NotificationManagerCompat.IMPORTANCE_HIGH),
    ARTICLE_LINKED("article-linked", R.string.preference_title_notification_article_linked, R.string.preference_summary_notification_article_linked, R.drawable.ic_notification_page_link),
    ALPHA_BUILD_CHECKER("alpha-builder-checker", R.string.alpha_update_notification_title, R.string.alpha_update_notification_text, R.drawable.ic_w_transparent, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = GROUP_OTHER),
    READING_LIST_SYNCING("reading-list-syncing", R.string.notification_syncing_reading_list_channel_title, R.string.notification_syncing_reading_list_channel_description, android.R.drawable.ic_popup_sync, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = GROUP_OTHER),
    SYNCING("syncing", R.string.notification_channel_title, R.string.notification_channel_description, android.R.drawable.stat_sys_download, importance = NotificationManagerCompat.IMPORTANCE_LOW, group = GROUP_OTHER);

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {

        private val MENTIONS_GROUP = listOf(MENTION, EDIT_USER_TALK, EMAIL_USER, USER_RIGHTS, REVERTED)
        val FILTERS_GROUP = listOf(EDIT_USER_TALK, MENTION, EMAIL_USER, REVERTED, USER_RIGHTS, EDIT_THANK, MILESTONE_EDIT, LOGIN_FAIL, SYSTEM, ARTICLE_LINKED)

        val MAP = EnumCodeMap(NotificationCategory::class.java)

        private fun findOrNull(id: String): NotificationCategory? {
            return MAP.valueIterator().asSequence().firstOrNull { id == it.id || id.startsWith(it.id) }
        }

        fun find(id: String): NotificationCategory {
            return findOrNull(id) ?: MAP[0]
        }

        fun isMentionsGroup(category: String): Boolean {
            return MENTIONS_GROUP.find { category.startsWith(it.id) } != null
        }

        fun isFiltersGroup(category: String): Boolean {
            return FILTERS_GROUP.find { category.startsWith(it.id) } != null
        }

        fun createNotificationChannels(context: Context) {
            // Notification channel ( >= API 26 )
            val notificationManagerCompat = NotificationManagerCompat.from(context)

            var notificationChannelGroupWikipediaNotifications = notificationManagerCompat.getNotificationChannelGroupCompat(GROUP_WIKIPEDIA_NOTIFICATIONS)

            if (notificationChannelGroupWikipediaNotifications == null) {
                notificationChannelGroupWikipediaNotifications = NotificationChannelGroupCompat.Builder(GROUP_WIKIPEDIA_NOTIFICATIONS)
                    .setName(context.getString(R.string.notifications_channel_group_wikipedia_notifications_title))
                    .setDescription(context.getString(R.string.notifications_channel_group_wikipedia_notifications_description))
                    .build()
                notificationManagerCompat.createNotificationChannelGroup(notificationChannelGroupWikipediaNotifications)

                notificationChannelGroupWikipediaNotifications = NotificationChannelGroupCompat.Builder(GROUP_OTHER)
                    .setName(context.getString(R.string.notifications_channel_group_other_title))
                    .setDescription(context.getString(R.string.notifications_channel_group_other_title))
                    .build()
                notificationManagerCompat.createNotificationChannelGroup(notificationChannelGroupWikipediaNotifications)
            } else {
                // cancel the process because the following notification channels were created.
                L.d("Create notification channels skipped.")
                return
            }

            // Remove old channels
            notificationManagerCompat.deleteNotificationChannel("MEDIAWIKI_ECHO_CHANNEL")
            notificationManagerCompat.deleteNotificationChannel("ALPHA_UPDATE_CHECKER_CHANNEL")
            notificationManagerCompat.deleteNotificationChannel("READING_LIST_SYNCING_CHANNEL")
            notificationManagerCompat.deleteNotificationChannel("SYNCING_CHANNEL")

            val notificationChannels = MAP.valueIterator().asSequence().toList().mapNotNull {
                val notificationChannelCompat = notificationManagerCompat.getNotificationChannelCompat(it.id)
                if (notificationChannelCompat == null) {
                    NotificationChannelCompat.Builder(it.id, it.importance)
                        .setName(context.getString(it.title))
                        .setDescription(context.getString(it.description))
                        .setGroup(it.group)
                        .setLightColor(ContextCompat.getColor(context, R.color.accent50))
                        .setVibrationEnabled(true)
                        .build()
                } else {
                    null
                }
            }
            notificationManagerCompat.createNotificationChannelsCompat(notificationChannels)
        }
    }
}
