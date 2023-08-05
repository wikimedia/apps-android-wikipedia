package org.wikipedia.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.style.URLSpan
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.text.getSpans
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwParseResponse
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

class WidgetProviderFeaturedPage : AppWidgetProvider() {

    private interface Callback {
        fun onFeaturedArticleReceived(pageTitle: PageTitle, widgetText: CharSequence)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderFeaturedPage::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        getFeaturedArticleInformation(object : Callback {
            override fun onFeaturedArticleReceived(pageTitle: PageTitle, widgetText: CharSequence) {
                for (widgetId in allWidgetIds) {
                    L.d("updating widget...")
                    val remoteViews = RemoteViews(context.packageName, R.layout.widget_featured_page)
                    if (widgetText.isNotEmpty()) {
                        remoteViews.setTextViewText(R.id.widget_content_text, widgetText)
                    }
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)

                    val intent = Intent(context, PageActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(Constants.ARG_TITLE, pageTitle)
                    intent.putExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET, true)
                    val pendingIntent = PendingIntentCompat.getActivity(context, 1, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT, false)

                    remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        })
    }

    private fun getFeaturedArticleInformation(cb: Callback) {
        val app = WikipediaApp.instance
        val mainPageTitle = PageTitle(
                MainPageNameData.valueFor(app.appOrSystemLanguageCode),
                app.wikiSite)
        val (year, month, day) = DateUtil.getYearMonthAndDayForAge(0)
        ServiceFactory.getRest(WikipediaApp.instance.wikiSite)
            .getAggregatedFeed(year, month, day)
                .flatMap { response: AggregatedFeedContent ->
                    if (response.tfa != null) {
                        Observable.just(response.tfa)
                    } else {
                        // TODO: this logic can be removed if the feed API can return the featured article for all languages.
                        ServiceFactory.get(mainPageTitle.wikiSite).parseTextForMainPage(mainPageTitle.prefixedText)
                    }
                }
                .subscribeOn(Schedulers.io())
                .flatMap { response ->
                    if (response is MwParseResponse) {
                        L.d("Downloaded page " + mainPageTitle.displayText)
                        ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getSummary(null, findFeaturedArticleTitle(response.text))
                    } else {
                        Observable.just(response as PageSummary)
                    }
                }
                .subscribe({ response ->
                    val widgetText = StringUtil.fromHtml(response.displayTitle)
                    val pageTitle = response.getPageTitle(app.wikiSite)
                    cb.onFeaturedArticleReceived(pageTitle, widgetText)
                }) { throwable ->
                    cb.onFeaturedArticleReceived(mainPageTitle, mainPageTitle.displayText)
                    L.e(throwable)
                }
    }

    private fun findFeaturedArticleTitle(pageLeadContent: String): String {
        // Extract the actual link to the featured page in a hacky way (until we
        // have the correct API for it):
        // Parse the HTML, and look for the first link, which should be the
        // article of the day.
        val text = StringUtil.fromHtml(pageLeadContent)
        val spans = text.getSpans<URLSpan>()
        var titleText = ""
        for (span in spans) {
            if (!span.url.startsWith("/wiki/") ||
                    text.getSpanEnd(span) - text.getSpanStart(span) <= 1) {
                continue
            }
            val title = PageTitle.titleForInternalLink(UriUtil.decodeURL(span.url), WikiSite(span.url))
            if (!title.isFilePage && !title.isSpecial) {
                titleText = title.displayText
                break
            }
        }
        return titleText
    }

    companion object {
        fun forceUpdateWidget(context: Context) {
            val intent = Intent(context, WidgetProviderFeaturedPage::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context.applicationContext)
                    .getAppWidgetIds(ComponentName(context.applicationContext, WidgetProviderFeaturedPage::class.java))
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
