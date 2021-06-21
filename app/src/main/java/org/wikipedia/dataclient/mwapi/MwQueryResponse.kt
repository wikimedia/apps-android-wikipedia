package org.wikipedia.dataclient.mwapi

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName

open class MwQueryResponse : MwResponse() {

    @SerializedName("batchcomplete")
    private val batchComplete = false

    @SerializedName("continue")
    private val continuation: Map<String, String>? = null
    private var query: MwQueryResult? = null

    fun batchComplete(): Boolean {
        return batchComplete
    }

    fun continuation(): Map<String, String> {
        return continuation ?: emptyMap()
    }

    fun query(): MwQueryResult? {
        return query
    }

    fun success(): Boolean {
        return query != null
    }

    @VisibleForTesting
    protected fun setQuery(query: MwQueryResult?) {
        this.query = query
    }
}