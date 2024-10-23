package org.wikipedia.dataclient.donate

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
class DonationConfig(
    val version: Int,
    val currencyMinimumDonation: Map<String, Float> = emptyMap(),
    val currencyMaximumDonation: Map<String, Float> = emptyMap(),
    val currencyAmountPresets: Map<String, List<Float>> = emptyMap(),
    val currencyTransactionFees: Map<String, Float> = emptyMap(),
    val countryCodeEmailOptInRequired: List<String> = emptyList(),
    val countryCodeGooglePayEnabled: List<String> = emptyList()
)
