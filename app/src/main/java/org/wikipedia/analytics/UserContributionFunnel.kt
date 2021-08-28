package org.wikipedia.analytics

import org.wikipedia.WikipediaApp

class UserContributionFunnel private constructor() : Funnel(WikipediaApp.instance, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    fun logOpen() {
        log("action", "open_hist")
    }

    fun logFilterDescriptions() {
        log("action", "filt_desc")
    }

    fun logFilterCaptions() {
        log("action", "filt_caption")
    }

    fun logFilterTags() {
        log("action", "filt_tag")
    }

    fun logFilterAll() {
        log("action", "filt_all")
    }

    fun logViewDescription() {
        log("action", "desc_view")
    }

    fun logViewCaption() {
        log("action", "caption_view")
    }

    fun logViewTag() {
        log("action", "tag_view")
    }

    fun logViewMisc() {
        log("action", "misc_view")
    }

    fun logNavigateDescription() {
        log("action", "desc_view2")
    }

    fun logNavigateCaption() {
        log("action", "caption_view2")
    }

    fun logNavigateTag() {
        log("action", "tag_view2")
    }

    fun logNavigateMisc() {
        log("action", "misc_view2")
    }

    fun logPaused() {
        log("action", "paused")
    }

    fun logDisabled() {
        log("action", "disabled")
    }

    fun logIpBlock() {
        log("action", "ip_block")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppUserContribution"
        private const val REV_ID = 20217330
        private var INSTANCE: UserContributionFunnel? = null

        fun get(): UserContributionFunnel {
            if (INSTANCE == null) {
                INSTANCE = UserContributionFunnel()
            }
            return INSTANCE!!
        }

        fun reset() {
            INSTANCE = null
        }
    }
}
