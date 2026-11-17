package org.wikipedia.donate

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Donor relationship codes stored in the "wikimedia-donor" user option.
 * Values 1–5 mirror WikimediaCustomizations DonorIdentification relationships.json,
 * and NONE (0) matches web's "not consented" default.
 * Note: UNKNOWN (-1) is app-only: any value we don't recognize maps here instead of failing to decode.
 */
@Serializable(with = DonorCode.Serializer::class)
enum class DonorCode(val value: Int) {
    NONE(0),
    RECENT(1),
    SUSTAINING(2),
    LAPSED(3),
    RETURNING(4),
    CONTACTABLE(5),
    UNKNOWN(-1);

    val isRecentDonor get() = this in RECENT_DONOR_CODES

    companion object {
        private val RECENT_DONOR_CODES = setOf(RECENT, SUSTAINING, RETURNING)
        fun fromValue(value: Int): DonorCode = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }

    object Serializer : KSerializer<DonorCode> {
        override val descriptor = PrimitiveSerialDescriptor("DonorCode", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: DonorCode) = encoder.encodeInt(value.value)
        override fun deserialize(decoder: Decoder): DonorCode = fromValue(decoder.decodeInt())
    }
}
