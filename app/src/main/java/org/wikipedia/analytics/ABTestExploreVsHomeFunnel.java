package org.wikipedia.analytics;

public final class ABTestExploreVsHomeFunnel extends ABTestFunnel {
    public ABTestExploreVsHomeFunnel() {
        super("exploreVsHome", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeHome() {
        boolean enrolled = isEnrolled();
        boolean showHome = getABTestGroup() == ABTestFunnel.GROUP_1;
        if (!enrolled) {
            log(showHome);
        }
        return showHome;
    }

    private void log(boolean showChecks) {
        logGroupEvent(showChecks ? "exploreVsHome_GroupA" : "exploreVsHome_GroupB");
    }
}
