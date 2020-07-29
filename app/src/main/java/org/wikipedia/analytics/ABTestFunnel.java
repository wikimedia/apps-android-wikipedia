package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.PrefsIoUtil;

import java.util.Random;

public class ABTestFunnel extends Funnel {
    public static final int GROUP_SIZE_2 = 2;
    public static final int GROUP_SIZE_3 = 3;

    public static final int GROUP_1 = 0;
    public static final int GROUP_2 = 1;
    public static final int GROUP_3 = 2;

    private static final String SCHEMA_NAME = "MobileWikiAppABTest";
    private static final int REV_ID = 19990870;

    private static final String AB_TEST_KEY_PREFIX = "ab_test_";
    private final String abTestName;
    private final int abTestGroupCount;

    ABTestFunnel(@NonNull String abTestName, int abTestGroupCount) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.abTestName = abTestName;
        this.abTestGroupCount = abTestGroupCount;
    }

    int getABTestGroup() {
        int group = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1);
        if (group == -1) {
            // initialize the group if it hasn't been yet.
            group = new Random().nextInt(Integer.MAX_VALUE);
            PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, group);
        }
        return group % abTestGroupCount;
    }

    protected boolean isEnrolled() {
        return PrefsIoUtil.contains(AB_TEST_KEY_PREFIX + abTestName);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    void logGroupEvent(@NonNull String groupEventId) {
        log(
                "test_group", groupEventId
        );
    }
}
