package org.wikipedia.util;

import java.util.concurrent.TimeUnit;

public class ActiveTimer {
    private long startMillis;
    private long pauseMillis;

    public ActiveTimer() {
        reset();
    }

    public void reset() {
        startMillis = System.currentTimeMillis();
        pauseMillis = startMillis;
    }

    public void pause() {
        pauseMillis = System.currentTimeMillis();
    }

    public void resume() {
        startMillis -= (System.currentTimeMillis() - pauseMillis);
    }

    public int getElapsedSec() {
        return (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startMillis);
    }
}
