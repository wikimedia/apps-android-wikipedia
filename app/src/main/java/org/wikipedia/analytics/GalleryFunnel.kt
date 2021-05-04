package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class GalleryFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_100, wiki) {

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", source)
        return super.preprocessData(eventData)
    }

    private fun logGalleryAction(action: String, currentPageTitle: PageTitle?, currentMediaTitle: String) {
        log(
                "action", action,
                "pageTitle", currentPageTitle?.displayText ?: "FeedFeaturedImage",
                "imageTitle", currentMediaTitle
        )
    }

    fun logGalleryOpen(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("open", currentPageTitle, currentMediaTitle)
    }

    fun logGalleryClose(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("close", currentPageTitle, currentMediaTitle)
    }

    fun logGallerySwipeLeft(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("swipeLeft", currentPageTitle, currentMediaTitle)
    }

    fun logGallerySwipeRight(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("swipeRight", currentPageTitle, currentMediaTitle)
    }

    fun logGalleryShare(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("share", currentPageTitle, currentMediaTitle)
    }

    fun logGallerySave(currentPageTitle: PageTitle?, currentMediaTitle: String) {
        logGalleryAction("save", currentPageTitle, currentMediaTitle)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppMediaGallery"
        private const val REV_ID = 18115560
        const val SOURCE_LEAD_IMAGE = 0
        const val SOURCE_NON_LEAD_IMAGE = 1
        const val SOURCE_LINK_PREVIEW = 2
        const val SOURCE_FEED_FEATURED_IMAGE = 3
    }
}
