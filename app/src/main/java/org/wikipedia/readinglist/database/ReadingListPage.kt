package org.wikipedia.readinglist.database

import android.text.TextUtils
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import java.io.Serializable
import java.util.*

data class ReadingListPage(var listId: Long = -1,
                           val wiki: WikiSite,
                           val namespace: Namespace,
                           var displayTitle: String,
                           val apiTitle: String,
                           var description: String? = null,
                           var thumbUrl: String? = null,
                           var id: Long = 0,
                           var mtime: Long = 0,
                           var atime: Long = 0,
                           var offline: Boolean = false,
                           var status: Long = 0,
                           var sizeBytes: Long = 0,
                           var lang: String = "en",
                           var revId: Long = 0,
                           var remoteId: Long = 0) : Serializable {

    constructor(wiki: WikiSite, namespace: Namespace, displayTitle: String, apiTitle: String, listId: Long) :
            this(listId, wiki, namespace, displayTitle, if (apiTitle.isEmpty()) displayTitle else apiTitle)

    constructor(title: PageTitle) :
            this( -1, title.wikiSite, title.namespace(), title.displayText, title.prefixedText, title.description, title.thumbUrl) {
        val now = System.currentTimeMillis()
        mtime = now
        atime = now
        status = STATUS_QUEUE_FOR_SAVE
    }

    @Transient
    var downloadProgress = 0

    @Transient
    private var selected = false

    @Transient
    private var accentAndCaseInvariantTitle: String? = null


    fun accentAndCaseInvariantTitle(): String {
        if (accentAndCaseInvariantTitle == null) {
            accentAndCaseInvariantTitle = StringUtils.stripAccents(displayTitle).toLowerCase(Locale.getDefault())
        }
        return accentAndCaseInvariantTitle as String
    }

    fun touch() {
        atime = System.currentTimeMillis()
    }

    fun saving(): Boolean {
        return offline && (status == STATUS_QUEUE_FOR_SAVE || status == STATUS_QUEUE_FOR_FORCED_SAVE)
    }

    companion object {
        const val STATUS_QUEUE_FOR_SAVE = 0L
        const val STATUS_SAVED = 1L
        const val STATUS_QUEUE_FOR_DELETE = 2L
        const val STATUS_QUEUE_FOR_FORCED_SAVE = 3L

        @JvmField
        val DATABASE_TABLE = ReadingListPageTable()

        @JvmStatic
        fun toPageSummary(page: ReadingListPage): PageSummary {
            return PageSummary(page.displayTitle, page.apiTitle, page.description, page.description, page.thumbUrl, page.lang)
        }

        @JvmStatic
        fun toPageTitle(page: ReadingListPage): PageTitle {
            return PageTitle(page.apiTitle, page.wiki, page.thumbUrl, page.description, page.displayTitle)
        }
    }
}