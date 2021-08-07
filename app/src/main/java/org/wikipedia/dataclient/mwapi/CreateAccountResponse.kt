package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CreateAccountResponse(@Json(name = "createaccount") internal val createAccount: Result? = null) : MwResponse() {
    val status get() = createAccount?.status.orEmpty()
    val user get() = createAccount?.username.orEmpty()
    val message get() = createAccount?.message.orEmpty()

    @JsonClass(generateAdapter = true)
    class Result(val status: String = "", val message: String = "", val username: String = "")
}
