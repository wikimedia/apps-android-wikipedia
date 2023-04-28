package org.wikipedia.analytics.metricsplatform

import org.wikipedia.page.PageFragment

class AndroidPageClientMetadata(
    var fragment: PageFragment
) : AndroidClientMetadata() {

    override fun getMediawikiDatabase(): String {
        return fragment.model.title?.wikiSite?.dbName()!!
    }

    override fun getPageId(): Int {
        return fragment.model.page?.pageProperties?.pageId!!
    }

    override fun getPageNamespaceName(): String {
        return fragment.model.page?.pageProperties?.namespace?.name!!
    }

    override fun getPageTitle(): String {
        return fragment.title?.text!!
    }

    override fun getPageRevisionId(): Int? {
        return fragment.model.page?.pageProperties?.revisionId?.toInt()
    }

    override fun getPageWikidataItemQid(): String {
        return fragment.model.page?.pageProperties?.wikiBaseItem ?: ""
    }

    override fun getPageContentLanguage(): String {
        return fragment.model.title?.wikiSite?.languageCode!!
    }
}
