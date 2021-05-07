package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import java.util.concurrent.TimeUnit

abstract class TimedFunnel @JvmOverloads constructor(app: WikipediaApp, schemaName: String, revision: Int, sampleRate: Int, wiki: WikiSite? = null) :
        Funnel(app, schemaName, revision, sampleRate, wiki) {

    private var startTime = System.currentTimeMillis()
    private var pauseTime = 0L
    /** Override me for deviant implementations.  */
    private val durationFieldName = "time_spent"
    private val duration = System.currentTimeMillis() - startTime
    private val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(duration)

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, durationFieldName, durationSeconds)
        return super.preprocessData(eventData)
    }

    fun pause() {
        pauseTime = System.currentTimeMillis()
    }

    fun resume() {
        if (pauseTime > 0) {
            startTime += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
    }

    protected fun resetDuration() {
        startTime = System.currentTimeMillis()
    }
}
