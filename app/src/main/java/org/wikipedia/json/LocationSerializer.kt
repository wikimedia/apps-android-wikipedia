package org.wikipedia.json

import android.location.Location
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

object LocationSerializer : KSerializer<Location> {
    override val descriptor = buildClassSerialDescriptor("Location") {
        element<Double>("lat")
        element<Double>("lon")
    }

    override fun serialize(encoder: Encoder, value: Location) =
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.latitude)
            encodeDoubleElement(descriptor, 1, value.longitude)
        }

    override fun deserialize(decoder: Decoder): Location =
        decoder.decodeStructure(descriptor) {
            var lat = 0.0
            var lon = 0.0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> lat = decodeDoubleElement(descriptor, 0)
                    1 -> lon = decodeDoubleElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            val loc = Location("")
            loc.latitude = lat
            loc.longitude = lon
            loc
        }
}
