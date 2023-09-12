package org.wikipedia.analytics.metricsplatform

class CreateAccountEvent(private val requestSource: String) : MetricsEvent() {

    fun logStart() {
        submit("start")
    }

    fun logError(code: String?) {
        submit("error", code.orEmpty())
    }

    fun logSuccess() {
        submit("success")
    }

    private fun submit(action: String, errorText: String = "") {
        submitEvent(
            "create_account_interaction",
            mapOf(
                "action" to action,
                "source" to requestSource,
                "error_text" to errorText
            )
        )
    }
}

class UserContributionEvent(val action: String) {
    companion object : MetricsEvent() {
        private const val STREAM_NAME = "android.user_contribution_screen"

        fun logOpen() {
            submit("open_hist")
        }

        fun logFilterDescriptions() {
            submit("filt_desc")
        }

        fun logFilterCaptions() {
            submit("filt_caption")
        }

        fun logFilterTags() {
            submit("filt_tag")
        }

        fun logFilterAll() {
            submit("filt_all")
        }

        fun logViewDescription() {
            submit("desc_view")
        }

        fun logViewCaption() {
            submit("caption_view")
        }

        fun logViewTag() {
            submit("tag_view")
        }

        fun logViewMisc() {
            submit("misc_view")
        }

        fun logNavigateDescription() {
            submit("desc_view2")
        }

        fun logNavigateCaption() {
            submit("caption_view2")
        }

        fun logNavigateTag() {
            submit("tag_view2")
        }

        fun logNavigateMisc() {
            submit("misc_view2")
        }

        fun logPaused() {
            submit("paused")
        }

        fun logDisabled() {
            submit("disabled")
        }

        fun logIpBlock() {
            submit("ip_block")
        }

        private fun submit(action: String) {
            submitEvent(
                "user_contribution_screen",
                mapOf(
                    "action" to action
                )
            )
        }
    }
}
