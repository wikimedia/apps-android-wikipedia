package org.wikipedia.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(with = PacketSerializer::class)
data class Packet(val dataType: String, val payload: Any)

object PacketSerializer : KSerializer<Packet> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Packet") {
        element("dataType", serialDescriptor<String>())
        element("payload", buildClassSerialDescriptor("Any"))
    }

    @Suppress("UNCHECKED_CAST")
    private val dataTypeSerializers: Map<String, KSerializer<Any>> =
        mapOf(
            "String" to serializer<String>(),
            "Int" to serializer<Int>(),
            //list them all
             ).mapValues { (_, v) -> v as KSerializer<Any> }

    private fun getPayloadSerializer(dataType: String): KSerializer<Any> = dataTypeSerializers[dataType]
        ?: throw SerializationException("Serializer for class $dataType is not registered in PacketSerializer")

    override fun serialize(encoder: Encoder, value: Packet) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.dataType)
            encodeSerializableElement(descriptor, 1, getPayloadSerializer(value.dataType), value.payload)
        }
    }

    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): Packet = decoder.decodeStructure(descriptor) {
        if (decodeSequentially()) {
            val dataType = decodeStringElement(descriptor, 0)
            val payload = decodeSerializableElement(descriptor, 1, getPayloadSerializer(dataType))
            Packet(dataType, payload)
        } else {
            require(decodeElementIndex(descriptor) == 0) { "dataType field should precede payload field" }
            val dataType = decodeStringElement(descriptor, 0)
            val payload = when (val index = decodeElementIndex(descriptor)) {
                1 -> decodeSerializableElement(descriptor, 1, getPayloadSerializer(dataType))
                CompositeDecoder.DECODE_DONE -> throw SerializationException("payload field is missing")
                else -> error("Unexpected index: $index")
            }
            Packet(dataType, payload)
        }
    }
}