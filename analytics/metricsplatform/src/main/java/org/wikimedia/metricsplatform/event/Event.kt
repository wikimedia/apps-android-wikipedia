package org.wikimedia.metricsplatform.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikimedia.metricsplatform.config.sampling.SampleConfig
import org.wikimedia.metricsplatform.context.ClientData
import org.wikimedia.metricsplatform.context.InteractionData

@Serializable
open class Event(@Transient val _stream: String = "") {
    var name: String? = null

    @SerialName("\$schema")
    var schema: String = ""

    @SerialName("dt") var timestamp: String? = null

    @SerialName("custom_data") var customData: Map<String, String>? = null

    protected val meta = Meta(_stream)

    @SerialName("client_data") var clientData: ClientData = ClientData()

    @SerialName("sample") var sample: SampleConfig? = null

    @SerialName("interaction_data") var interactionData: InteractionData = InteractionData()

    var stream
        get() = meta.stream
        set(value) { meta.stream = value }

    fun setDomain(domain: String?) {
        meta.domain = domain
    }

    @Serializable
    protected class Meta(
        var stream: String = "",
        var domain: String? = null
    )
}
