package org.wikimedia.testkitchen.instrument

import kotlinx.serialization.json.Json
import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikimedia.testkitchen.context.PageData

class InstrumentImpl(
    val name: String,
    private val client: TestKitchenClient? = null
) {
    var funnel: Funnel? = null
    var experiment: ExperimentImpl? = null
    private var defaultActionSource: String? = null
    private var defaultMediaWikiData: MediawikiData? = null

    fun submitInteraction(
        action: String,
        actionSource: String? = null,
        actionSubtype: String? = null,
        elementId: String? = null,
        elementFriendlyName: String? = null,
        pageData: PageData? = null,
        mediawikiData: MediawikiData? = null,
        actionContext: Map<String, Any>? = null
    ) {
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
                actionSource = actionSource ?: defaultActionSource,
                actionSubtype = actionSubtype,
                elementId = elementId,
                elementFriendlyName = elementFriendlyName,
                actionContext = if (actionContextFinal.isNotEmpty()) Json.encodeToString(actionContextFinal) else null
            ),
            pageData = pageData,
            mediawikiData = mediawikiData ?: defaultMediaWikiData
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

    fun setDefaultActionSource(source: String): InstrumentImpl {
        defaultActionSource = source
        return this
    }

    fun setDefaultMediaWikiData(dbName: String): InstrumentImpl {
        defaultMediaWikiData = MediawikiData(dbName)
        return this
    }
}
