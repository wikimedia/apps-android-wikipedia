package org.wikipedia.dataclient.mwapi

open class MwPostResponse : MwResponse() {
    val pageInfo: MwQueryPage? = null
    val options: String? = null
    val successVal = 0
    fun success(result: String?): Boolean {
        return "success" == result
    }
}
