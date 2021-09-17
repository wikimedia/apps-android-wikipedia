package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.PageTitle
import java.util.*

class TalkFunnel constructor(private val title: PageTitle, private val invokeSource: Constants.InvokeSource) :
        TimedFunnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", invokeSource.value)
        preprocessData(eventData, "anon", !AccountUtil.isLoggedIn)
        preprocessData(eventData, "pageNS", title.namespace.lowercase(Locale.getDefault()).capitalize(Locale.getDefault()))
        return super.preprocessData(eventData)
    }

    fun logOpenTalk() {
        log("action", "open_talk")
    }

    fun logOpenTopic() {
        log("action", "open_topic")
    }

    fun logNewTopicClick() {
        log("action", "new_topic_click")
    }

    fun logReplyClick() {
        log("action", "reply_click")
    }

    fun logRefresh() {
        log("action", "refresh")
    }

    fun logChangeLanguage() {
        log("action", "lang_change")
    }

    fun logEditSubmit() {
        log("action", "submit")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppTalk"
        private const val REV_ID = 21020341
    }
}
