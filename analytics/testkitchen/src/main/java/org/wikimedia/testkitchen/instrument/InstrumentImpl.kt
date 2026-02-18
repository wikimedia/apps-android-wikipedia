package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.PageData

class InstrumentImpl(
    val name: String,
    private val client: TestKitchenClient? = null
) {
    var funnel: Funnel? = null

    fun submitInteraction(action: String, actionSource: String? = null, elementId: String? = null, pageData: PageData? = null) {
        client?.submitInteraction(
            instrument = this,
            interactionData = InteractionData(
                action = action,
                actionSource = actionSource,
                elementId = elementId
            ),
            pageData = pageData
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
