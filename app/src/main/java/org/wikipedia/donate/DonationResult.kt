package org.wikipedia.donate

import kotlinx.serialization.Serializable

@Serializable
class DonationResult(
    val dateTime: String = "",
    val fromWeb: Boolean = false
)
