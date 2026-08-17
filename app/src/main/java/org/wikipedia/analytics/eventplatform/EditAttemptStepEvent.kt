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
class EditAttemptStepEvent(
    private val wiki: String,
    private val event: EditAttemptStepInteractionEvent) : EventWithDt(STREAM_NAME) {

    companion object {
        const val INTERFACE_WIKITEXT = "wikitext"
        const val INTERFACE_OTHER = "other"

        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        private const val INTEGRATION_ID = "app-android"

        fun logInit(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("init", editorInterface, pageTitle, editCount = editCount)
        }

        fun logSaveIntent(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("saveIntent", editorInterface, pageTitle, editCount = editCount)
        }

        fun logSaveAttempt(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("saveAttempt", editorInterface, pageTitle, editCount = editCount)
        }

        fun logSaveSuccess(pageTitle: PageTitle, revisionId: Long, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("saveSuccess", editorInterface, pageTitle, revisionId, editCount = editCount)
        }

        fun logSaveFailure(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("saveFailure", editorInterface, pageTitle, editCount = editCount)
        }

        fun logAbort(pageTitle: PageTitle, editorInterface: String = INTERFACE_WIKITEXT, editCount: Int = -1) {
            submitEditAttemptEvent("abort", editorInterface, pageTitle, editCount = editCount)
        }

        private fun submitEditAttemptEvent(action: String, editorInterface: String, pageTitle: PageTitle, revisionId: Long? = null, editCount: Int = -1) {
            EventPlatformClient.submit(EditAttemptStepEvent(
                wiki = pageTitle.wikiSite.dbName(),
                event = EditAttemptStepInteractionEvent(
                    action = action,
                    app_install_id = WikipediaApp.instance.appInstallID,
                    editing_session_id = "",
                    editor_interface = editorInterface,
                    integration = INTEGRATION_ID,
                    mw_version ="",
                    platform = WikipediaApp.instance.getString(R.string.device_type).lowercase(),
                    user_editcount = editCount,
                    user_id = getUserIdForWikiSite(pageTitle.wikiSite),
                    is_anon = !AccountUtil.isLoggedIn,
                    user_is_temp = AccountUtil.isTemporaryAccount,
                    version = 1,
                    page_title = pageTitle.prefixedText,
                    page_ns = pageTitle.namespace().code(),
                    revision_id = revisionId
                )
            ))
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
                                      private val is_anon: Boolean,
                                      private val user_is_temp: Boolean,
                                      private val version: Int,
                                      private val page_title: String,
                                      private val page_ns: Int,
                                      private val revision_id: Long? = null)
