package org.wikipedia.readinglist.recommended

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.notifications.experiment.NotificationsExperimentAbTest
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.StringUtil
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object RecommendedReadingListNotificationManager {

    private val NOTIFICATIONS_EXPERIMENT_SEND_DAY = DayOfWeek.MONDAY
    private val NOTIFICATIONS_EXPERIMENT_SEND_TIME = LocalTime.of(18, 0)

    fun scheduleRecommendedReadingListNotification(context: Context) {
        // Always cancel before scheduling a new one
        cancelRecommendedReadingListNotification(context)
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_RECOMMENDED_READING_LIST)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (NotificationsExperimentAbTest().isNotificationsExperimentEligible()) {
            // setAndAllowWhileIdle: fire once at the experiment's send time (even during Doze), instead of a repeating alarm that drifts.
            // This is a one-shot alarm, so it must be re-armed after each notification is sent (see NotificationPollBroadcastReceiver).
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                getNotificationsExperimentDurationUntilSend().toMillis() + System.currentTimeMillis(),
                PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            return
        }

        val durationUntilNextUpdate = timeUntilNextUpdate(Prefs.recommendedReadingListUpdateFrequency)
        val triggerUpdateMillis = System.currentTimeMillis() + durationUntilNextUpdate.toMillis()
        val nextUpdateIntervalInMillis = when (Prefs.recommendedReadingListUpdateFrequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> AlarmManager.INTERVAL_DAY
            RecommendedReadingListUpdateFrequency.WEEKLY -> 7 * AlarmManager.INTERVAL_DAY
            RecommendedReadingListUpdateFrequency.MONTHLY -> 0 // this can be 0 because we are cancelling and re-scheduling again for monthly
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerUpdateMillis,
            nextUpdateIntervalInMillis,
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        )
    }

    fun showNotification(context: Context, source: RecommendedReadingListUpdateFrequency) {
        val frequency = when (source) {
            RecommendedReadingListUpdateFrequency.DAILY -> context.getString(R.string.recommended_reading_list_settings_updates_frequency_daily)
            RecommendedReadingListUpdateFrequency.WEEKLY -> context.getString(R.string.recommended_reading_list_settings_updates_frequency_weekly)
            RecommendedReadingListUpdateFrequency.MONTHLY -> context.getString(R.string.recommended_reading_list_settings_updates_frequency_monthly)
        }

        val experimentSource = if (NotificationsExperimentAbTest().isNotificationsExperimentEnabled()) Prefs.notificationsExperimentDiscoverSource else null
        val (title, text) = if (experimentSource != null) {
            getNotificationsExperimentTitleAndText(context, experimentSource)
        } else {
            context.getString(R.string.recommended_reading_list_notification_title, frequency) to
                    context.getString(R.string.recommended_reading_list_notification_subtitle)
        }
        Prefs.resetRecommendedReadingList = true
        RecommendedReadingListEvent.submit("impression", "rrl_notification")
        NotificationPresenter.showNotification(
            context = context,
            builder = NotificationPresenter.getDefaultBuilder(context, 1, notificationCategory = NotificationCategory.RECOMMENDED_READING_LISTS),
            id = 1,
            title = title,
            text = text,
            longText = if (experimentSource != null) text else null,
            lang = null,
            icon = null,
            color = R.color.blue600,
            bodyIntent = ReadingListActivity.newIntent(
                context = context,
                readingListMode = ReadingListMode.RECOMMENDED,
                invokeSource = Constants.InvokeSource.NOTIFICATION,
                notificationCategory = NotificationCategory.RECOMMENDED_READING_LISTS
            )
        )
    }

    private fun getNotificationsExperimentTitleAndText(context: Context, experimentSource: NotificationsExperimentDiscoverSource): Pair<String, String> {
        val articleTitle = StringUtil.fromHtml(experimentSource.title.displayText).toString()
        return when (experimentSource.source) {
            RecommendedReadingListSource.INTERESTS -> {
                context.getString(R.string.notifications_experiment_discover_interests_title, articleTitle) to
                        context.getString(R.string.notifications_experiment_discover_message)
            }
            RecommendedReadingListSource.READING_LIST, RecommendedReadingListSource.HISTORY -> {
                context.getString(R.string.notifications_experiment_discover_saved_or_history_title, articleTitle) to
                        context.getString(R.string.notifications_experiment_discover_message)
            }
        }
    }

    fun cancelRecommendedReadingListNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_RECOMMENDED_READING_LIST)
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
    }

    private fun timeUntilNextUpdate(frequency: RecommendedReadingListUpdateFrequency): Duration {
        val now = LocalDateTime.now()
        return when (frequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> {
                val startOfNextDay = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
                Duration.between(now, startOfNextDay)
            }
            RecommendedReadingListUpdateFrequency.WEEKLY -> {
                val startOfNextWeek = now
                    .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay()
                Duration.between(now, startOfNextWeek)
            }
            RecommendedReadingListUpdateFrequency.MONTHLY -> {
                val startOfNextMonth = now
                    .plusMonths(1)
                    .withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay()
                Duration.between(now, startOfNextMonth)
            }
        }
    }

    private fun getNotificationsExperimentDurationUntilSend(): Duration {
        // Dev-only override: if a custom trigger time is set and still in the future, use it.
        val customTriggerMillis = Prefs.notificationsExperimentDiscoverDevTriggerTime
        if (ReleaseUtil.isDevRelease && customTriggerMillis > System.currentTimeMillis()) {
            return Duration.ofMillis(customTriggerMillis - System.currentTimeMillis())
        }
        val currentLocalDateTime = LocalDateTime.now()
        var notificationDateTime = currentLocalDateTime
            .with(TemporalAdjusters
            .nextOrSame(NOTIFICATIONS_EXPERIMENT_SEND_DAY))
            .toLocalDate()
            .atTime(NOTIFICATIONS_EXPERIMENT_SEND_TIME)

        // If today is the send day and the send time has already passed, schedule for next week instead.
        if (!notificationDateTime.isAfter(currentLocalDateTime)) notificationDateTime = notificationDateTime.plusWeeks(1)

        return Duration.between(currentLocalDateTime, notificationDateTime)
    }
}
