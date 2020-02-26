package org.wikipedia.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.offline.OfflineObjectDbHelper;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.ByteString;

import static org.wikipedia.dataclient.RestService.REST_API_PREFIX;
import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_DIR_NAME;

public final class SavedPagesConversionUtil {
    private static final String LEAD_SECTION_ENDPOINT = "/page/mobile-sections-lead/";
    private static final String REMAINING_SECTIONS_ENDPOINT = "/page/mobile-sections-remaining/";

    @SuppressLint("StaticFieldLeak")
    private static WebView WEBVIEW;
    private static List<ReadingListPage> PAGES_TO_CONVERT = new ArrayList<>();
    private static ReadingListPage CURRENT_PAGE;

    @SuppressLint({"SetJavaScriptEnabled", "CheckResult"})
    public static void runOneTimeSavedPagesConversion() {
        List<ReadingList> allReadingLists = ReadingListDbHelper.instance().getAllLists();
        if (allReadingLists.isEmpty()) {
            Prefs.setOfflinePcsToMobileHtmlConversionComplete(true);
            return;
        }

        for (ReadingList readingList : allReadingLists) {
            for (ReadingListPage page : readingList.pages()) {
                if (!page.offline()) {
                    continue;
                }
                PAGES_TO_CONVERT.add(page);
            }
        }

        if (PAGES_TO_CONVERT.isEmpty()) {
            Prefs.setOfflinePcsToMobileHtmlConversionComplete(true);
            return;
        }

        new Handler(WikipediaApp.getInstance().getMainLooper())
                .post(SavedPagesConversionUtil::setUpWebViewForConversion);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void setUpWebViewForConversion() {
        if (WEBVIEW == null) {
            WEBVIEW = new WebView(WikipediaApp.getInstance().getApplicationContext());
            WEBVIEW.getSettings().setJavaScriptEnabled(true);
            WEBVIEW.getSettings().setAllowUniversalAccessFromFileURLs(true);
            WEBVIEW.addJavascriptInterface(new ConversionJavascriptInterface(), "conversionClient");
        }
        try {
            String html = FileUtil.readFile(WikipediaApp.getInstance().getAssets().open("offline_convert/index.html"));

            WEBVIEW.loadDataWithBaseURL("", html, "text/html", "utf-8", "");

            WEBVIEW.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    File file = new File(WikipediaApp.getInstance().getFilesDir(), OfflineObjectDbHelper.OFFLINE_PATH);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    postNextPage();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConversionJavascriptInterface {
        @JavascriptInterface
        public synchronized void onReceiveHtml(String html) {
            storeConvertedFile(html);

            //postNextPage();

        }
    }

    private static void postNextPage() {
        new Handler(WikipediaApp.getInstance().getMainLooper())
                .post(SavedPagesConversionUtil::convertNextPage);
    }

    private static void convertNextPage() {
        if (PAGES_TO_CONVERT.isEmpty()) {
            onConversionComplete();
            return;
        }

        CURRENT_PAGE = PAGES_TO_CONVERT.remove(0);

        String baseUrl = CURRENT_PAGE.wiki().url();
        String title = CURRENT_PAGE.apiTitle();

        String leadSectionUrl = baseUrl + REST_API_PREFIX + LEAD_SECTION_ENDPOINT + title;
        String remainingSectionsUrl = baseUrl + REST_API_PREFIX + REMAINING_SECTIONS_ENDPOINT + title;

        // Do the cache files exist for this page?
        File offlineCacheDir = new File(WikipediaApp.getInstance().getFilesDir(), CACHE_DIR_NAME);

        File leadJSONFile = new File(offlineCacheDir, ByteString.encodeUtf8(leadSectionUrl).md5().hex() + ".1");
        File remJSONFile = new File(offlineCacheDir, ByteString.encodeUtf8(remainingSectionsUrl).md5().hex() + ".1");

        if (!leadJSONFile.exists() || !remJSONFile.exists()) {
            postNextPage();
            return;
        }

        try {
            String leadJSON = FileUtil.readFile(new FileInputStream(leadJSONFile));
            String remainingSectionsJSON = FileUtil.readFile(new FileInputStream(remJSONFile));

            String restPrefix = CURRENT_PAGE.wiki().url() + "/api/rest_v1/";

            WEBVIEW.evaluateJavascript("PCSHTMLConverter.convertMobileSectionsJSONToMobileHTML("
                            + leadJSON + ","
                            + remainingSectionsJSON + ","
                            + "\"" + CURRENT_PAGE.wiki().authority() + "\"" + ","
                            + "\"" + restPrefix + "\""
                            + ")",
                    value -> {
                    });

        } catch (IOException e) {
            e.printStackTrace();
            postNextPage();
        }
    }

    private static void onConversionComplete() {
        Prefs.setOfflinePcsToMobileHtmlConversionComplete(true);

        FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir(), CACHE_DIR_NAME));
    }

    private static void storeConvertedFile(String html) {
        String baseUrl = CURRENT_PAGE.wiki().url();
        String title = CURRENT_PAGE.apiTitle();
        String mobileHtmlUrl = baseUrl + REST_API_PREFIX + RestService.PAGE_HTML_ENDPOINT + UriUtil.encodeURL(title);

        OfflineCacheInterceptor.createCacheItemFor(CURRENT_PAGE, mobileHtmlUrl, html, "text/html");
    }

    private SavedPagesConversionUtil() {
    }
}
