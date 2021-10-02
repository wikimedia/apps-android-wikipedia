package org.wikipedia.gallery

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
class ArtistInfo(
    val name: String? = null,
    @SerialName("user_page") private val userPage: String? = null
) : TextInfo()
