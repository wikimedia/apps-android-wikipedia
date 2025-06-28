package org.wikimedia.metrics_platform.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikimedia.metrics_platform.config.sampling.SampleConfig
import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.InteractionData

@Serializable
open class Event(@Transient val _stream: String = "") {
    val name: String? = null

    @SerialName("\$schema")
    var schema: String? = null

    @SerialName("dt") protected var timestamp: String? = null

    @SerialName("custom_data") var customData: MutableMap<String, String>? = null

    protected val meta = Meta(_stream)

    @SerialName("client_data") var clientData: ClientData = ClientData()

    @SerialName("sample") var sample: SampleConfig? = null

    @SerialName("interaction_data") var interactionData: InteractionData = InteractionData()

    val stream: String get() = meta.stream

    fun setDomain(domain: String?) {
        meta.domain = domain
    }

    @Serializable
    protected class Meta(
        val stream: String = "",
        var domain: String? = null
    )
}
