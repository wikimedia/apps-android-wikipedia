package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

@Serializable
abstract class MwResponse : PostProcessable {
    private val errors: List<MwServiceError>? = null

    @SerializedName("servedby") @SerialName("servedby")
    private val servedBy: String? = null

    override fun postProcess() {
        if (errors?.isNotEmpty() == true) {
            // prioritize "blocked" errors over others.
            throw MwException(errors.firstOrNull { it.title.contains("blocked") } ?: errors.first())
        }
    }
}
