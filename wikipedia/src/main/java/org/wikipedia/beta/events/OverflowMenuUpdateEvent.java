package org.wikipedia.beta.events;

/**
 * Event fired when the overflow menu needs to be updated (e.g. upon loading of a page)
 */
public class OverflowMenuUpdateEvent {
    private final int state;
    private final int substate;

    public OverflowMenuUpdateEvent(int state, int substate) {
        this.state = state;
        this.substate = substate;
    }

    public int getState() {
        return state;
    }

    public int getSubstate() {
        return substate;
    }
}
