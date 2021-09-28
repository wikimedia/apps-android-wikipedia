package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.wikipedia.util.log.L
import java.lang.Exception
import java.util.*

@Deprecated("This serializer is used only for HistoryEntry items in our Tabs structure and" +
        "should be removed once we switch to saving tabs in the database instead of serializing.")
object DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date {
        try {
            return Date(decoder.decodeLong())
        } catch (e: Exception) {
            L.w(e)
        }
        return Date(Date.parse(decoder.decodeString()))
    }
}
