package org.wikipedia.suggestededits

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

class SuggestedEditsSummary {

    var title: String
    var lang: String
    var normalizedTitle: String? = null
    var displayTitle: String? = null
    var description: String? = null
    var thumbnailUrl: String? = null
    var extractHtml: String? = null
    var timestamp: String? = null
    var user: String? = null
    var metadata: ExtMetadata? = null

    constructor(rbPageSummary: RbPageSummary) {
        title = rbPageSummary.title
        normalizedTitle = rbPageSummary.normalizedTitle
        displayTitle = rbPageSummary.displayTitle
        description = rbPageSummary.description
        thumbnailUrl = rbPageSummary.thumbnailUrl
        extractHtml = rbPageSummary.extractHtml
        lang = rbPageSummary.lang
    }

    constructor(mwQueryPage: MwQueryPage, caption: String?, langCode: String) {
        title = mwQueryPage.title()
        normalizedTitle = StringUtil.removeUnderscores(mwQueryPage.title())
        displayTitle = StringUtil.removeHTMLTags(mwQueryPage.title())
        description = caption
        thumbnailUrl = if (mwQueryPage.imageInfo() != null) mwQueryPage.imageInfo()!!.thumbUrl else mwQueryPage.thumbUrl()
        user = mwQueryPage.imageInfo()!!.user
        timestamp = mwQueryPage.imageInfo()!!.timestamp
        lang = langCode
    }

    fun getPageTitle(wiki: WikiSite): PageTitle {
        return PageTitle(title, wiki, thumbnailUrl, description)
    }
}
