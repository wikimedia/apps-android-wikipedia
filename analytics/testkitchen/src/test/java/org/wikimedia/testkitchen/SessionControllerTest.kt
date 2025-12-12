package org.wikimedia.testkitchen

import org.assertj.core.api.Assertions.assertThat

internal class SessionControllerTest {
    @Test
    fun testSessionExpiry() {
        val oneHourAgo = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS)
        val sessionController = SessionController(oneHourAgo)
        assertThat(sessionController.sessionExpired()).isTrue()
    }

    @Test
    fun testTouchSession() {
        val oneHourAgo = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS)
        val sessionController = SessionController(oneHourAgo)
        val sessionId1 = sessionController.sessionId
        sessionController.touchSession()
        assertThat(sessionController.sessionExpired()).isFalse()

        val sessionId2 = sessionController.sessionId

        assertThat(sessionId1).isNotEqualTo(sessionId2)
    }

    @Test
    fun testSessionIdLength() {
        val twoHoursAgo = java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS)
        val sessionController = SessionController(twoHoursAgo)
        val sessionId = sessionController.sessionId
        assertThat(sessionId.length).isEqualTo(20)
    }
}
