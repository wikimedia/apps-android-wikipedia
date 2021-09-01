package org.wikipedia.serialization

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@ExperimentalSerializationApi
@Parcelize
class DynamicLookupSerializer: KSerializer<Date>, Parcelable {
    override val descriptor: SerialDescriptor = DateSurrogate.serializer().descriptor

    @InternalSerializationApi
    override fun serialize(encoder: Encoder, value: Date) {
        val surrogate = DateSurrogate(value.time)
        encoder.encodeSerializableValue(DateSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Date {
        val surrogate = decoder.decodeSerializableValue(DateSurrogate.serializer())
        return Date(surrogate.timeInMillis)
    }
    @Serializable
    @SerialName("Date")
    private class DateSurrogate(val timeInMillis: Long)
}