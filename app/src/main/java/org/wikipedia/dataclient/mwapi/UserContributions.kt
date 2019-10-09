package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName

class UserContributions {
    @SerializedName("user")
    var user: String? = null

    @SerializedName("title")
    var title: String? = null
}
