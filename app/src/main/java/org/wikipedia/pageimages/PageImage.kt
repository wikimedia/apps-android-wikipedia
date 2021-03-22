package org.wikipedia.pageimages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

@Parcelize
data class PageImage(val title: PageTitle?, val imageName: String?) : Parcelable {

    companion object {
        @JvmField
        val DATABASE_TABLE = PageImageDatabaseTable()

        @JvmStatic
        fun imageMapFromPages(wiki: WikiSite, titles: MutableList<PageTitle>, pages: MutableList<MwQueryPage>): Map<PageTitle, PageImage> {
            val pageImagesMap = mutableMapOf<PageTitle, PageImage>()
            // nominal case
            val titlesMap = mutableMapOf<String, PageTitle>()
            titles.forEach {
                titlesMap[it.prefixedText] = it
            }
            val thumbnailSourcesMap = mutableMapOf<String, String?>()

            // noinspection ConstantConditions
            pages.forEach {
                thumbnailSourcesMap[PageTitle(null, it.title(), wiki).prefixedText] = it.thumbUrl()
                if (!it.convertedFrom().isNullOrEmpty()) {
                    thumbnailSourcesMap[PageTitle(null, it.convertedFrom()!!, wiki).prefixedText] = it.thumbUrl()
                }
                if (!it.redirectFrom().isNullOrEmpty()) {
                    thumbnailSourcesMap[PageTitle(null, it.redirectFrom()!!, wiki).prefixedText] = it.thumbUrl()
                }
            }

            titlesMap.forEach {
                if (thumbnailSourcesMap.containsKey(it.key)) {
                    pageImagesMap[it.value] = PageImage(it.value, thumbnailSourcesMap[it.key])
                }
            }
            return pageImagesMap
        }
    }
}
