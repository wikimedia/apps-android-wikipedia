package org.wikipedia.suggestededits

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

class SuggestedEditsSummary {

    // TODO: verify the following variables' access level
    val pageTitle: PageTitle
    var title: String
    var lang: String
    var normalizedTitle: String? = null
    var displayTitle: String? = null
    var description: String? = null
    var thumbnailUrl: String? = null
    var originalUrl: String? = null
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
        pageTitle = rbPageSummary.getPageTitle(WikiSite.forLanguageCode(lang))
    }

    constructor(mwQueryPage: MwQueryPage, caption: String?, langCode: String) {
        title = StringUtil.removeNamespace(mwQueryPage.title())
        normalizedTitle = StringUtil.removeUnderscores(title)
        displayTitle = StringUtil.removeHTMLTags(title)
        description = caption
        thumbnailUrl = if (mwQueryPage.imageInfo() != null) mwQueryPage.imageInfo()!!.thumbUrl else mwQueryPage.thumbUrl()
        originalUrl = if (mwQueryPage.imageInfo() != null) mwQueryPage.imageInfo()!!.originalUrl else thumbnailUrl
        user = mwQueryPage.imageInfo()!!.user
        timestamp = mwQueryPage.imageInfo()!!.timestamp
        lang = langCode
        pageTitle = PageTitle(mwQueryPage.namespace().name, title, null, thumbnailUrl, WikiSite.forLanguageCode(lang))
    }
}
