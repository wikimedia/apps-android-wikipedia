package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.settings.Prefs;

public class ReadingListsFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppReadingLists";
    private static final int REV_ID = 18118739;

    public ReadingListsFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID);
    }

    public ReadingListsFunnel(WikiSite wiki) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, wiki);
    }

    public void logAddClick(InvokeSource source) {
        log(
                "action", "addclick",
                "addsource", source.ordinal()
        );
    }

    public void logAddToList(ReadingList list, int listCount,
                             InvokeSource source) {
        log(
                "action", list.pages().isEmpty() ? "addtonew" : "addtoexisting",
                "addsource", source.ordinal(),
                "itemcount", list.pages().size(),
                "listcount", listCount
        );
    }

    public void logModifyList(ReadingList list, int listCount) {
        log(
                "action", "modifylist",
                "itemcount", list.pages().size(),
                "listcount", listCount
        );
    }

    public void logDeleteList(ReadingList list, int listCount) {
        log(
                "action", "deletelist",
                "itemcount", list.pages().size(),
                "listcount", listCount
        );
    }

    public void logDeleteItem(ReadingList list, int listCount) {
        log(
                "action", "deleteitem",
                "itemcount", list.pages().size(),
                "listcount", listCount
        );
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "synced", Prefs.isReadingListSyncEnabled());
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
