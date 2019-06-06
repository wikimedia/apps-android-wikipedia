package org.wikipedia.suggestededits

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.Namespace
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

    constructor(fileTitle: String, imageInfo: ImageInfo, caption: String?, langCode: String) {
        title = StringUtil.removeNamespace(fileTitle)
        normalizedTitle = StringUtil.removeUnderscores(fileTitle)
        displayTitle = StringUtil.removeHTMLTags(fileTitle)
        description = caption
        thumbnailUrl = imageInfo.thumbUrl
        originalUrl = imageInfo.originalUrl
        user = imageInfo.user
        timestamp = imageInfo.timestamp
        metadata = imageInfo.metadata
        lang = langCode
        pageTitle = PageTitle(Namespace.FILE.name, title, null, thumbnailUrl, WikiSite.forLanguageCode(lang))
    }
}
