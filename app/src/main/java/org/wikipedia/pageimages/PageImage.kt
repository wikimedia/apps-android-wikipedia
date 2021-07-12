package org.wikipedia.pageimages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

@Parcelize
data class PageImage(val title: PageTitle, val imageName: String?) : Parcelable {

    companion object {
        @JvmField
        val DATABASE_TABLE = PageImageDatabaseTable()

        @JvmStatic
        fun imageMapFromPages(wiki: WikiSite, titles: MutableList<PageTitle>, pages: MutableList<MwQueryPage>): Map<PageTitle, PageImage> {
            // nominal case
            val titlesMap = titles.associateBy { it.prefixedText }
            val thumbnailSourcesMap = mutableMapOf<String, String?>()

            // noinspection ConstantConditions
            pages.forEach {
                val convertedFrom = it.convertedFrom()
                val redirectFrom = it.redirectFrom()
                thumbnailSourcesMap[PageTitle(null, it.title(), wiki).prefixedText] = it.thumbUrl()
                if (!convertedFrom.isNullOrEmpty()) {
                    thumbnailSourcesMap[PageTitle(null, convertedFrom, wiki).prefixedText] = it.thumbUrl()
                }
                if (!redirectFrom.isNullOrEmpty()) {
                    thumbnailSourcesMap[PageTitle(null, redirectFrom, wiki).prefixedText] = it.thumbUrl()
                }
            }

            return titlesMap.filterKeys { thumbnailSourcesMap.containsKey(it) }
                .map { (key, value) -> value to PageImage(value, thumbnailSourcesMap[key].orEmpty()) }
                .toMap()
        }
    }
}
