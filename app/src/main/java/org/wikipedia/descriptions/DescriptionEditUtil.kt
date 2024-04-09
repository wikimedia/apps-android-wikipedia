package org.wikipedia.descriptions

import org.wikipedia.page.Page

object DescriptionEditUtil {

    private const val DESCRIPTION_SOURCE_LOCAL = "local"
    private const val DESCRIPTION_SOURCE_WIKIDATA = "central"

    fun wikiUsesLocalDescriptions(lang: String): Boolean {
        return lang == "en" || lang == "test"
    }

    fun isEditAllowed(page: Page): Boolean {
        return if (wikiUsesLocalDescriptions(page.title.wikiSite.languageCode)) {
            // For English Wikipedia, allow editing the description for all articles, since the
            // edit will go directly into the article instead of Wikidata.
            true
        } else !page.pageProperties.wikiBaseItem.isNullOrEmpty() &&
                page.pageProperties.descriptionSource != DESCRIPTION_SOURCE_LOCAL
    }
}
