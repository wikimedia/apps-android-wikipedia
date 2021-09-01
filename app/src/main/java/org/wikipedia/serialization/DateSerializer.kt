package org.wikipedia.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.wikipedia.serialization.DynamicLookupSerializer.DateSurrogate
import org.wikipedia.util.DateUtil
import java.text.SimpleDateFormat
import java.util.*
@Serializer(forClass = DateSerializer::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = DateSurrogate.serializer().descriptor


    override fun serialize(output: Encoder, obj: Date) {
        output.encodeString(obj.time.toString())
    }

    override fun deserialize(input: Decoder): Date {
        return  SimpleDateFormat("yyyy-MM-dd'Z'", Locale.getDefault()).parse(input.decodeString())!!
    }
    @Serializable
    @SerialName("Date")
    private class DateSurrogate(val timeInMillis: Long)
}