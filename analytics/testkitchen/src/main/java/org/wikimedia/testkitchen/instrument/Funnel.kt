package org.wikimedia.testkitchen.instrument

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wikimedia.testkitchen.SessionController

@Parcelize
class Funnel(
    val name: String? = null,
    val token: String = SessionController.generateSessionId(),
    private var sequencePosition: Int = 1,
    private var startTime: Long = System.currentTimeMillis()
) : Parcelable {
    val sequence get() = sequencePosition

    @IgnoredOnParcel
    private var pauseTime = 0L

    fun touch() {
        sequencePosition++
    }

    val duration get() = (System.currentTimeMillis() - startTime).toInt()

    fun pause() {
        pauseTime = System.currentTimeMillis()
    }

    fun resume() {
        if (pauseTime > 0) {
            startTime += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
    }

    fun reset() {
        startTime = System.currentTimeMillis()
    }

    fun addActionContext(actionContext: MutableMap<String, String>) {
        actionContext["time_spent_ms"] = duration.toString()
    }
}
