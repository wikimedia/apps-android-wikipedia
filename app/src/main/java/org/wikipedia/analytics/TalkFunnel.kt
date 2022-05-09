package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.PageTitle

class TalkFunnel constructor(private val title: PageTitle, private val invokeSource: Constants.InvokeSource) :
        TimedFunnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", invokeSource.value)
        preprocessData(eventData, "anon", !AccountUtil.isLoggedIn)
        preprocessData(eventData, "pageNS", title.namespace().code().toString())
        return super.preprocessData(eventData)
    }

    fun logOpenTalk() {
        log("action" to "open_talk")
    }

    fun logOpenTopic() {
        log("action" to "open_topic")
    }

    fun logNewTopicClick() {
        log("action" to "new_topic_click")
    }

    fun logReplyClick() {
        log("action" to "reply_click")
    }

    fun logRefresh() {
        log("action" to "refresh")
    }

    fun logChangeLanguage() {
        log("action" to "lang_change")
    }

    fun logEditSubmit() {
        log("action" to "submit")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppTalk"
        private const val REV_ID = 21020341
    }
}
