package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import java.util.concurrent.TimeUnit

abstract class TimedFunnel @JvmOverloads constructor(app: WikipediaApp, schemaName: String,
                                                     revision: Int, sampleRate: Int, wiki: WikiSite? = null) :
        Funnel(app, schemaName, revision, sampleRate, wiki) {
    private var startTime: Long
    private var pauseTime: Long = 0
    override fun preprocessData(eventData: JSONObject): JSONObject? {
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

    /** Override me for deviant implementations.  */
    private val durationFieldName: String
        get() = "time_spent"

    protected fun resetDuration() {
        startTime = System.currentTimeMillis()
    }

    private val duration: Long
        get() = System.currentTimeMillis() - startTime
    private val durationSeconds: Long
        get() = TimeUnit.MILLISECONDS.toSeconds(duration)

    init {
        startTime = System.currentTimeMillis()
    }
}
