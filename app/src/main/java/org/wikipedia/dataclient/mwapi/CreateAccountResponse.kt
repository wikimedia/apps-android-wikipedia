package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
class CreateAccountResponse : MwResponse() {

    private val createaccount: Result? = null

    val status get() = createaccount?.status.orEmpty()

    val user get() = createaccount?.username.orEmpty()

    val message get() = createaccount?.message.orEmpty()

    @Serializable
    class Result {

        val status: String = ""
        val message: String = ""
        val username: String = ""
    }
}
