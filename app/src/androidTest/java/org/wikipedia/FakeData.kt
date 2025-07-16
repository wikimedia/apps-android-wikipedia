package org.wikipedia

import android.location.Location
import android.net.Uri
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle

object FakeData {
    val site = WikiSite(
        uri = Uri.parse("https://en.wikipedia.org")
    )
    val title = PageTitle(
        _displayText = "Hopf_fibration",
        _text = "Hopf fibration",
        description = "Fiber bundle of the 3-sphere over the 2-sphere, with 1-spheres as fibers",
        thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/Hopf_Fibration.png/320px-Hopf_Fibration.png",
        wikiSite = site
    )
    val inNewTab = false
    val position = 0
    val location: Location? = null
    val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_SEARCH)

    val testSiteTitle = PageTitle(
        _displayText = "What is Espresso",
        _text = "What is Espresso",
        wikiSite = WikiSite(
            languageCode = "test",
            uri = Uri.parse("https://test.wikipedia.org")
        )
    )
    val testSiteHistoryEntry = HistoryEntry(title = testSiteTitle, source = HistoryEntry.SOURCE_SEARCH)
}
