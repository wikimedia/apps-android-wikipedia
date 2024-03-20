package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.wikipedia.json.LocalDateTimeSerializer
import org.wikipedia.util.DateUtil
import java.time.LocalDateTime

object ThreadItemDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val timestamp = decoder.decodeString()
        return if ("T" in timestamp) {
            // Assume a ISO 8601 timestamp
            DateUtil.iso8601LocalDateTimeParse(timestamp)
        } else {
            // Assume a DB timestamp
            DateUtil.dbLocalDateTimeParse(timestamp)
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        LocalDateTimeSerializer.serialize(encoder, value)
    }
}

typealias ThreadItemDateTime = @Serializable(ThreadItemDateTimeSerializer::class) LocalDateTime
