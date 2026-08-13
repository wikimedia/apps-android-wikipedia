package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateAccountResponse : MwResponse() {

    private val createaccount: Result? = null

    val status get() = createaccount?.status.orEmpty()

    val user get() = createaccount?.username.orEmpty()

    val message get() = createaccount?.message.orEmpty()

    val messageCode get() = createaccount?.messageCode.orEmpty()

    @Serializable
    class Result {

        val status: String = ""
        val message: String = ""
        @SerialName("messagecode") val messageCode: String = ""
        val username: String = ""
    }
}
