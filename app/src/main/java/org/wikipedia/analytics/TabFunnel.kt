package org.wikipedia.analytics

import org.wikipedia.WikipediaApp
import java.util.*

class TabFunnel : Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, SCHEMA_REVISION, SAMPLE_LOG_100) {

    fun logOpenInNew(size: Int) {
        log("openInNew", size)
    }

    fun logEnterList(size: Int) {
        log("enterList", size)
    }

    fun logCreateNew(size: Int) {
        log("createNew", size)
    }

    fun logClose(size: Int, index: Int) {
        log("close", size, index)
    }

    fun logSelect(size: Int, index: Int) {
        log("select", size, index)
    }

    fun logCancel(size: Int) {
        log("cancel", size)
    }

    private fun log(action: String, size: Int, index: Int? = null) {
        val params: MutableList<Any> = ArrayList()
        params.add("action")
        params.add(action)
        params.add("tabCount")
        params.add(size)
        if (index != null) {
            params.add("tabIndex")
            params.add(index)
        }
        log(*params.toTypedArray())
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppTabs"
        private const val SCHEMA_REVISION = 18118767
    }
}
