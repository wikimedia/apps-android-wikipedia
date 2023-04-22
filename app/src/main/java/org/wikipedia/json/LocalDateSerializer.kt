package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

/**
 * Serializes/deserializes [LocalDate] values from ISO-8601 timestamp strings.
 */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDateTimeSerializer.deserialize(decoder).toLocalDate()
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        LocalDateTimeSerializer.serialize(encoder, value.atStartOfDay())
    }
}

typealias LocalDateAsTimestamp = @Serializable(LocalDateSerializer::class) LocalDate
