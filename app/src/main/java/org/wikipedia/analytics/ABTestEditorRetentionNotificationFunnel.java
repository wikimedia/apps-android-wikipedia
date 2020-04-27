package org.wikipedia.analytics;

public final class ABTestEditorRetentionNotificationFunnel extends ABTestFunnel {
    public ABTestEditorRetentionNotificationFunnel() {
        super("editorRetentionNotification", ABTestFunnel.GROUP_SIZE_2);
    }

    public boolean shouldSeeNotification() {
        return getABTestGroup() == ABTestFunnel.GROUP_1;
    }

    public void logNotificationStage1() {
        logGroupEvent(shouldSeeNotification() ? "editorRetentionNotification1_GroupA" : "editorRetentionNotification1_GroupB");
    }

    public void logNotificationStage2() {
        logGroupEvent(shouldSeeNotification() ? "editorRetentionNotification2_GroupA" : "editorRetentionNotification2_GroupB");
    }
}
