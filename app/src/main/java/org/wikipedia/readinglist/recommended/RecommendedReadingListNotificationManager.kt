package org.wikipedia.readinglist.recommended

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.wikipedia.R
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.settings.Prefs
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object RecommendedReadingListNotificationManager {
    private const val NOTIFICATION_TYPE_LOCAL = "local"

    fun scheduleRecommendedReadingListNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.Companion.ACTION_RECOMMENDED_READING_LIST)
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
        NotificationPresenter.showNotification(
            context = context,
            builder = NotificationPresenter.getDefaultBuilder(context, 1, NOTIFICATION_TYPE_LOCAL),
            id = 1,
            title = context.getString(R.string.recommended_reading_list_notification_title, frequency),
            text = context.getString(R.string.recommended_reading_list_notification_subtitle),
            longText = null,
            lang = null,
            icon = null,
            color = R.color.blue600,
            bodyIntent = ReadingListActivity.Companion.newIntent(context, ReadingListMode.RECOMMENDED)
        )
    }

    fun cancelRecommendedReadingListNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.Companion.ACTION_RECOMMENDED_READING_LIST)
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
    }

    private fun timeUntilNextUpdate(frequency: RecommendedReadingListUpdateFrequency): Duration {
        val now = LocalDateTime.now()
        return when (frequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> {
                val startOfNextDay = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
                return Duration.between(now, startOfNextDay)
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
}