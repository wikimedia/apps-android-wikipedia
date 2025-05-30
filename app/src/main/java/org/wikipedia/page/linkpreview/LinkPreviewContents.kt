package org.wikipedia.page.linkpreview

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.L10nUtil

class LinkPreviewContents(pageSummary: PageSummary, wiki: WikiSite) {
    val title = pageSummary.getPageTitle(wiki)
    val ns = pageSummary.namespace
    val isDisambiguation = pageSummary.type == PageSummary.TYPE_DISAMBIGUATION
    val extract = if (isDisambiguation)
        "<p>" + L10nUtil.getString(title, R.string.link_preview_disambiguation_description) + "</p>" + pageSummary.extractHtml
        else pageSummary.extractHtml
}
