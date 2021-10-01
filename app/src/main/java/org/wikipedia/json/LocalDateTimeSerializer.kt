package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.wikipedia.util.log.L
import java.lang.Exception
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Deprecated("This serializer is used only for HistoryEntry items in our Tabs structure and " +
        "should be removed once we switch to saving tabs in the database instead of serializing.")
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val date = try {
            Date(decoder.decodeLong())
        } catch (e: Exception) {
            L.w(e)
            Date(Date.parse(decoder.decodeString()))
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    }
}
