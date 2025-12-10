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

    @SerialName("\$schema") var schema: String = ""

    @SerialName("dt") var timestamp: String? = null

    protected val meta = Meta(_stream)

    // TODO
    @SerialName("sample") var sample: SampleConfig? = null

    @Transient var clientData: ClientData = ClientData()
    @Transient var interactionData: InteractionData = InteractionData()

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
