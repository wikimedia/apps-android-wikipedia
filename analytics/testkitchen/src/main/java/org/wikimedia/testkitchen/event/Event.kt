package org.wikimedia.testkitchen.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData

@Serializable
open class Event(@Transient val _stream: String = "") {
    @Transient var name: String? = null

    @SerialName("\$schema")
    var schema: String = ""

    @SerialName("dt") var timestamp: String? = null

    // TODO
    //@SerialName("custom_data")
    @Transient
    var customData: Map<String, String>? = null

    protected val meta = Meta(_stream)

    // TODO
    //@SerialName("client_data")
    @Transient
    var clientData: ClientData = ClientData()

    @SerialName("sample") var sample: SampleConfig? = null

    //@SerialName("interaction_data")
    @Transient
    var interactionData: InteractionData = InteractionData()

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
