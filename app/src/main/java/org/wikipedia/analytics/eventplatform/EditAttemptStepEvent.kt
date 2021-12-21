package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.ActionType.*
import org.wikipedia.analytics.eventplatform.EditorInterfaceType.WIKITEXT
import org.wikipedia.auth.AccountUtil

@Serializable
@SerialName("/analytics/legacy/editattemptstep/1.2.0")
class EditAttemptStepEvent(private val event: EditAttemptStepInteractionEvent) : Event(STREAM_NAME) {
    companion object {
        fun logInit() {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(1, INIT.valueString, WIKITEXT.valueString, "1.38.0-alpha", PLATFORM,
                INTEGRATION_ID, "8fdeb9d057acd7a01ce55efa34477b73", AccountUtil.getUserIdForLanguage("en"), 0)))
        }

        fun logSaveIntent() {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(1, SAVE_INTENT.valueString, WIKITEXT.valueString, "1.38.0-alpha", PLATFORM,
                INTEGRATION_ID, "8fdeb9d057acd7a01ce55efa34477b73", AccountUtil.getUserIdForLanguage("en"), 0)))
        }

        fun logSaveAttempt() {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(1, SAVE_ATTEMPT.valueString, WIKITEXT.valueString, "1.38.0-alpha", PLATFORM,
                INTEGRATION_ID, "8fdeb9d057acd7a01ce55efa34477b73", AccountUtil.getUserIdForLanguage("en"), 0)))
        }

        fun logSaveSuccess() {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(1, SAVE_SUCCESS.valueString, WIKITEXT.valueString, "1.38.0-alpha", PLATFORM,
                INTEGRATION_ID, "8fdeb9d057acd7a01ce55efa34477b73", AccountUtil.getUserIdForLanguage("en"), 0)))
        }

        fun logSaveFailure() {
            EventPlatformClient.submit(EditAttemptStepEvent(EditAttemptStepInteractionEvent(1, SAVE_FAILURE.valueString, WIKITEXT.valueString, "1.38.0-alpha", PLATFORM,
                INTEGRATION_ID, "8fdeb9d057acd7a01ce55efa34477b73", AccountUtil.getUserIdForLanguage("en"), 0)))
        }

        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        private const val INTEGRATION_ID = "app-android"
        private val PLATFORM = WikipediaApp.getInstance().getString(R.string.device_type).lowercase()
    }
}

@Serializable
class EditAttemptStepInteractionEvent(private val version: Int,
                                      private val action: String,
                                      private val editor_interface: String,
                                      private val mw_version: String,
                                      private val platform: String,
                                      private val integration: String,
                                      private val editing_session_id: String,
                                      private val user_id: Int,
                                      private val user_editcount: Int)

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
