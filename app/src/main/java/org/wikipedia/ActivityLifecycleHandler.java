package org.wikipedia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wikipedia.main.MainActivity;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.StringUtil;

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

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private boolean haveMainActivity;
    private boolean anyActivityResumed;
    public static final String LEAD_SECTION_ENDPOINT = "/page/mobile-sections-lead/";
    public static final String REMAINING_SECTIONS_ENDPOINT = "/page/mobile-sections-remaining/";
    public static final String CONVERTED_FILES_DIRECTORY_NAME = "converted-files";

    boolean haveMainActivity() {
        return haveMainActivity;
    }

    boolean isAnyActivityResumed() {
        return anyActivityResumed;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        WikipediaApp app = WikipediaApp.getInstance();
        if (activity instanceof MainActivity) {
            haveMainActivity = true;
        }
        if (Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Theme currentTheme = app.getCurrentTheme();
            switch (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if (!app.getCurrentTheme().isDark()) {
                        app.setCurrentTheme(!app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.BLACK : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if (app.getCurrentTheme().isDark()) {
                        app.setCurrentTheme(app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.LIGHT : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void runOneTimeSavedPagesConversion(Activity activity) {
        List<SavedReadingListPage> savedReadingListPages = new ArrayList<>();
        List<String> filesNamesToBeDeleted = new ArrayList<>();
        WebView dummyWebviewForConversion = new WebView(activity);
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

    private void crossCheckAndComplete(List<SavedReadingListPage> savedReadingListPages, List<String> filesNamesToBeDeleted) {
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

    private void deleteOldCacheFilesForSavedPages(boolean conversionComplete, List<String> filesNamesToBeDeleted) {
        if (!conversionComplete) {
            return;
        }
        for (String filename : filesNamesToBeDeleted) {
            FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir() + "/" + CACHE_DIR_NAME, filename));
        }
    }

    private void extractJSONsToConvert(List<SavedReadingListPage> savedReadingListPages, List<String> filesToBeDeleted) {
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

    private class SavedReadingListPage{
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

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        anyActivityResumed = true;
        if (!Prefs.isOfflinePcsToMobileHtmlConversionComplete()) {
            runOneTimeSavedPagesConversion(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        anyActivityResumed = false;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            haveMainActivity = false;
        }
    }
}
