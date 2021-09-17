package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs

class ReadingListsFunnel : Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID) {

    fun logAddClick(source: InvokeSource) {
        log("action", "addclick", "addsource", source.ordinal)
    }

    fun logAddToList(list: ReadingList, listCount: Int,
                     source: InvokeSource) {
        log(
                "action", if (list.pages.isEmpty()) "addtonew" else "addtoexisting",
                "addsource", source.ordinal,
                "itemcount", list.pages.size,
                "listcount", listCount
        )
    }

    fun logMoveClick(source: InvokeSource) {
        log("action", "moveclick", "addsource", source.ordinal)
    }

    fun logMoveToList(list: ReadingList, listCount: Int,
                      source: InvokeSource) {
        log(
                "action", if (list.pages.isEmpty()) "movetonew" else "movetoexisting",
                "addsource", source.ordinal,
                "itemcount", list.pages.size,
                "listcount", listCount
        )
    }

    fun logModifyList(list: ReadingList, listCount: Int) {
        log("action", "modifylist", "itemcount", list.pages.size, "listcount", listCount)
    }

    fun logDeleteList(list: ReadingList, listCount: Int) {
        log("action", "deletelist", "itemcount", list.pages.size, "listcount", listCount)
    }

    fun logDeleteItem(list: ReadingList, listCount: Int) {
        log("action", "deleteitem", "itemcount", list.pages.size, "listcount", listCount)
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "synced", Prefs.isReadingListSyncEnabled)
        return super.preprocessData(eventData)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppReadingLists"
        private const val REV_ID = 20339451
    }
}
