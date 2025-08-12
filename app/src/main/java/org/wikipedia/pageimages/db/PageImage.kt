package org.wikipedia.pageimages.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import kotlinx.parcelize.Parcelize
import org.wikipedia.page.PageTitle

@Parcelize
@Entity(
    primaryKeys = ["lang", "namespace", "apiTitle"],
    indices = [Index(value = ["lang", "namespace", "apiTitle"])]
)
data class PageImage(
    val lang: String,
    val namespace: String,
    val apiTitle: String,
    var imageName: String?,
    var description: String?,
    var timeSpentSec: Int = 0,
    var geoLat: Double = 0.0,
    var geoLon: Double = 0.0
) : Parcelable {

    constructor(title: PageTitle, imageName: String?, description: String?, geoLat: Double?, geoLon: Double?) : this(title.wikiSite.languageCode,
        title.namespace, title.text, imageName, description, 0, geoLat ?: 0.0, geoLon ?: 0.0)
}
