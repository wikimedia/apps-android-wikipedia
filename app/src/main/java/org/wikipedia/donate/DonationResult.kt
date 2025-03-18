package org.wikipedia.donate

import kotlinx.serialization.Serializable

@Serializable
class DonationResult(
    val dateTime: String = "",
    val fromWeb: Boolean = false,
    val amount: Float = 0f,
    val currency: String = "",
    val recurring: Boolean = false
)
