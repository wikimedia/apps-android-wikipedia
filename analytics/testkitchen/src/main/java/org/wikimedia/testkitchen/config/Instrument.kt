@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.testkitchen.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wikimedia.testkitchen.context.InstantSerializer
import java.time.Instant

@Serializable
class Instrument : Sampleable() {
    val name: String = ""
    val start: Instant? = null
    val end: Instant? = null
    @SerialName("stream_name") val streamName: String = ""
    @SerialName("schema_title") val schemaTitle: String = ""
    @SerialName("contextual_attributes") val contextualAttributes: List<String> = emptyList()
    @SerialName("sample_unit") val sampleUnit: String = ""

}
