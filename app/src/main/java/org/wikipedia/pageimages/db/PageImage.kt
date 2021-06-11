package org.wikipedia.pageimages.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle

@Parcelize
@Entity
data class PageImage(
    val authority: String = "",
    val lang: String,
    val apiTitle: String,
    val displayTitle: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val namespace: String?,
    val imageName: String?
    ) : Parcelable {

    constructor(title: PageTitle, imageName: String?) : this(title.wikiSite.authority(),
        title.wikiSite.languageCode(), title.text, title.displayText, namespace = title.namespace,
        imageName = imageName) {
        pageTitle = title
    }

    @IgnoredOnParcel
    @Ignore
    private var pageTitle: PageTitle? = null

    val title: PageTitle get() {
        if (pageTitle == null) {
            pageTitle = PageTitle(namespace, apiTitle, WikiSite(authority, lang))
            pageTitle!!.setDisplayText(displayTitle)
        }
        return pageTitle!!
    }

    companion object {
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
