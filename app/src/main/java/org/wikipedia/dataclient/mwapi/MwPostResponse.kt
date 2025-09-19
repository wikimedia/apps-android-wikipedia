package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil

@Serializable
open class MwPostResponse : MwResponse() {
    val pageInfo: MwQueryPage? = null
    val options: String? = null
    val success = 0

    init {
        // If the user gets assigned a temporary account as a result of a POST action, then
        // record the day of the account creation.
        AccountUtil.maybeSetTempAccountDay()
    }
}
