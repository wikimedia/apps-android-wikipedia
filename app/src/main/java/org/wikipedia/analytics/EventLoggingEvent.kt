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
class EventLoggingEvent(schema: String?, revID: Int, wiki: String?, eventData: JSONObject?) {
    val data: JSONObject = JSONObject()

    /**
     * Create an EventLoggingEvent that logs to a given revision of a given schema with
     * the gven data payload.
     *
     * @param schema Schema name (as specified on meta.wikimedia.org)
     * @param revID Revision of the schema to log to
     * @param wiki DBName (enwiki, dewiki, etc) of the wiki in which we are operating
     * @param eventData Data for the actual event payload. Considered to be
     */
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
