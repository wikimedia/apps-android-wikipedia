package org.wikipedia.analytics.testkitchen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikimedia.testkitchen.context.PageData
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ActiveTimer

open class ArticleEvent : Event {
    private val timer = ActiveTimer()
    private val pageData: PageData?
    var source: Int

    constructor(source: Int) {
        this.source = source
        this.pageData = null
    }

    constructor(fragment: PageFragment, source: Int) {
        this.source = source
        pageData = getPageData(fragment)
    }

    constructor(pageTitle: PageTitle, pageId: Int, source: Int) {
        this.source = source
        pageData = getPageData(pageTitle, pageId)
    }

    constructor(pageTitle: PageTitle, summary: PageSummary, source: Int) {
        this.source = source
        pageData = getPageData(pageTitle, summary)
    }

    @Serializable
    class ContextData(
        @SerialName("time_spent_ms") val timeSpentMillis: Long? = null
    )

    companion object {
        const val STREAM_LINK_PREVIEW_INTERACTION = "android.product_metrics.article_link_preview_interaction"
    }
}
