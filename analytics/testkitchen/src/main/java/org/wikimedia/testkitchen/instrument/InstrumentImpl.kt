package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.InteractionData

class InstrumentImpl(
    val name: String,
    private val client: TestKitchenClient
) {
    var funnel: Funnel? = null

    fun submitInteraction(action: String, actionSource: String? = null, elementId: String? = null) {
        client.submitInteraction(
            this,
            InteractionData(
                action = action,
                actionSource = actionSource,
                elementId = elementId
            )
        )
        funnel?.touch()
    }

    fun startFunnel(name: String? = null) {
        funnel = Funnel(name)
    }

    fun stopFunnel() {
        funnel = null
    }
}
