package org.wikipedia.history

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.DateSerializer
import org.wikipedia.page.PageTitle
import org.wikipedia.parcel.DateParceler
import java.util.*

@Serializable
@Parcelize
@TypeParceler<Date, DateParceler>()
@Entity
// TODO: change these properties back to val when HistoryEntry is no longer serializable. (i.e. when we update Tabs to be in the database instead of Prefs)
class HistoryEntry(
    var authority: String = "",
    var lang: String = "",
    var apiTitle: String = "",
    var displayTitle: String = "",
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var namespace: String = "",
    @Serializable(with = DateSerializer::class) var timestamp: Date = Date(),
    var source: Int = SOURCE_INTERNAL_LINK,
    var timeSpentSec: Int = 0,
) : Parcelable {
    constructor(title: PageTitle, source: Int, timestamp: Date = Date(), timeSpentSec: Int = 0) : this(title.wikiSite.authority(),
        title.wikiSite.languageCode, title.text, title.displayText, namespace = title.namespace,
        timestamp = timestamp, source = source, timeSpentSec = timeSpentSec) {
        pageTitle = title
    }

    @IgnoredOnParcel
    @Transient
    @Ignore
    private var pageTitle: PageTitle? = null

    val title: PageTitle get() {
        if (pageTitle == null) {
            pageTitle = PageTitle(namespace, apiTitle, WikiSite(authority, lang))
            pageTitle!!.displayText = displayTitle
        }
        return pageTitle!!
    }

    // To be set when navigating back and forth between articles.
    @IgnoredOnParcel
    @Transient
    @Ignore
    var referrer: String? = null

    companion object {
        const val SOURCE_SEARCH = 1
        const val SOURCE_INTERNAL_LINK = 2
        const val SOURCE_EXTERNAL_LINK = 3
        const val SOURCE_HISTORY = 4
        const val SOURCE_LANGUAGE_LINK = 6
        const val SOURCE_RANDOM = 7
        const val SOURCE_MAIN_PAGE = 8
        const val SOURCE_DISAMBIG = 10
        const val SOURCE_READING_LIST = 11
        const val SOURCE_FEED_CONTINUE_READING = 12
        const val SOURCE_FEED_BECAUSE_YOU_READ = 13
        const val SOURCE_FEED_MOST_READ = 14
        const val SOURCE_FEED_FEATURED = 15
        const val SOURCE_NEWS = 16
        const val SOURCE_FEED_MAIN_PAGE = 17
        const val SOURCE_FEED_RANDOM = 18
        const val SOURCE_GALLERY = 19
        const val SOURCE_APP_SHORTCUT_RANDOM = 20
        const val SOURCE_APP_SHORTCUT_CONTINUE_READING = 21
        const val SOURCE_FEED_MOST_READ_ACTIVITY = 22
        const val SOURCE_ON_THIS_DAY_CARD = 23
        const val SOURCE_ON_THIS_DAY_ACTIVITY = 24
        const val SOURCE_NOTIFICATION = 25
        const val SOURCE_NOTIFICATION_SYSTEM = 26
        const val SOURCE_FLOATING_QUEUE = 27
        const val SOURCE_EDIT_DESCRIPTION = 28
        const val SOURCE_WIDGET = 29
        const val SOURCE_SUGGESTED_EDITS = 30
        const val SOURCE_TALK_TOPIC = 31
        const val SOURCE_WATCHLIST = 32
        const val SOURCE_EDIT_DIFF_DETAILS = 33
        const val SOURCE_ERROR = 34
        const val SOURCE_EDIT_HISTORY = 35
        const val SOURCE_CATEGORY = 36
    }
}
