package org.wikipedia.notifications.experiment

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.TopPageViews
import org.wikipedia.feed.model.TopReadCard
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object NotificationsExperimentHelper {

    private val NOTIFICATIONS_EXPERIMENT_SEND_DAY = DayOfWeek.MONDAY
    private val NOTIFICATIONS_EXPERIMENT_SEND_TIME = LocalTime.of(18, 0)
    private const val DAYS_IN_WEEK = 7
    private const val MAX_DAYS_TO_CHECK = 14
    private const val MAX_WEEKLY_MOST_READ_ARTICLES = 50

    fun maybeScheduleWeeklyMostReadNotification(context: Context) {
        if (Prefs.notificationsExperimentWeeklyMostReadScheduled || !NotificationsExperimentAbTest().isNotificationsExperimentEligible()) {
            return
        }
        scheduleNotification(context, fromFiredAlarm = false)
        Prefs.notificationsExperimentWeeklyMostReadScheduled = true
    }

    fun scheduleNotification(context: Context, fromFiredAlarm: Boolean) {
        // Always cancel before scheduling a new one
        cancelNotification(context)
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_WEEKLY_MOST_READ)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            getNotificationsExperimentTriggerMillis(fromFiredAlarm),
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    suspend fun showNotification(context: Context) {
        val languageCode = WikipediaApp.instance.languageState.appLanguageCode
        val articles = getWeeklyMostRead(languageCode)
        val mostReadArticle = articles.firstOrNull() ?: return
        val (title, text) = context.getString(R.string.notifications_experiment_weekly_most_read_notification_title) to
                context.getString(R.string.notifications_experiment_weekly_most_read_notification_description, StringUtil.fromHtml(mostReadArticle.displayTitle).toString())
        val isNotificationPosted = NotificationPresenter.showNotification(
            context = context,
            builder = NotificationPresenter.getDefaultBuilder(context, 1, notificationCategory = NotificationCategory.WEEKLY_MOST_READ),
            id = 1,
            title = title,
            text = text,
            longText = null,
            lang = null,
            icon = null,
            color = R.color.blue600,
            bodyIntent = TopReadArticlesActivity.newIntent(
                context = context,
                card = TopReadCard(TopRead(articles = articles), age = 0, site = WikiSite.forLanguageCode(languageCode)),
                fromNotification = true
            )
        )
        if (isNotificationPosted) {
            NotificationsExperimentAbTest().maybeSendExposureEventIfEligible()
        }
    }

    fun cancelNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            .setAction(NotificationPollBroadcastReceiver.ACTION_WEEKLY_MOST_READ)
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
    }

    private fun getNotificationsExperimentTriggerMillis(fromFiredAlarm: Boolean): Long {
        // Dev settings override: if a custom trigger time is set and still in the future, use it.
        val customTriggerMillis = Prefs.notificationsExperimentWeeklyMostReadDevTriggerTime
        if (customTriggerMillis > System.currentTimeMillis()) {
            return customTriggerMillis
        }
        return getNotificationsExperimentNextSendTime(ZonedDateTime.now(), fromFiredAlarm).toInstant().toEpochMilli()
    }

    // Zoned (rather than local) date-times keep the send time at the local send time across DST changes.
    internal fun getNotificationsExperimentNextSendTime(now: ZonedDateTime, fromFiredAlarm: Boolean): ZonedDateTime {
        val sendTime = now
            .with(TemporalAdjusters.nextOrSame(NOTIFICATIONS_EXPERIMENT_SEND_DAY))
            .with(NOTIFICATIONS_EXPERIMENT_SEND_TIME)
        // Move to next week if today's send time has passed, or if the alarm that just fired already covered today's send.
        val isTodaySendHandled = fromFiredAlarm && sendTime.toLocalDate() == now.toLocalDate()
        return if (!sendTime.isAfter(now) || isTodaySendHandled) sendTime.plusWeeks(1) else sendTime
    }

    /**
     * @return the week's most read articles ordered by total views, so the first one is the most read
     * article of the week, or an empty list if no page views are available.
     */
    suspend fun getWeeklyMostRead(languageCode: String): List<PageSummary> = coroutineScope {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val mainPageTitle = StringUtil.addUnderscores(MainPageNameData.valueFor(languageCode))
        val nonArticleNamespacesDeferred = async {
            ServiceFactory.get(wikiSite).getPageNamespaceWithSiteInfo(null).query?.namespaces.orEmpty().values
                .filter { it.id != Namespace.MAIN.code() }
                .map { StringUtil.addUnderscores(it.name) }
                .toSet()
        }
        val weeklyArticles = (1..MAX_DAYS_TO_CHECK).asFlow()
            .map { age -> getDailyTopArticles(wikiSite, age) }
            .filter { it.isNotEmpty() }
            .take(DAYS_IN_WEEK)
            .toList()
            .flatten()
        val nonArticleNamespaces = nonArticleNamespacesDeferred.await()

        val rankedTitles = weeklyArticles
            .groupingBy { it.article }
            .fold(0L) { totalViews, entry -> totalViews + entry.views }
            .filterKeys { it != mainPageTitle && it.substringBefore(':', "") !in nonArticleNamespaces }
            .entries
            .sortedByDescending { it.value }
            .take(MAX_WEEKLY_MOST_READ_ARTICLES)
            .map { it.key }
        if (rankedTitles.isEmpty()) {
            return@coroutineScope emptyList()
        }

        val rankByTitle = rankedTitles.withIndex().associate { it.value to it.index }
        ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(titles = rankedTitles.joinToString("|"))
            .query?.pages.orEmpty()
            .filter { it.pageId > 0 }
            .distinctBy { it.pageId }
            // The API returns pages in its own order, with titles normalized and redirects resolved.
            .sortedBy { rankByTitle[StringUtil.addUnderscores(it.redirectFrom ?: it.title)] ?: Int.MAX_VALUE }
            .map {
                PageSummary(
                    displayTitle = it.displayTitle(languageCode),
                    prefixTitle = it.title,
                    description = it.description,
                    extract = null,
                    thumbnail = it.thumbUrl(),
                    lang = languageCode,
                    pageId = it.pageId
                )
            }
    }

    private suspend fun getDailyTopArticles(wikiSite: WikiSite, age: Int): List<TopPageViews.Article> {
        val date = DateUtil.getUtcRequestDateFor(age)
        return runCatching {
            ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getTopPageViews(wikiSite.authority(), date.year, date.month, date.day)
                .items.flatMap { it.articles }
        }.getOrDefault(emptyList())
    }
}
