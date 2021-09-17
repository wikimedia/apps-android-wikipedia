package org.wikipedia.userprofile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.parcel.DateParceler
import java.util.*

@Parcelize
@TypeParceler<Date, DateParceler>()
class Contribution(
    val qNumber: String,
    val revId: Long,
    var apiTitle: String,
    var displayTitle: String,
    var description: String,
    val editType: Int,
    var imageUrl: String?,
    val date: Date,
    val wikiSite: WikiSite,
    var pageViews: Long,
    var sizeDiff: Int,
    var top: Boolean,
    var tagCount: Int
) : Parcelable {
    companion object {
        const val EDIT_TYPE_GENERIC = 0
        const val EDIT_TYPE_ARTICLE_DESCRIPTION = 1
        const val EDIT_TYPE_IMAGE_CAPTION = 2
        const val EDIT_TYPE_IMAGE_TAG = 3
    }
}
