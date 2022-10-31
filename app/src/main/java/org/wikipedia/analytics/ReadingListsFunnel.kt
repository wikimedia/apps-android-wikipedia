package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs

class ReadingListsFunnel : Funnel(WikipediaApp.instance, SCHEMA_NAME, REV_ID) {

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

    fun logShareList(list: ReadingList) {
        log("action", "share", "itemcount", list.pages.size)
    }

    fun logExportList(list: ReadingList, listCount: Int) {
        log("action", "export", "itemcount", list.pages.size, "listcount", listCount)
    }

    fun logReceiveStart() {
        log("action", "receive_start")
    }

    fun logReceivePreview(list: ReadingList) {
        log("action", "receive_preview", "itemcount", list.pages.size)
    }

    fun logReceiveCancel(list: ReadingList) {
        log("action", "receive_cancel", "itemcount", list.pages.size)
    }

    fun logReceiveFinish(list: ReadingList) {
        log("action", "receive_finish", "itemcount", list.pages.size)
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "synced", Prefs.isReadingListSyncEnabled)
        return super.preprocessData(eventData)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppReadingLists"
        private const val REV_ID = 23999552
    }
}
