package org.wikipedia.json

import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Uri) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Uri = Uri.parse(decoder.decodeString())
}
