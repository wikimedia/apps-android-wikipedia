package org.wikipedia.page.tabs

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import java.util.Date

@Entity
@Serializable
class PageBackStackItem(
    @PrimaryKey(autoGenerate = true) var id: Long = -1,
    val apiTitle: String,
    val displayTitle: String,
    val langCode: String,
    val namespace: String,
    val timestamp: Long = Date().time,
    var scrollY: Int = 0,
    var source: Int = HistoryEntry.Companion.SOURCE_INTERNAL_LINK,
    var thumbUrl: String? = null,
    var description: String? = null,
    var extract: String? = null
) {
    constructor(title: PageTitle, entry: HistoryEntry) : this(
        apiTitle = title.prefixedText,
        displayTitle = title.displayText,
        langCode = title.wikiSite.languageCode,
        namespace = title.namespace,
        thumbUrl = title.thumbUrl,
        description = title.description,
        extract = title.extract,
        source = entry.source
    )

    fun getPageTitle(): PageTitle {
        return PageTitle(namespace, apiTitle, WikiSite.Companion.forLanguageCode(langCode)).apply {
            this.displayText = displayTitle
            this.thumbUrl = thumbUrl
            this.description = description
            this.extract = extract
        }
    }

    fun getHistoryEntry(): HistoryEntry {
        return HistoryEntry(getPageTitle(), source, Date(timestamp))
    }
}
