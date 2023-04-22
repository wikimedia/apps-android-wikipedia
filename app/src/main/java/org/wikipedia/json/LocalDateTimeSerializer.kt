package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.wikipedia.util.DateUtil
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Serializes/deserializes [LocalDateTime] values from ISO-8601 timestamp strings.
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = DateUtil.iso8601LocalDateTimeParse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.atZone(ZoneId.systemDefault()).toInstant().toString())
    }
}

typealias LocalDateTimeAsTimestamp = @Serializable(LocalDateTimeSerializer::class) LocalDateTime
