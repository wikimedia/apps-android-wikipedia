package org.wikipedia.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.RemoteViews;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.log.L;

import static org.wikipedia.util.UriUtil.decodeURL;

public class WidgetProviderFeaturedPage extends AppWidgetProvider {
    private static final String TAG = "WidgetFeatured";

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetProviderFeaturedPage.class);
        final int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        getMainPageLead(new Callback() {
            @Override
            public void onFeaturedArticleReceived(final String titleText) {
                for (final int widgetId : allWidgetIds) {
                    Log.d(TAG, "updating widget...");
                    final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                            R.layout.widget_featured_page);

                    if (!TextUtils.isEmpty(titleText)) {
                        remoteViews.setTextViewText(R.id.widget_content_text, titleText);
                    }
                    appWidgetManager.updateAppWidget(widgetId, remoteViews);

                    // Create a PendingIntent to act as the onClickListener
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(MainActivity.EXTRA_FEATURED_ARTICLE_FROM_WIDGET, true);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    // If we want to update the widget itself from the click event, then do something like this:
                    //Intent intent = new Intent(context, WidgetProviderFeaturedPage.class);
                    //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    //PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
            }
        });
    }

    private void getMainPageLead(final Callback cb) {
        WikipediaApp app = WikipediaApp.getInstance();
        final PageTitle title = new PageTitle(
                MainPageNameData.valueFor(app.getAppOrSystemLanguageCode()),
                app.getSite());

        getApiService(title).pageLead(title.getPrefixedText(), calculateLeadImageWidth(),
                !app.isImageDownloadEnabled(), new PageLead.Callback() {
                    @Override
                    public void success(PageLead pageLead) {
                        if (pageLead.hasError()) {
                            pageLead.logError("Error while updating widget");
                            return;
                        }

                        L.d("Downloaded page " + title.getDisplayText());
                        String titleText = findFeaturedArticleTitle(pageLead.getLeadSectionContent());

                        cb.onFeaturedArticleReceived(titleText);
                    }

                    @Override
                    public void failure(Throwable error) {
                        L.e("Error while updating widget: " + error);
                    }
                });
    }

    private String findFeaturedArticleTitle(String pageLeadContent) {
        // Extract the actual link to the featured page in a hacky way (until we
        // have the correct API for it):
        // Parse the HTML, and look for the first link, which should be the
        // article of the day.
        Spanned text = Html.fromHtml(pageLeadContent);
        URLSpan[] spans = text.getSpans(0, text.length(), URLSpan.class);
        String titleText = "";
        for (URLSpan span : spans) {
            if (!span.getURL().startsWith("/wiki/")
                    || (text.getSpanEnd(span) - text.getSpanStart(span) <= 1)) {
                continue;
            }
            PageTitle title = WikipediaApp.getInstance().getSite()
                    .titleForInternalLink(decodeURL(span.getURL()));
            if (!title.isFilePage() && !title.isSpecial()) {
                titleText = title.getDisplayText();
                break;
            }
        }
        return titleText;
    }

    private int calculateLeadImageWidth() {
        Resources res = WikipediaApp.getInstance().getResources();
        return (int) (res.getDimension(R.dimen.leadImageWidth) / DimenUtil.getDensityScalar());
    }

    private PageService getApiService(PageTitle title) {
        return PageServiceFactory.create(title.getSite());
    }

    private interface Callback {
        void onFeaturedArticleReceived(String titleText);
    }
}
