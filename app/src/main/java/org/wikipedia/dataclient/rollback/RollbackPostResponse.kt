package org.wikipedia.dataclient.rollback

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwPostResponse

@Serializable
class RollbackPostResponse : MwPostResponse() {
    val rollback: Rollback? = null
}
