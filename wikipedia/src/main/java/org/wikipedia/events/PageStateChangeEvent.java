package org.wikipedia.events;

/**
 * Event fired when the currently displayed PageViewFragment changes state
 */
public class PageStateChangeEvent {
    private final int state;

    public PageStateChangeEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
