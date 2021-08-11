package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CreateAccountResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    @Json(name = "createaccount") val createAccount: Result = Result()
) : MwResponse(errors, servedBy) {
    @JsonClass(generateAdapter = true)
    class Result(val status: String = "", val message: String = "", val username: String = "")
}
