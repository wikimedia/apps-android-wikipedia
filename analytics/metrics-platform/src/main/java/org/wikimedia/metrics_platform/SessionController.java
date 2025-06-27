package org.wikimedia.metrics_platform;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Manages sessions and session IDs for the Metrics Platform Client.
 *
 * A session begins when the application is launched and expires when the app is in the background
 * for 30 minutes or more.
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class SessionController {

    private static final Duration SESSION_LENGTH = Duration.ofMinutes(30);
    @GuardedBy("this")
    @Nonnull
    private String sessionId = generateSessionId();
    @GuardedBy("this")
    @Nonnull
    private Instant sessionTouched;
    private static final Random RANDOM = new SecureRandom();

    SessionController() {
        this(Instant.now());
    }

    /**
     * Constructor for testing.
     *
     * @param date session start time
     */
    SessionController(Instant date) {
        this.sessionTouched = date;
    }

    @Nonnull
    synchronized String getSessionId() {
        return sessionId;
    }

    synchronized void touchSession() {
        if (sessionExpired()) {
            sessionId = generateSessionId();
        }

        sessionTouched = Instant.now();
    }

    synchronized void beginSession() {
        sessionId = generateSessionId();
        sessionTouched = Instant.now();
    }

    synchronized void closeSession() {
        // @ToDo Determine how to close the session.
        sessionTouched = Instant.now();
    }

    synchronized boolean sessionExpired() {
        return Duration.between(sessionTouched, Instant.now()).compareTo(SESSION_LENGTH) >= 0;
    }

    @Nonnull
    private static String generateSessionId() {
        Random random = RANDOM;
        return String.format(Locale.US, "%08x", random.nextInt()) +
                String.format(Locale.US, "%08x", random.nextInt()) +
                String.format(Locale.US, "%04x", random.nextInt() & 0xFFFF);
    }
}
