package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

abstract class MwResponse : PostProcessable {
    private val errors: List<MwServiceError>? = null

    @SerializedName("servedby")
    private val servedBy: String? = null

    override fun postProcess() {
        if (errors?.isNotEmpty() == true) {
            // prioritize "blocked" errors over others.
            throw MwException(errors.firstOrNull { it.title.contains("blocked") } ?: errors.first())
        }
    }
}
