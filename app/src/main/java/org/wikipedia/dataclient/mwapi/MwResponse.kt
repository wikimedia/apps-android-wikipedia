package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

@Serializable
abstract class MwResponse : PostProcessable {
    val errors: List<MwServiceError>? = null

    @SerializedName("servedby")
    private val servedBy: String? = null
    override fun postProcess() {
        if (errors != null && errors.isNotEmpty()) {
            for (error in errors) {
                // prioritize "blocked" errors over others.
                if (error.title.contains("blocked")) {
                    throw MwException(error)
                }
            }
            throw MwException(errors[0])
        }
    }
}
