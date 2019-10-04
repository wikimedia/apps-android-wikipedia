package org.wikipedia.suggestededits

import com.google.gson.annotations.SerializedName

internal class UserContributions {
    @SerializedName("user")
    var user: String? = null

    @SerializedName("title")
    var title: String? = null
}
