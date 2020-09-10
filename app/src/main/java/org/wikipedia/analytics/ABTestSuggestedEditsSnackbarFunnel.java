package org.wikipedia.analytics;

import org.wikipedia.util.ReleaseUtil;

public final class ABTestSuggestedEditsSnackbarFunnel extends ABTestFunnel {
    public ABTestSuggestedEditsSnackbarFunnel() {
        super("suggestedEditsSnackbar", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeSnackbarAction() {
        return (getABTestGroup() == ABTestFunnel.GROUP_1)
                || ReleaseUtil.isPreBetaRelease();
    }

    public void logSnackbarShown() {
        logGroupEvent(shouldSeeSnackbarAction() ? "suggestedEditsSnackbar_GroupA" : "suggestedEditsSnackbar_GroupB");
    }
}
