package org.wikipedia.history

import io.reactivex.rxjava3.functions.Action
import org.wikipedia.WikipediaApp
import org.wikipedia.database.DatabaseClient
import org.wikipedia.database.contract.PageHistoryContract

/**
 * Save the history entry for the specified page.
 */
class UpdateHistoryTask(private val entry: HistoryEntry) : Action {
    override fun run() {
        val client = WikipediaApp.getInstance().getDatabaseClient(HistoryEntry::class.java)
        client.upsert(HistoryEntry(entry.title, entry.timestamp, entry.source,
                entry.timeSpentSec + getPreviousTimeSpent(client)),
                PageHistoryContract.Page.SELECTION)
    }

    private fun getPreviousTimeSpent(client: DatabaseClient<HistoryEntry>): Int {
        var timeSpent = 0
        val selection = ":siteCol == ? and :langCol == ? and :apiTitleCol == ?"
                .replace(":siteCol".toRegex(), PageHistoryContract.Page.SITE.qualifiedName())
                .replace(":langCol".toRegex(), PageHistoryContract.Page.LANG.qualifiedName())
                .replace(":apiTitleCol".toRegex(), PageHistoryContract.Page.API_TITLE.qualifiedName())
        val selectionArgs = arrayOf(entry.title.wikiSite.authority(),
                entry.title.wikiSite.languageCode(), entry.title.text)
        val cursor = client.select(selection, selectionArgs, null)
        if (cursor.moveToFirst()) {
            timeSpent = PageHistoryContract.Col.TIME_SPENT.`val`(cursor)
        }
        cursor.close()
        return timeSpent
    }
}
