package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MergeWithOtherReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
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

    public void logAddClick(AddToReadingListDialog.InvokeSource source) {
        log(
                "action", "addclick",
                "addsource", source.code()
        );
    }

    public void logMoveClick(MoveToReadingListDialog.InvokeSource source) {
        // TODO: Add the appropriate parameters.
        /*log(
                "action", "addclick",
                "addsource", source.code()
        );*/
    }

    public void logMergeClick(MergeWithOtherReadingListDialog.InvokeSource source) {
        // TODO
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

    public void logMoveToList(ReadingList fromList, ReadingList toList,
                              int listCount, MoveToReadingListDialog.InvokeSource source) {
        // TODO: Add the appropriate parameters.
        /*log(
                "action", list.pages().isEmpty() ? "addtonew" : "addtoexisting",
                "addsource", source.code(),
                "itemcount", list.pages().size(),
                "listcount", listCount
        );*/
    }

    public void logMergeWithList(ReadingList fromList, ReadingList toList,
                                 MergeWithOtherReadingListDialog.InvokeSource source) {
        // TODO
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
