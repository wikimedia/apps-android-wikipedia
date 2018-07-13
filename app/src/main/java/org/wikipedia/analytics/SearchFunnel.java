package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.SearchInvokeSource;
import org.wikipedia.util.StringUtil;

public class SearchFunnel extends Funnel {
    /**
     * Please email someone in Discovery (Search Team's Product Manager or a Data Analyst)
     * if you change the schema name or version.
     */
    private static final String SCHEMA_NAME = "MobileWikiAppSearch";
    private static final int REVISION = 18204643;
    private SearchInvokeSource source;

    public SearchFunnel(WikipediaApp app, SearchInvokeSource source) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_100);
        this.source = source;
    }

    public void searchStart() {
        log(
                "action", "start",
                "language", StringUtil.listToJsonArrayString(getApp().language().getAppLanguageCodes())
        );
    }

    public void searchCancel(String languageCode) {
        log(
                "action", "cancel",
                "language", languageCode
        );
    }

    public void searchClick(int position, String languageCode) {
        log(
                "action", "click",
                "position", position,
                "language", languageCode
        );
    }

    public void searchDidYouMean(String languageCode) {
        log(
                "action", "didyoumean",
                "language", languageCode
        );
    }

    public void searchResults(boolean fullText, int numResults, int delayMillis, String languageCode) {
        log(
                "action", "results",
                "type_of_search", fullText ? "full" : "prefix",
                "number_of_results", numResults,
                "time_to_display_results", delayMillis,
                "language", languageCode
        );
    }

    public void searchError(boolean fullText, int delayMillis, String languageCode) {
        log(
                "action", "error",
                "type_of_search", fullText ? "full" : "prefix",
                "time_to_display_results", delayMillis,
                "language", languageCode
        );
    }

    public void searchLanguageSwitch(String previousLanguage, String currentLanguage) {
        if (!previousLanguage.equals(currentLanguage)) {
            log(
                    "action", "langswitch",
                    "language", previousLanguage + ">" + currentLanguage
            );
        }
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "invoke_source", source.code());
        return super.preprocessData(eventData);
    }
}
