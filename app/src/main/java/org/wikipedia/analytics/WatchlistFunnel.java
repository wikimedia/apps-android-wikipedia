package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

import java.util.List;

public class WatchlistFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppWatchlist";
    private static final int REV_ID = 20936401;

    public WatchlistFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
    }

    public void logAction(String action) {
        log("action", action);
    }

    public void logShowTooltipMore() {
        logAction("show_tooltip_more");
    }

    public void logShowTooltip() {
        logAction("show_tooltip");
    }

    public void logAddArticle() {
        logAction("add_article");
    }

    public void logAddExpiry() {
        logAction("add_expiry");
    }

    public void logAddSuccess() {
        logAction("add_success");
    }

    public void logViewWatchlist() {
        logAction("view_watchlist");
    }

    public void logOpenWatchlist() {
        logAction("open_watchlist");
    }

    public void logRemoveArticle() {
        logAction("remove_article");
    }

    public void logRemoveSuccess() {
        logAction("remove_success");
    }

    public void logChangeLanguage(@NonNull List<String> languagesList) {
        log(
                "action", "change_language",
                "languages", StringUtil.listToJsonArrayString(languagesList)
        );
    }
}
