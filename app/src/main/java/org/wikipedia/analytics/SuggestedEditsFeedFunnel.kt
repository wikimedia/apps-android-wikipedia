package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.descriptions.DescriptionEditActivity

class SuggestedEditsFeedFunnel(private val type: DescriptionEditActivity.Action, private val source: InvokeSource) :
        Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REVISION, SAMPLE_LOG_ALL) {

    fun start() {
        log("action", "start")
    }

    fun stop() {
        log("action", "stop")
    }

    fun editSuccess() {
        log("action", "editSuccess")
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", source.value)
        preprocessData(eventData, "type", if (type === DescriptionEditActivity.Action.ADD_IMAGE_TAGS) "tags"
        else if (type === DescriptionEditActivity.Action.ADD_CAPTION || type === DescriptionEditActivity.Action.TRANSLATE_CAPTION) "captions"
        else "descriptions")
        return super.preprocessData(eventData)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppSuggestedEditsFeed"
        private const val REVISION = 20437611
    }
}
