package org.wikipedia.watchlist

import com.google.gson.annotations.SerializedName

class WatchListItem {
    private val type: String? = null
    private val pageid: Int = 0
    private val revid: Int = 0
    private val ns: Int = 0
    private val title: String? = null

    @SerializedName("old_revid")
    private val oldRevId: Int = 0

    fun title(): String {
        return title.orEmpty()
    }
}
