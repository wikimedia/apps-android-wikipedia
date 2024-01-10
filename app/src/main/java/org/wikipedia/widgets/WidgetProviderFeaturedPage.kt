package org.wikipedia.widgets

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.style.URLSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.text.getSpans
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
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
        fun onFeaturedArticleReceived(pageTitle: PageTitle)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        val thisWidget = ComponentName(context, WidgetProviderFeaturedPage::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        getFeaturedArticleInformation(object : Callback {
            override fun onFeaturedArticleReceived(pageTitle: PageTitle) {
                for (widgetId in allWidgetIds) {
                    L.d("updating widget...")
                    val remoteViews = RemoteViews(context.packageName, R.layout.widget_featured_page)

                    remoteViews.setTextViewText(R.id.widget_content_title, StringUtil.fromHtml(pageTitle.displayText))
                    if (pageTitle.description.isNullOrEmpty()) {
                        remoteViews.setViewVisibility(R.id.widget_content_description, View.GONE)
                    } else {
                        remoteViews.setTextViewText(R.id.widget_content_description, pageTitle.description)
                        remoteViews.setViewVisibility(R.id.widget_content_description, View.VISIBLE)
                    }
                    if (pageTitle.thumbUrl.isNullOrEmpty()) {
                        remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.GONE)
                    } else {
                        val widgetTarget = object : AppWidgetTarget(context, R.id.widget_content_thumbnail, remoteViews, widgetId) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                super.onResourceReady(resource, transition)
                                appWidgetManager.updateAppWidget(widgetId, remoteViews)
                            }
                        }

                        Glide.with(context.applicationContext)
                            .asBitmap()
                            .load(pageTitle.thumbUrl)
                            .into(widgetTarget)

                        remoteViews.setViewVisibility(R.id.widget_content_thumbnail, View.VISIBLE)
                    }

                    appWidgetManager.updateAppWidget(widgetId, remoteViews)

                    val pendingIntent = PendingIntentCompat.getActivity(context, 1,
                        Intent(context, PageActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Constants.ARG_TITLE, pageTitle)
                            .putExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET, true),
                            PendingIntent.FLAG_UPDATE_CURRENT, false)

                    remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun getFeaturedArticleInformation(cb: Callback) {
        val app = WikipediaApp.instance
        val mainPageTitle = PageTitle(
                MainPageNameData.valueFor(app.appOrSystemLanguageCode),
                app.wikiSite)
        val date = DateUtil.getUtcRequestDateFor(-1)
        ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getAggregatedFeed(date.year, date.month, date.day)
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
                    val pageTitle = response.getPageTitle(app.wikiSite)
                    pageTitle.displayText = response.displayTitle
                    cb.onFeaturedArticleReceived(pageTitle)
                }) { throwable ->
                    cb.onFeaturedArticleReceived(mainPageTitle)
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
