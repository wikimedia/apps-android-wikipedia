package org.wikimedia.testkitchen

import java.time.Duration
import java.time.Instant
import java.util.Random

/**
 * Manages sessions and session IDs.
 *
 * A session begins when the application is launched and expires when the app is in the background
 * for SESSION_LENGTH time or more.
 */
class SessionController internal constructor(
    private var sessionTouched: Instant = Instant.now()
) {
    @get:Synchronized
    var sessionId = generateSessionId()
        private set

    @Synchronized
    fun touchSession() {
        if (sessionExpired()) {
            sessionId = generateSessionId()
        }
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun beginSession() {
        sessionId = generateSessionId()
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun closeSession() {
        // TODO: Determine how to close the session.
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun sessionExpired(): Boolean {
        return Duration.between(sessionTouched, Instant.now()) >= SESSION_LENGTH
    }

    companion object {
        private val SESSION_LENGTH = Duration.ofMinutes(30)

        private fun generateSessionId(): String {
            val random = Random()
            return String.format("%08x", random.nextInt()) + String.format("%08x", random.nextInt()) + String.format("%04x", random.nextInt() and 0xFFFF)
        }
    }
}
