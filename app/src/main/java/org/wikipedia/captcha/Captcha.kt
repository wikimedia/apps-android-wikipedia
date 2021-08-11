package org.wikipedia.captcha

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
data class Captcha(
    val errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") val servedBy: String = "",
    @Json(name = "fancycaptchareload") internal val fancyCaptchaReload: FancyCaptchaReload = FancyCaptchaReload()
) {
    init {
        if (errors.isNotEmpty()) {
            for (error in errors) {
                // prioritize "blocked" errors over others.
                if (error.title.contains("blocked")) {
                    throw MwException(error)
                }
            }
            throw MwException(errors[0])
        }
    }

    val captchaId: String
        get() = fancyCaptchaReload.index

    @JsonClass(generateAdapter = true)
    data class FancyCaptchaReload(val index: String = "")
}
