package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CreateAccountEvent(private val requestSource: String) {

    fun logStart() {
        submitEvent("start")
    }

    fun logError(code: String?) {
        submitEvent("error", code.orEmpty())
    }

    fun logSuccess() {
        submitEvent("success")
    }

    private fun submitEvent(action: String, errorText: String = "") {
        EventPlatformClient.submit(CreateAccountEventImpl(action, requestSource, errorText))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_create_account_interaction/1.0.0")
    class CreateAccountEventImpl(private val action: String,
                                 private val source: String,
                                 @SerialName("error_text")private val errorText: String) :
        MobileAppsEvent("android.create_account_interaction")
}
