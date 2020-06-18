package org.wikipedia.suggestededits

import org.wikipedia.dataclient.WikiSite
import java.util.*

class Contribution internal constructor(val qNumber: String, var title: String, var description: String, val editType: Int, var imageUrl: String?,
                                        val date: Date, val wikiSite: WikiSite, var pageViews: Long, var revisionId: Long) {

    companion object {
        const val EDIT_TYPE_GENERIC = 0
        const val EDIT_TYPE_ARTICLE_DESCRIPTION = 1
        const val EDIT_TYPE_IMAGE_CAPTION = 2
        const val EDIT_TYPE_IMAGE_TAG = 3
    }
}
