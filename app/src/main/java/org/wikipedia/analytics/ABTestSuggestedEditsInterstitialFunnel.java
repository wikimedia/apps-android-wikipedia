package org.wikipedia.analytics;

public final class ABTestSuggestedEditsInterstitialFunnel extends ABTestFunnel {
    public ABTestSuggestedEditsInterstitialFunnel() {
        super("suggestedEditsInterstitial", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeInterstitial() {
        return getABTestGroup() == ABTestFunnel.GROUP_1;
    }

    public void logInterstitialShown() {
        logGroupEvent(shouldSeeInterstitial() ? "suggestedEditsInterstitial_GroupA" : "suggestedEditsInterstitial_GroupB");
    }
}
