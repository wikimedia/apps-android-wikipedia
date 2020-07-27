package org.wikipedia.analytics;

import org.wikipedia.util.ReleaseUtil;

public final class ABTestDescriptionEditChecksFunnel extends ABTestFunnel {
    public ABTestDescriptionEditChecksFunnel() {
        super("descriptionEditChecks", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeChecks() {
        if (ReleaseUtil.isPreBetaRelease()) {
            // TODO: expose A/B tests as developer preferences.
            return true;
        }
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
