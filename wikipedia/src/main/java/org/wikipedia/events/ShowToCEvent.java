package org.wikipedia.events;

public class ShowToCEvent {
    public static final int ACTION_SHOW = 0;
    public static final int ACTION_HIDE = 1;
    public static final int ACTION_TOGGLE = 2;
    private int action;

    public ShowToCEvent(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }

}
