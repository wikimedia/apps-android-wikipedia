package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.event.Event

class ExperimentImpl(
    val name: String,
    val group: String,
    val subjectId: String? = null,
    val isLoggable: () -> Boolean = { true },
    val coordinator: String = Event.EventExperiment.COORDINATOR_CUSTOM
)
