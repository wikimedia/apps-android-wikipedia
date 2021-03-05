package org.wikipedia.pageimages

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

class PageImage(val title: PageTitle, val imageName: String) {

    companion object {
        @JvmField
        val DATABASE_TABLE = PageImageDatabaseTable()

        @JvmStatic
        fun imageMapFromPages(wiki: WikiSite, titles: MutableList<PageTitle>, pages: MutableList<MwQueryPage>): Map<PageTitle?, PageImage> {
            val pageImagesMap = mutableMapOf<PageTitle?, PageImage>()
            // nominal case
            val titlesMap = mutableMapOf<String, PageTitle>()
            titles.forEach { title ->
                titlesMap[title.prefixedText] = title
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

            titlesMap.keys.forEach { key ->
                if (thumbnailSourcesMap.containsKey(key)) {
                    pageImagesMap[titlesMap[key]] = PageImage(titlesMap[key]!!, thumbnailSourcesMap[key]!!)
                }
            }
            return pageImagesMap
        }
    }
}