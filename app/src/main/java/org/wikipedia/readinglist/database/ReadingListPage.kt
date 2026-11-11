package org.wikipedia.readinglist.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ArticleOfflineState
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.io.Serializable

@Entity
data class ReadingListPage(
    val wiki: WikiSite,
    val namespace: Namespace,
    var displayTitle: String,
    var apiTitle: String,
    var description: String? = null,
    var thumbUrl: String? = null,
    var listId: Long = -1,
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var mtime: Long = 0,
    var atime: Long = 0,
    var offline: Boolean = Prefs.isDownloadingReadingListArticlesEnabled,
    var status: Long = STATUS_QUEUE_FOR_SAVE,
    var sizeBytes: Long = 0,
    var lang: String = "en",
    var revId: Long = 0,
    var remoteId: Long = 0
) : Serializable {

    constructor(title: PageTitle) :
            this(title.wikiSite, title.namespace(), title.displayText, title.prefixedText,
                title.description, title.thumbUrl, lang = title.wikiSite.languageCode) {
        val now = System.currentTimeMillis()
        mtime = now
        atime = now
    }

    @delegate:Transient
    val accentInvariantTitle: String by lazy {
        StringUtils.stripAccents(StringUtil.fromHtml(displayTitle).toString())
    }

    @Transient var downloadProgress = 0

    @Transient var selected = false

    @Ignore @Transient var articleOfflineState: ArticleOfflineState? = null

    val offlineRequested get() = articleOfflineState?.requested ?: (offline && status != STATUS_QUEUE_FOR_DELETE)

    val availableOffline get() = articleOfflineState?.downloaded == true

    val availableOfflineBytes get() = articleOfflineState?.availableBytes ?: 0L

    val downloadQueued get() = offline && (status == STATUS_QUEUE_FOR_SAVE || status == STATUS_QUEUE_FOR_FORCED_SAVE)
    val saving get() = articleOfflineState?.pending ?: downloadQueued
    val canRetryOffline get() = offlineRequested && !availableOffline && !saving

    companion object {
        const val STATUS_QUEUE_FOR_SAVE = 0L
        const val STATUS_SAVED = 1L
        const val STATUS_QUEUE_FOR_DELETE = 2L
        const val STATUS_QUEUE_FOR_FORCED_SAVE = 3L
        const val STATUS_DOWNLOAD_FAILED = 4L

        fun toPageSummary(page: ReadingListPage): PageSummary {
            return PageSummary(page.displayTitle, page.apiTitle, page.description, page.description, page.thumbUrl, page.lang)
        }

        fun toPageTitle(page: ReadingListPage): PageTitle {
            val wiki = page.wiki
            wiki.languageCode = page.lang
            return PageTitle(page.apiTitle, wiki, page.thumbUrl, page.description, page.displayTitle)
        }
    }
}
