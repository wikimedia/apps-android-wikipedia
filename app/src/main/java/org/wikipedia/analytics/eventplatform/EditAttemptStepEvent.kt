package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.PageTitle

@Suppress("unused")
@Serializable
@SerialName("/analytics/legacy/editattemptstep/1.4.0")
class EditAttemptStepEvent(private val event: EditAttemptStepInteractionEvent) : Event(STREAM_NAME) {

    companion object {
        const val INTERFACE_WIKITEXT = "wikitext"
        const val INTERFACE_OTHER = "other"

        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        private const val INTEGRATION_ID = "app-android"

        fun logInit(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT) {
            submitEditAttemptEvent("init", editorInterface, pageTitle)
        }

        fun logSaveIntent(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT) {
            submitEditAttemptEvent("saveIntent", editorInterface, pageTitle)
        }

        fun logSaveAttempt(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT) {
            submitEditAttemptEvent("saveAttempt", editorInterface, pageTitle)
        }

        fun logSaveSuccess(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT) {
            submitEditAttemptEvent("saveSuccess", editorInterface, pageTitle)
        }

        fun logSaveFailure(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT) {
            submitEditAttemptEvent("saveFailure", editorInterface, pageTitle)
        }

        private fun submitEditAttemptEvent(action: String, editorInterface: String, pageTitle: PageTitle) {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(action, "", editorInterface,
                INTEGRATION_ID, "", WikipediaApp.instance.getString(R.string.device_type).lowercase(), 0,
                    if (AccountUtil.isLoggedIn) AccountUtil.getUserIdForLanguage(pageTitle.wikiSite.languageCode) else 0,
                1, pageTitle.prefixedText, pageTitle.namespace().code())))
        }
    }
}

@Suppress("unused")
@Serializable
class EditAttemptStepInteractionEvent(private val action: String,
                                      private val editing_session_id: String,
                                      private val editor_interface: String,
                                      private val integration: String,
                                      private val mw_version: String,
                                      private val platform: String,
                                      private val user_editcount: Int,
                                      private val user_id: Int,
                                      private val version: Int,
                                      private val page_title: String,
                                      private val page_ns: Int)
