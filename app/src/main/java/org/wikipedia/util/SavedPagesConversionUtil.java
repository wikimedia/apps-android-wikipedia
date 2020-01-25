package org.wikipedia.util;

import android.annotation.SuppressLint;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wikipedia.dataclient.RestService.REST_API_PREFIX;
import static org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.CACHE_DIR_NAME;

public final class SavedPagesConversionUtil {
    public static final String LEAD_SECTION_ENDPOINT = "/page/mobile-sections-lead/";
    public static final String REMAINING_SECTIONS_ENDPOINT = "/page/mobile-sections-remaining/";
    public static final String CONVERTED_FILES_DIRECTORY_NAME = "converted-files";

    @SuppressLint("SetJavaScriptEnabled")
    public static void runOneTimeSavedPagesConversion() {
        List<SavedReadingListPage> savedReadingListPages = new ArrayList<>();
        List<String> filesNamesToBeDeleted = new ArrayList<>();
        WebView dummyWebviewForConversion = new WebView(WikipediaApp.getInstance().getApplicationContext());
        dummyWebviewForConversion.getSettings().setJavaScriptEnabled(true);
        dummyWebviewForConversion.getSettings().setAllowUniversalAccessFromFileURLs(true);
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

                    List<ReadingList> allReadingLists = ReadingListDbHelper.instance().getAllLists();
                    AtomicInteger fileCount = new AtomicInteger();
                    for (ReadingList readingList : allReadingLists) {
                        for (ReadingListPage page : readingList.pages()) {
                            String leadSectionUrl = page.wiki().url() + REST_API_PREFIX + LEAD_SECTION_ENDPOINT + StringUtil.fromHtml(page.apiTitle());
                            String remainingSectionsUrl = page.wiki().url() + REST_API_PREFIX + REMAINING_SECTIONS_ENDPOINT + StringUtil.fromHtml(page.apiTitle());
                            savedReadingListPages.add(new SavedReadingListPage(StringUtil.fromHtml(page.apiTitle()).toString(), leadSectionUrl, remainingSectionsUrl));
                        }
                    }
                    extractJSONsToConvert(savedReadingListPages, filesNamesToBeDeleted);

                    for (SavedReadingListPage savedReadingListPage : savedReadingListPages) {
                        dummyWebviewForConversion.evaluateJavascript("PCSHTMLConverter.convertMobileSectionsJSONToMobileHTML(" + savedReadingListPage.getLeadSectionJSON() + "," + savedReadingListPage.getRemainingSectionsJSON() + ")",
                                value -> {
                                    FileUtil.writeToFileInDirectory(StringEscapeUtils.unescapeJava(value), WikipediaApp.getInstance().getFilesDir() + "/" + CONVERTED_FILES_DIRECTORY_NAME, savedReadingListPage.title);
                                    if (fileCount.incrementAndGet() == savedReadingListPages.size()) {
                                        crossCheckAndComplete(savedReadingListPages, filesNamesToBeDeleted);
                                    }
                                });
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void crossCheckAndComplete(List<SavedReadingListPage> savedReadingListPages, List<String> filesNamesToBeDeleted) {
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
        for (SavedReadingListPage savedReadingListPage : savedReadingListPages) {
            if (!fileNames.contains(savedReadingListPage.title)) {
                conversionComplete = false;
            }
        }
        Prefs.setOfflinePcsToMobileHtmlConversionComplete(conversionComplete);
        deleteOldCacheFilesForSavedPages(conversionComplete, filesNamesToBeDeleted);
    }

    public static void deleteOldCacheFilesForSavedPages(boolean conversionComplete, List<String> filesNamesToBeDeleted) {
        if (!conversionComplete) {
            return;
        }
        for (String filename : filesNamesToBeDeleted) {
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, filename));
        }
    }

    public static void extractJSONsToConvert(List<SavedReadingListPage> savedReadingListPages, List<String> filesToBeDeleted) {
        File offlineCacheDirectory = new File(WikipediaApp.getInstance().getFilesDir(), CACHE_DIR_NAME);
        if (offlineCacheDirectory.exists()) {
            File[] files = offlineCacheDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    BufferedReader bufferedReader = null;
                    try {
                        bufferedReader = new BufferedReader(new FileReader(file));
                        String firstLine = bufferedReader.readLine();
                        for (SavedReadingListPage page : savedReadingListPages) {
                            String resultFileName = file.getName().substring(0, file.getName().length() - 1) + '1';
                            File resultFile = new File(offlineCacheDirectory, resultFileName);

                            if (firstLine.trim().contains(page.leadSectionUrl)) {
                                String leadJSON = FileUtil.readFile(new FileInputStream(resultFile));
                                page.setLeadSectionJSON(leadJSON);
                                filesToBeDeleted.add(file.getName());
                                filesToBeDeleted.add(resultFileName);
                            }

                            if (firstLine.trim().contains(page.remainingSectionsUrl)) {
                                String remainingSectionsJSON = FileUtil.readFile(new FileInputStream(resultFile));
                                page.setRemainingSectionsJSON(remainingSectionsJSON);
                                filesToBeDeleted.add(file.getName());
                                filesToBeDeleted.add(resultFileName);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static class SavedReadingListPage {
        String title;
        String leadSectionUrl;
        String remainingSectionsUrl;
        String leadSectionJSON;
        String remainingSectionsJSON;

        SavedReadingListPage(String title, String leadSectionUrl, String remainingSectionsUrl) {
            this.title = title;
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
    }

    private SavedPagesConversionUtil() {
    }
}
