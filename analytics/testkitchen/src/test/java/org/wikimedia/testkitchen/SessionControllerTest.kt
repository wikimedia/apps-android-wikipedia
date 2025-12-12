package org.wikimedia.testkitchen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SessionControllerTest {
    @Test
    fun testSessionExpiry() {
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)
        val sessionController = SessionController(oneHourAgo)
        assertTrue(sessionController.sessionExpired())
    }

    @Test
    fun testTouchSession() {
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)
        val sessionController = SessionController(oneHourAgo)
        val sessionId1 = sessionController.sessionId
        sessionController.touchSession()
        assertFalse(sessionController.sessionExpired())

        val sessionId2 = sessionController.sessionId
        assertNotEquals(sessionId1, sessionId2)
    }

    @Test
    fun testSessionIdLength() {
        val twoHoursAgo = java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS)
        val sessionController = SessionController(twoHoursAgo)
        val sessionId = sessionController.sessionId
        assertEquals(20, sessionId.length)
    }
}
