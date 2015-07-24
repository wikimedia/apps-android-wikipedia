package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

import java.util.List;

public class SuggestedPagesFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppArticleSuggestions";
    private static final int REV_ID = 12443791;
    private static final int READ_MORE_SOURCE_FULL_TEXT = 0;
    private static final int READ_MORE_SOURCE_MORELIKE = 1;

    private boolean moreLikeSearchEnabled;

    public SuggestedPagesFunnel(WikipediaApp app, boolean moreLikeSearchEnabled) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100);

        this.moreLikeSearchEnabled = moreLikeSearchEnabled;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "readMoreSource", moreLikeSearchEnabled
                ? READ_MORE_SOURCE_MORELIKE
                : READ_MORE_SOURCE_FULL_TEXT);
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void logSuggestionsShown(PageTitle currentPageTitle, List<PageTitle> suggestedTitles) {
        log(
                currentPageTitle.getSite(),
                "action", "shown",
                "pageTitle", currentPageTitle.getDisplayText(),
                "readMoreList", TextUtils.join("|", suggestedTitles)
        );
    }

    public void logSuggestionClicked(PageTitle currentPageTitle, List<PageTitle> suggestedTitles,
                                     int clickedIndex) {
        log(
                currentPageTitle.getSite(),
                "action", "clicked",
                "pageTitle", currentPageTitle.getDisplayText(),
                "readMoreList", TextUtils.join("|", suggestedTitles),
                "readMoreIndex", clickedIndex
        );
    }
}
