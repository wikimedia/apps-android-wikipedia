package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.database.ReadingList;

public class ReadingListsFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppReadingLists";
    private static final int REV_ID = 15520526;

    public ReadingListsFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID);
    }

    public ReadingListsFunnel(WikiSite wiki) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, wiki);
    }

    public void logAddClick(AddToReadingListDialog.InvokeSource source) {
        log(
                "action", "addclick",
                "addsource", source.code()
        );
    }

    public void logAddToList(ReadingList list, int listCount,
                             AddToReadingListDialog.InvokeSource source) {
        log(
                "action", list.pages().isEmpty() ? "addtonew" : "addtoexisting",
                "addsource", source.code(),
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

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
