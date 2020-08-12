package org.wikipedia.analytics;

public final class ABTestDescriptionEditChecksFunnel extends ABTestFunnel {
    public ABTestDescriptionEditChecksFunnel() {
        super("descriptionEditChecks", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeChecks() {
        boolean enrolled = isEnrolled();
        boolean showChecks = getABTestGroup() == ABTestFunnel.GROUP_1;
        if (!enrolled) {
            log(showChecks);
        }
        return showChecks;
    }

    private void log(boolean showChecks) {
        logGroupEvent(showChecks ? "descriptionEditChecks_GroupA" : "descriptionEditChecks_GroupB");
    }
}
