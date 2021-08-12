package org.wikipedia.analytics.eventplatform

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class UserContributionEvent(internal val action: String) : Event(SCHEMA_NAME, STREAM_NAME) {
    companion object {
        private const val SCHEMA_NAME = "/analytics/mobile_apps/android_user_contribution_screen/2.0.0"
        private const val STREAM_NAME = "android.user_contribution_screen"

        fun logOpen() {
            EventPlatformClient.submit(UserContributionEvent("open_hist"))
        }

        fun logFilterDescriptions() {
            EventPlatformClient.submit(UserContributionEvent("filt_desc"))
        }

        fun logFilterCaptions() {
            EventPlatformClient.submit(UserContributionEvent("filt_caption"))
        }

        fun logFilterTags() {
            EventPlatformClient.submit(UserContributionEvent("filt_tag"))
        }

        fun logFilterAll() {
            EventPlatformClient.submit(UserContributionEvent("filt_all"))
        }

        fun logViewDescription() {
            EventPlatformClient.submit(UserContributionEvent("desc_view"))
        }

        fun logViewCaption() {
            EventPlatformClient.submit(UserContributionEvent("caption_view"))
        }

        fun logViewTag() {
            EventPlatformClient.submit(UserContributionEvent("tag_view"))
        }

        fun logViewMisc() {
            EventPlatformClient.submit(UserContributionEvent("misc_view"))
        }

        fun logNavigateDescription() {
            EventPlatformClient.submit(UserContributionEvent("desc_view2"))
        }

        fun logNavigateCaption() {
            EventPlatformClient.submit(UserContributionEvent("caption_view2"))
        }

        fun logNavigateTag() {
            EventPlatformClient.submit(UserContributionEvent("tag_view2"))
        }

        fun logNavigateMisc() {
            EventPlatformClient.submit(UserContributionEvent("misc_view2"))
        }

        fun logPaused() {
            EventPlatformClient.submit(UserContributionEvent("paused"))
        }

        fun logDisabled() {
            EventPlatformClient.submit(UserContributionEvent("disabled"))
        }

        fun logIpBlock() {
            EventPlatformClient.submit(UserContributionEvent("ip_block"))
        }
    }
}
