package org.wikipedia.util;

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wikipedia.WikipediaApp;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okio.ByteString;

import static org.wikipedia.dataclient.RestService.REST_API_PREFIX;
import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_DIR_NAME;
import static org.wikipedia.util.StringUtil.hasSpecialCharacters;

public final class SavedPagesConversionUtil {
    private static final String LEAD_SECTION_ENDPOINT = "/page/mobile-sections-lead/";
    private static final String REMAINING_SECTIONS_ENDPOINT = "/page/mobile-sections-remaining/";
    public static final String CONVERTED_FILES_DIRECTORY_NAME = "converted-files";

    private static List<SavedReadingListPage> PAGES_TO_CONVERT = new ArrayList<>();
    private static AtomicInteger FILE_COUNT = new AtomicInteger();

    @SuppressLint({"SetJavaScriptEnabled", "CheckResult"})
    public static void runOneTimeSavedPagesConversion() {
        List<ReadingList> allReadingLists = ReadingListDbHelper.instance().getAllLists();
        if (allReadingLists.isEmpty()) {
            Prefs.setOfflinePcsToMobileHtmlConversionComplete(true);
            return;
        }

        for (ReadingList readingList : allReadingLists) {
            for (ReadingListPage page : readingList.pages()) {
                if (page.offline()) {
                    String baseUrl = page.wiki().url();
                    String title = hasSpecialCharacters(page.apiTitle()) ? UriUtil.encodeURL(page.apiTitle()) : page.apiTitle();
                    String leadSectionUrl = baseUrl + REST_API_PREFIX + LEAD_SECTION_ENDPOINT + title;
                    String remainingSectionsUrl = baseUrl + REST_API_PREFIX + REMAINING_SECTIONS_ENDPOINT + title;
                    PAGES_TO_CONVERT.add(new SavedReadingListPage(StringUtil.fromHtml(page.apiTitle()).toString(), baseUrl, leadSectionUrl, remainingSectionsUrl));
                }
            }
        }

        if (PAGES_TO_CONVERT.isEmpty()) {
            Prefs.setOfflinePcsToMobileHtmlConversionComplete(true);
            return;
        }

        Completable.fromAction(() -> recordJSONFileNames(PAGES_TO_CONVERT)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(SavedPagesConversionUtil::setUpWebViewForConversion, L::e);

    }

    private static void recordJSONFileNames(List<SavedReadingListPage> savedReadingListPages) {
        File offlineCacheDirectory = new File(WikipediaApp.getInstance().getFilesDir(), CACHE_DIR_NAME);
        String leadJSON;
        String remainingSectionsJSON;

        for (SavedReadingListPage savedReadingListPage : savedReadingListPages) {
            File leadJSONFile = new File(offlineCacheDirectory, ByteString.encodeUtf8(savedReadingListPage.getLeadSectionUrl()).md5().hex() + ".1");
            File remJSONFile = new File(offlineCacheDirectory, ByteString.encodeUtf8(savedReadingListPage.getRemainingSectionsUrl()).md5().hex() + ".1");
            try {
                leadJSON = FileUtil.readFile(new FileInputStream(leadJSONFile));
                remainingSectionsJSON = FileUtil.readFile(new FileInputStream(remJSONFile));
                savedReadingListPage.setLeadSectionJSON(leadJSON);
                savedReadingListPage.setRemainingSectionsJSON(remainingSectionsJSON);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void setUpWebViewForConversion() {
        WebView dummyWebviewForConversion = new WebView(WikipediaApp.getInstance().getApplicationContext());
        dummyWebviewForConversion.getSettings().setJavaScriptEnabled(true);
        dummyWebviewForConversion.getSettings().setAllowUniversalAccessFromFileURLs(true);
        dummyWebviewForConversion.addJavascriptInterface(new ConversionJavascriptInterface(dummyWebviewForConversion), "conversionClient");

        try {
            String html = FileUtil.readFile(WikipediaApp.getInstance().getAssets().open("pcs-html-converter/index.html"));

            dummyWebviewForConversion.loadDataWithBaseURL("", html, "text/html", "utf-8", "");

            dummyWebviewForConversion.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    File file = new File(WikipediaApp.getInstance().getFilesDir(), CONVERTED_FILES_DIRECTORY_NAME);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    convertToMobileHtml(dummyWebviewForConversion);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConversionJavascriptInterface {
        private WebView webView;
        ConversionJavascriptInterface(WebView webView) {
            this.webView = webView;
        }
        @JavascriptInterface
        public synchronized void onReceiveHtml(String html) {
            storeConvertedFile(html, PAGES_TO_CONVERT.get(FILE_COUNT.get()).title);
            if (FILE_COUNT.incrementAndGet() == PAGES_TO_CONVERT.size()) {
                crossCheckAndComplete();
            } else {
                convertToMobileHtml(webView);
            }
        }
    }

    private static void convertToMobileHtml(WebView dummyWebviewForConversion) {
        SavedReadingListPage savedReadingListPage = PAGES_TO_CONVERT.get(FILE_COUNT.get());
        String restPrefix = savedReadingListPage.baseUrl + "/api/rest_v1/";

        dummyWebviewForConversion.evaluateJavascript("PCSHTMLConverter.convertMobileSectionsJSONToMobileHTML(" + savedReadingListPage.getLeadSectionJSON() + "," + savedReadingListPage.getRemainingSectionsJSON() + "," + "\"" + StringUtil.removeNamespace(savedReadingListPage.getBaseUrl()).replace("//", "") + "\"" + "," + "\"" + restPrefix + "\"" + ")",
                value -> { });
    }

    @SuppressLint("CheckResult")
    private static void storeConvertedFile(String convertedString, String fileName) {
        Completable.fromAction(() -> FileUtil.writeToFileInDirectory(convertedString, WikipediaApp.getInstance().getFilesDir() + "/" + CONVERTED_FILES_DIRECTORY_NAME, fileName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, L::e);
    }

    private static void crossCheckAndComplete() {
        List<String> fileNames = new ArrayList<>();
        boolean conversionComplete = true;
        File convertedCacheDirectory = new File(WikipediaApp.getInstance().getFilesDir(), CONVERTED_FILES_DIRECTORY_NAME);
        if (convertedCacheDirectory.exists()) {
            File[] files = convertedCacheDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
            }
        }
        for (SavedReadingListPage savedReadingListPage : PAGES_TO_CONVERT) {
            if (!fileNames.contains(savedReadingListPage.title)) {
                conversionComplete = false;
            }
        }
        Prefs.setOfflinePcsToMobileHtmlConversionComplete(conversionComplete);
        deleteOldCacheFilesForSavedPages(conversionComplete);
    }

    private static void deleteOldCacheFilesForSavedPages(boolean conversionComplete) {
        if (!conversionComplete) {
            return;
        }
        for (SavedReadingListPage savedReadingListPage : PAGES_TO_CONVERT) {
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, savedReadingListPage.getLeadSectionJSON()));
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, savedReadingListPage.getLeadSectionJSON().substring(0, savedReadingListPage.getLeadSectionJSON().length() - 1) + "0"));
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, savedReadingListPage.getRemainingSectionsJSON()));
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, savedReadingListPage.getRemainingSectionsJSON().substring(0, savedReadingListPage.getRemainingSectionsJSON().length() - 1) + "0"));
        }
    }

    private static class SavedReadingListPage {
        String title;
        String baseUrl;
        String leadSectionUrl;
        String remainingSectionsUrl;
        String leadSectionJSON;
        String remainingSectionsJSON;

        SavedReadingListPage(String title, String baseUrl, String leadSectionUrl, String remainingSectionsUrl) {
            this.title = title;
            this.baseUrl = baseUrl;
            this.leadSectionUrl = leadSectionUrl;
            this.remainingSectionsUrl = remainingSectionsUrl;
        }

        String getLeadSectionJSON() {
            return leadSectionJSON;
        }

        void setLeadSectionJSON(String leadSectionJSON) {
            this.leadSectionJSON = leadSectionJSON;
        }

        String getRemainingSectionsJSON() {
            return remainingSectionsJSON;
        }

        void setRemainingSectionsJSON(String remainingSectionsJSON) {
            this.remainingSectionsJSON = remainingSectionsJSON;
        }

        String getLeadSectionUrl() {
            return leadSectionUrl;
        }

        String getRemainingSectionsUrl() {
            return remainingSectionsUrl;
        }

        String getBaseUrl() {
            return baseUrl;
        }
    }

    private SavedPagesConversionUtil() {
    }
}
