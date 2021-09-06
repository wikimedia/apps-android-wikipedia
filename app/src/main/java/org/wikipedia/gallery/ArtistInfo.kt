package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
class ArtistInfo : TextInfo() {

    val name: String? = null

    @SerializedName("user_page")
    private val userPage: String? = null
}
