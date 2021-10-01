package org.wikipedia.dataclient.watch

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
data class Watch(val title: String?,
                                  val ns: Int,
                                  val pageid: Int,
                                  val expiry: String?,
                                  val watched: Boolean,
                                  val unwatched: Boolean,
                                  val missing: Boolean) : MwResponse()
