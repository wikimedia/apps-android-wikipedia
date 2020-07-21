package org.wikipedia.analytics;

import org.wikipedia.settings.Prefs;

public final class ABTestSuggestedEditsOnboardingIconFunnel extends ABTestFunnel {
    private boolean iconShown;

    public ABTestSuggestedEditsOnboardingIconFunnel() {
        super("suggestedEditsOnboardingIcon", ABTestFunnel.GROUP_SIZE_2);
    }

    public void setIconShown(boolean shown) {
        iconShown = shown;
    }

    public void logWasIconClicked(boolean clicked) {
        if (!Prefs.isPulsatingIconEventSent() && iconShown) {
            logGroupEvent(clicked ? "seAnimation_GroupA" : "seAnimation_GroupB");
            Prefs.setPulsatingIconEventSent(true);
        }
    }
}
