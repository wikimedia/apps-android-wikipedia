package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EditorInterfaceType.OTHER
import org.wikipedia.auth.AccountUtil

@Serializable
@SerialName("/analytics/legacy/editattemptstep/1.2.0")
class EditAttemptStepEvent(private val event: EditAttemptStepInteractionEvent) : Event(STREAM_NAME) {

    companion object {

        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        private const val INTEGRATION_ID = "app-android"
        private val PLATFORM = WikipediaApp.getInstance().getString(R.string.device_type).lowercase()

        fun logInit(wikiCode: String) {
            submitEditAttemptEvent(ActionType.INIT, wikiCode)
        }

        fun logSaveIntent(wikiCode: String) {
            submitEditAttemptEvent(ActionType.SAVE_INTENT, wikiCode)
        }

        fun logSaveAttempt(wikiCode: String) {
            submitEditAttemptEvent(ActionType.SAVE_ATTEMPT, wikiCode)
        }

        fun logSaveSuccess(wikiCode: String) {
            submitEditAttemptEvent(ActionType.SAVE_SUCCESS, wikiCode)
        }

        fun logSaveFailure(wikiCode: String) {
            submitEditAttemptEvent(ActionType.SAVE_FAILURE, wikiCode)
        }

        private fun submitEditAttemptEvent(action: ActionType, wikiCode: String) {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(action.valueString, "", OTHER.valueString,
                INTEGRATION_ID, "", PLATFORM, 0, if (AccountUtil.isLoggedIn) AccountUtil.getUserIdForLanguage(wikiCode) else 0, 1)))
        }
    }
}

@Serializable
class EditAttemptStepInteractionEvent(private val action: String,
                                      private val editing_session_id: String,
                                      private val editor_interface: String,
                                      private val integration: String,
                                      private val mw_version: String,
                                      private val platform: String,
                                      private val user_editcount: Int,
                                      private val user_id: Int,
                                      private val version: Int)

enum class ActionType(val valueString: String) {
    INIT("init"),
    READY("ready"),
    LOADED("loaded"),
    FIRST_CHANGE("firstChange"),
    SAVE_INTENT("saveIntent"),
    SAVE_ATTEMPT("saveAttempt"),
    SAVE_SUCCESS("saveSuccess"),
    SAVE_FAILURE("saveFailure"),
    ABORT("abort");
}

enum class EditorInterfaceType(val valueString: String) {
    VISUAL_EDITOR("visualeditor"),
    WIKITEXT_2017("wikitext-2017"),
    WIKITEXT("wikitext"),
    OTHER("other")
}
