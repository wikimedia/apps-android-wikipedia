package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

@Suppress("unused")
@Serializable
@SerialName("/analytics/legacy/editattemptstep/2.0.3")
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
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(action,
                WikipediaApp.instance.appInstallID, "", editorInterface,
                INTEGRATION_ID, "", WikipediaApp.instance.getString(R.string.device_type).lowercase(), 0, getUserIdForWikiSite(pageTitle.wikiSite),
                1, pageTitle.prefixedText, pageTitle.namespace().code())))
        }

        private fun getUserIdForWikiSite(wikiSite: WikiSite): Int {
            return if (AccountUtil.isLoggedIn) SharedPreferenceCookieManager.instance.getCookieByName("UserID", wikiSite.authority(), false)?.toIntOrNull() ?: 0 else 0
        }
    }
}

@Suppress("unused")
@Serializable
class EditAttemptStepInteractionEvent(private val action: String,
                                      private val app_install_id: String,
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
