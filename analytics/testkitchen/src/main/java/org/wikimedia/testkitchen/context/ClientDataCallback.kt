package org.wikimedia.testkitchen.context

interface ClientDataCallback {
    fun getAgentData(): AgentData?
    fun getMediawikiData(): MediawikiData?
    fun getPerformerData(): PerformerData?
}
