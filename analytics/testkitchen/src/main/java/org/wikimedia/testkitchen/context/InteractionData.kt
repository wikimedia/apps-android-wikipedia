package org.wikimedia.testkitchen.context


/**
 * Interaction data fields.
 *
 * Common interaction fields that describe the event being submitted. Most fields are nullable.
 */
class InteractionData(
    val action: String? = null,
    val actionSubtype: String? = null,
    val actionSource: String? = null,
    val actionContext: String? = null,
    val elementId: String? = null,
    val elementFriendlyName: String? = null,
    val funnelEntryToken: String? = null,
    val funnelEventSequencePosition: Int? = null
)
