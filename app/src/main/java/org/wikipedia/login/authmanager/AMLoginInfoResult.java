package org.wikipedia.login.authmanager;

public class AMLoginInfoResult {

    private boolean enabled;

    public AMLoginInfoResult(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return enabled;
    }
}
