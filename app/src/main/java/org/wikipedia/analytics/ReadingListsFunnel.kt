package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs

class ReadingListsFunnel : Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID) {

    fun logAddClick(source: InvokeSource) {
        log("action" to "addclick", "addsource" to source.ordinal)
    }

    fun logAddToList(list: ReadingList, listCount: Int,
                     source: InvokeSource) {
        log(
            "action" to if (list.pages.isEmpty()) "addtonew" else "addtoexisting",
            "addsource" to source.ordinal,
            "itemcount" to list.pages.size,
            "listcount" to listCount
        )
    }

    fun logMoveClick(source: InvokeSource) {
        log("action" to "moveclick", "addsource" to source.ordinal)
    }

    fun logMoveToList(list: ReadingList, listCount: Int, source: InvokeSource) {
        log(
            "action" to if (list.pages.isEmpty()) "movetonew" else "movetoexisting",
            "addsource" to source.ordinal,
            "itemcount" to list.pages.size,
            "listcount" to listCount
        )
    }

    fun logModifyList(list: ReadingList, listCount: Int) {
        log("action" to "modifylist", "itemcount" to list.pages.size, "listcount" to listCount)
    }

    fun logDeleteList(list: ReadingList, listCount: Int) {
        log("action" to "deletelist", "itemcount" to list.pages.size, "listcount" to listCount)
    }

    fun logDeleteItem(list: ReadingList, listCount: Int) {
        log("action" to "deleteitem", "itemcount" to list.pages.size, "listcount" to listCount)
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
