package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.wikipedia.util.log.L
import java.lang.Exception
import java.util.*

object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date {
        // TODO: remove this handler and just use decodeLong once we switch to saving Tabs
        // in the database instead of serializing.
        try {
            return Date(decoder.decodeLong())
        } catch (e: Exception) {
            L.w(e)
        }
        return Date(Date.parse(decoder.decodeString()))
    }
}
