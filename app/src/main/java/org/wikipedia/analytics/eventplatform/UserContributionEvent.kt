package org.wikipedia.analytics.eventplatform

import com.google.gson.annotations.SerializedName
import org.wikipedia.WikipediaApp

class UserContributionEvent private constructor() : Event(SCHEMA_NAME, STREAM_NAME) {
    @SerializedName("action") private var action: String = ""

    fun logOpen() {
        action = "open_hist"
        submitEvent()
    }

    fun logFilterDescriptions() {
        action = "filt_desc"
        submitEvent()
    }

    fun logFilterCaptions() {
        action = "filt_caption"
        submitEvent()
    }

    fun logFilterTags() {
        action = "filt_tag"
        submitEvent()
    }

    fun logFilterAll() {
        action = "filt_all"
        submitEvent()
    }

    fun logViewDescription() {
        action = "desc_view"
        submitEvent()
    }

    fun logViewCaption() {
        action = "caption_view"
        submitEvent()
    }

    fun logViewTag() {
        action = "tag_view"
        submitEvent()
    }

    fun logViewMisc() {
        action = "misc_view"
        submitEvent()
    }

    fun logNavigateDescription() {
        action = "desc_view2"
        submitEvent()
    }

    fun logNavigateCaption() {
        action = "caption_view2"
        submitEvent()
    }

    fun logNavigateTag() {
        action = "tag_view2"
        submitEvent()
    }

    fun logNavigateMisc() {
        action = "misc_view2"
        submitEvent()
    }

    fun logPaused() {
        action = "paused"
        submitEvent()
    }

    fun logDisabled() {
        action = "disabled"
        submitEvent()
    }

    fun logIpBlock() {
        action = "ip_block"
        submitEvent()
    }

    private fun submitEvent() {
        WikipediaApp.getInstance().eventPlatformClient.submit(this)
    }

    companion object {
        private var INSTANCE: UserContributionEvent? = null
        private const val SCHEMA_NAME = "/analytics/mobile_apps/android_user_contribution_screen/1.0.0"
        private const val STREAM_NAME = "android.user_contribution_screen"
        fun get(): UserContributionEvent {
            if (INSTANCE == null) {
                INSTANCE = UserContributionEvent()
            }
            return INSTANCE!!
        }

        fun reset() {
            INSTANCE = null
        }
    }
}