package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
class ArtistInfo : TextInfo() {

    val name: String? = null

    @SerializedName("user_page")
    private val userPage: String? = null
}
