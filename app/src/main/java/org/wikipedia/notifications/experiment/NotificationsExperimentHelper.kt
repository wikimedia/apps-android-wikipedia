package org.wikipedia.notifications.experiment

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.toBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.okhttp.HttpStatusException
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
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.views.imageservice.WhiteBackgroundTransformation
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

object NotificationsExperimentHelper {

    private val NOTIFICATIONS_EXPERIMENT_SEND_DAY = DayOfWeek.MONDAY
    private val NOTIFICATIONS_EXPERIMENT_SEND_TIME = LocalTime.of(18, 0)
    private const val DAYS_IN_WEEK = 7
    private const val MAX_DAYS_TO_CHECK = 14
    private const val MAX_WEEKLY_MOST_READ_ARTICLES = 50
    private const val NOTIFICATION_ID = 1003
    private const val FALLBACK_LARGE_ICON_SIZE_DP = 48f
    private const val FALLBACK_LARGE_ICON_LOGO_SIZE_DP = 28f

    // Copied from the "*" deny list of the wikifeeds most-read endpoint, which is the one applied to English Wikipedia:
    // https://github.com/wikimedia/mediawiki-services-wikifeeds/blob/master/lib/most-read.js
    private val MOST_READ_DENY_LIST = setOf(
        "-",
        "wiki.phtml",
        "Test_card",
        "Web_scraping",
        "XHamster",
        "Java_(programming_language)",
        "Images/upload/bel.jpg",
        "Superintelligence:_Paths,_Dangers,_Strategies",
        "Okto",
        "Proyecto_40",
        "AMGTV",
        "Lali_Espósito",
        "La7",
        "Vagina",
        "کس", // mznwiki
        "مقعد", // mznwiki
        "Tobias_Sammet", // dewiki T238942
        "Avantasia", // dewiki T238942
        "Edguy", // dewiki T238942
        "Pornhub", // dewiki T238942
        "Index", // T327904
        "Index_(statistics)",
        "Index,_Washington",
        "Index_(economics)",
        "XXX:_Return_of_Xander_Cage",
        "XXX_(film_series)",
        "XXX_(2002_film)",
        "Cookie_(informatique)",
        "Nigger",
        "nigger",
        "YouTube",
        "Cleopatra",
        "Outlook.com",
        "Sex",
        ".xxx",
        "XNXX",
        "XVideos",
        "XXXX_(beer)",
        "XXXX",
        "XXXX_Gold",
        "XXXX_(album)",
        "XXXX_Summer_Bright_Lager",
        "XXXX_Panzer_Corps",
        "XXX:_State_of_the_Union",
        "XXXTentacion",
        "X_(2022_film)",
        "Super_Bowl_XXX",
        "Murder_of_XXXTentacion",
        "ChatGPT",
        "Magic_cross_piercing",
        "Ejaculation",
        "Erection"
    )

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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val abTest = NotificationsExperimentAbTest()
        if (!abTest.isNotificationsExperimentEnabled()) {
            // Control users don't get the notification, but are exposed at the moment they would have received it.
            abTest.maybeSendExposureEventIfEligible()
            return
        }
        val languageCode = WikipediaApp.instance.languageState.appLanguageCode
        val articles = getWeeklyMostRead(languageCode)
        val mostReadArticle = articles.firstOrNull() ?: return
        val (title, text) = context.getString(R.string.notifications_experiment_weekly_most_read_notification_title) to
                context.getString(R.string.notifications_experiment_weekly_most_read_notification_description, StringUtil.fromHtml(mostReadArticle.displayTitle).toString())
        val color = R.color.blue600
        val largeIcon = loadThumbnail(context, mostReadArticle.thumbnailUrl) ?: drawWikipediaLogo(context, color)
        val isNotificationPosted = NotificationPresenter.showNotification(
            context = context,
            builder = NotificationPresenter.getDefaultBuilder(context, NOTIFICATION_ID.toLong(), notificationCategory = NotificationCategory.WEEKLY_MOST_READ),
            id = NOTIFICATION_ID,
            title = title,
            text = text,
            longText = null,
            lang = null,
            icon = null,
            color = color,
            bodyIntent = TopReadArticlesActivity.newIntent(
                context = context,
                card = TopReadCard(TopRead(articles = articles), age = 0, site = WikiSite.forLanguageCode(languageCode)),
                invokeSource = Constants.InvokeSource.NOTIFICATION,
                notificationCategory = NotificationCategory.WEEKLY_MOST_READ
            ),
            largeIcon = largeIcon
        )
        if (isNotificationPosted) {
            abTest.maybeSendExposureEventIfEligible()
        }
    }

    private suspend fun loadThumbnail(context: Context, thumbnailUrl: String?): Bitmap? {
        if (thumbnailUrl.isNullOrEmpty() || !Prefs.isImageDownloadEnabled) {
            return null
        }
        val request = ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .transformations(WhiteBackgroundTransformation())
            .build()
        val thumbnail = (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return null
        val side = min(thumbnail.width, thumbnail.height)
        val largeIconSize = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        return Bitmap.createBitmap(thumbnail, (thumbnail.width - side) / 2, (thumbnail.height - side) / 2, side, side)
            .scale(largeIconSize, largeIconSize)
    }

    private fun drawWikipediaLogo(context: Context, @ColorRes color: Int): Bitmap {
        val bitmapSize = DimenUtil.roundedDpToPx(FALLBACK_LARGE_ICON_SIZE_DP)
        val logoSize = DimenUtil.roundedDpToPx(FALLBACK_LARGE_ICON_LOGO_SIZE_DP)
        val logoOffset = (bitmapSize - logoSize) / 2
        val logo = ContextCompat.getDrawable(context, R.drawable.ic_wikipedia_w)!!.mutate()
        logo.setTint(ContextCompat.getColor(context, color))
        logo.setBounds(logoOffset, logoOffset, logoOffset + logoSize, logoOffset + logoSize)
        return createBitmap(bitmapSize, bitmapSize).applyCanvas { logo.draw(this) }
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
            .filterKeys { it != mainPageTitle && it !in MOST_READ_DENY_LIST && it.substringBefore(':', "") !in nonArticleNamespaces }
            .entries
            .sortedByDescending { it.value }
            .take(MAX_WEEKLY_MOST_READ_ARTICLES)
            .map { it.key }
        if (rankedTitles.isEmpty()) {
            return@coroutineScope emptyList()
        }

        val rankByTitle = rankedTitles.withIndex().associate { it.value to it.index }
        val queryResult = ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(titles = rankedTitles.joinToString("|")).query
        // Several requested titles can redirect to the same article, e.g. when it was renamed more than once during the week.
        val redirectSourcesByTarget = queryResult?.redirects.orEmpty()
            .groupBy({ StringUtil.addUnderscores(it.to.orEmpty()) }, { StringUtil.addUnderscores(it.from.orEmpty()) })
        queryResult?.pages.orEmpty()
            .asSequence()
            // Check again after redirects are resolved, since a redirect or a namespace alias (e.g. "WP:")
            // can lead to a non-article, the main page, or a denied article.
            .filter { it.pageId > 0 && it.ns == Namespace.MAIN.code() }
            .filterNot { it.articleTitle == mainPageTitle || it.articleTitle in MOST_READ_DENY_LIST }
            .distinctBy { it.pageId }
            // The API returns pages in its own order, with titles normalized and redirects resolved.
            // A page takes the best rank among its own title and every requested title that redirects to it.
            .sortedBy { page ->
                (redirectSourcesByTarget[page.articleTitle].orEmpty() + page.articleTitle).minOf { rankByTitle[it] ?: Int.MAX_VALUE }
            }
            .map {
                PageSummary(
                    displayTitle = it.displayTitle(languageCode),
                    prefixTitle = it.articleTitle,
                    description = it.description,
                    extract = null,
                    thumbnail = it.thumbUrl(),
                    lang = languageCode,
                    pageId = it.pageId
                )
            }
            .toList()
    }

    // A redirect to a section appends "#section" to the page title, which isn't part of the article's title.
    private val MwQueryPage.articleTitle get() = StringUtil.addUnderscores(StringUtil.removeSectionAnchor(title))

    private suspend fun getDailyTopArticles(wikiSite: WikiSite, age: Int): List<TopPageViews.Article> {
        val date = DateUtil.getUtcRequestDateFor(age)
        return try {
            ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getTopPageViews(wikiSite.authority(), date.year, date.month, date.day)
                .items.flatMap { it.articles }
        } catch (e: HttpStatusException) {
            // A day whose page views aren't published yet returns 404, so it's skipped.
            // Any other failure is rethrown, so that WeeklyMostReadNotificationWorker can retry.
            if (ThrowableUtil.is404(e)) emptyList() else throw e
        }
    }

    fun maybeScheduleWeeklyMostReadNotification(context: Context) {
        if (Prefs.notificationsExperimentWeeklyMostReadScheduled || !NotificationsExperimentAbTest().isNotificationsExperimentEligible()) {
            return
        }
        scheduleNotification(context, fromFiredAlarm = false)
        Prefs.notificationsExperimentWeeklyMostReadScheduled = true
    }
}
