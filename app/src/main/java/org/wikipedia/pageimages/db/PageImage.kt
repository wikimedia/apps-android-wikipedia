package org.wikipedia.pageimages.db

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import org.wikipedia.page.PageTitle

@Parcelize
@Entity(primaryKeys = ["lang", "namespace", "apiTitle"])
data class PageImage(
    val lang: String,
    val namespace: String,
    val apiTitle: String,
    val imageName: String?
    ) : Parcelable {

    constructor(title: PageTitle, imageName: String?) : this(title.wikiSite.languageCode,
        title.namespace.orEmpty(), title.text, imageName)
}
