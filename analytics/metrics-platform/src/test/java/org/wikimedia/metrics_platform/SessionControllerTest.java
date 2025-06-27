package org.wikimedia.metrics_platform;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SessionControllerTest {

    @Test void testSessionExpiry() {
        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        SessionController sessionController = new SessionController(oneHourAgo);
        assertThat(sessionController.sessionExpired()).isTrue();
    }

    @Test void testTouchSession() {
        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        SessionController sessionController = new SessionController(oneHourAgo);
        String sessionId1 = sessionController.getSessionId();
        sessionController.touchSession();
        assertThat(sessionController.sessionExpired()).isFalse();

        String sessionId2 = sessionController.getSessionId();

        assertThat(sessionId1).isNotEqualTo(sessionId2);
    }

    @Test void testSessionIdLength() {
        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        SessionController sessionController = new SessionController(twoHoursAgo);
        String sessionId = sessionController.getSessionId();
        assertThat(sessionId.length()).isEqualTo(20);
    }
}
