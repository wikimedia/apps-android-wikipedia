package org.wikipedia.page.tabs

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.PageBackStackItemSerializer
import org.wikipedia.page.PageTitle
import java.util.Date

@Entity
@Serializable(with = PageBackStackItemSerializer::class)
class PageBackStackItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var apiTitle: String = "",
    var displayTitle: String = "",
    var langCode: String = "",
    var namespace: String = "",
    val timestamp: Long = Date().time,
    var scrollY: Int = 0,
    var source: Int = HistoryEntry.Companion.SOURCE_INTERNAL_LINK,
    var thumbUrl: String? = null,
    var description: String? = null,
    var extract: String? = null
) {
    // This constructor is used for creating a new PageBackStackItem from a PageTitle and HistoryEntry.
    // The old val title and val entry were removed to avoid confusion with the new parameters.
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
        return PageTitle(namespace, apiTitle, WikiSite.forLanguageCode(langCode)).also {
            it.displayText = displayTitle
            it.thumbUrl = thumbUrl
            it.description = description
            it.extract = extract
        }
    }

    fun getHistoryEntry(): HistoryEntry {
        return HistoryEntry(getPageTitle(), source, Date(timestamp))
    }
}
