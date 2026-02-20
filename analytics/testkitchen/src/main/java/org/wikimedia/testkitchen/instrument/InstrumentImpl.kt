package org.wikimedia.testkitchen.instrument

import kotlinx.serialization.json.Json
import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.PageData

class InstrumentImpl(
    val name: String,
    private val client: TestKitchenClient? = null
) {
    var funnel: Funnel? = null
    var experiment: ExperimentImpl? = null

    fun submitInteraction(action: String, actionSource: String? = null, elementId: String? = null, pageData: PageData? = null, actionContext: Map<String, Any>? = null) {
        if (experiment?.isLoggable() == false) {
            return
        }
        val actionContextFinal = mutableMapOf<String, String>()
        funnel?.addActionContext(actionContextFinal)
        actionContext?.let {
            actionContextFinal.putAll(it.mapValues { (_, value) -> value.toString() })
        }
        client?.submitInteraction(
            instrument = this,
            interactionData = InteractionData(
                action = action,
                actionSource = actionSource,
                elementId = elementId,
                actionContext = if (actionContextFinal.isNotEmpty()) Json.encodeToString(actionContextFinal) else null
            ),
            pageData = pageData
        )
        funnel?.touch()
    }

    fun startFunnel(name: String? = null): InstrumentImpl {
        funnel = Funnel(name)
        return this
    }

    fun stopFunnel(): InstrumentImpl {
        funnel = null
        return this
    }

    fun setExperiment(name: String, group: String): InstrumentImpl {
        experiment = ExperimentImpl(name, group)
        return this
    }

    fun setExperiment(experiment: ExperimentImpl?): InstrumentImpl {
        this.experiment = experiment
        return this
    }
}
