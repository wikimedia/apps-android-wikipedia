package org.wikipedia.pageimages.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
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
}
