package org.wikipedia.analytics

import org.json.JSONException
import org.json.JSONObject

/**
 * Base class for all various types of events that are logged to EventLogging.
 *
 * Each Schema has its own class, and has its own constructor that makes it easy
 * to call from everywhere without having to duplicate param info at all places.
 * Updating schemas / revisions is also easier this way.
 */
class EventLoggingEvent(schema: String, revID: Int, wiki: String, eventData: JSONObject) {

    val data = JSONObject()

    init {
        try {
            data.put("schema", schema)
            data.put("revision", revID)
            data.put("wiki", wiki)
            data.put("event", eventData)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }
}
