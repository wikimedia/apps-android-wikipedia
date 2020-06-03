package org.wikipedia.suggestededits

import org.wikipedia.dataclient.WikiSite
import java.util.*

class Contribution internal constructor(val qNumber: String, var title: String, var description: String, val editType: Int, var imageUrl: String?,
                                        val date: Date, val wikiSite: WikiSite, var pageViews: Long) {
    override fun hashCode(): Int {
        return title.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Contribution) {
            return this.title == other.title
        }
        return false
    }

    companion object {
        const val EDIT_TYPE_ARTICLE_DESCRIPTION = 0
        const val EDIT_TYPE_IMAGE_CAPTION = 1
        const val EDIT_TYPE_IMAGE_TAG = 2
        const val ALL_EDIT_TYPES = 3
    }
}
