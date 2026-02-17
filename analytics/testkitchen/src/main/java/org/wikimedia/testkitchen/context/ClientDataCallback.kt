package org.wikimedia.testkitchen.context

interface ClientDataCallback {
    fun getAgentData(): AgentData?
    fun getPageData(): PageData?
    fun getMediawikiData(): MediawikiData?
    fun getPerformerData(): PerformerData?
    fun getDomain(): String?
}
