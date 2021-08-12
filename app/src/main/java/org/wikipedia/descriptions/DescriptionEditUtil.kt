package org.wikipedia.descriptions

import org.wikipedia.page.Page

object DescriptionEditUtil {

    private const val DESCRIPTION_SOURCE_LOCAL = "local"
    private const val DESCRIPTION_SOURCE_WIKIDATA = "central"

    @JvmStatic
    fun isEditAllowed(page: Page): Boolean {
        return if (page.title.wikiSite.languageCode == "en") {
            // For English Wikipedia, allow editing the description for all articles, since the
            // edit will go directly into the article instead of Wikidata.
            true
        } else !page.pageProperties.wikiBaseItem.isNullOrEmpty() &&
                page.pageProperties.descriptionSource != DESCRIPTION_SOURCE_LOCAL
    }
}
