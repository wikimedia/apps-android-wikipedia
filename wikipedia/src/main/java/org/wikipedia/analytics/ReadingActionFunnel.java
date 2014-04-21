package org.wikipedia.analytics;

import android.content.*;
import android.preference.*;
import android.text.format.*;
import org.wikipedia.*;

import java.util.*;

public class ReadingActionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppReadingAction";
    private static final int REV_ID = 8233801;

    private static final String APP_ID_PREF_NAME = "ANALYTICS_APP_ID_FOR_READING";

    private final String appInstallReadActionID;
    public ReadingActionFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REV_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefs.contains(APP_ID_PREF_NAME)) {
            appInstallReadActionID = prefs.getString(APP_ID_PREF_NAME, null);
        } else {
            appInstallReadActionID = UUID.randomUUID().toString();
            prefs.edit().putString(APP_ID_PREF_NAME, appInstallReadActionID).commit();
        }
    }

    public void logSomethingHappened(Site site) {
        log(
                site,
                "appInstallReadActionID", appInstallReadActionID,
                // clientSideTS is Unix Timestamp, so is in seconds. Java's is in Milliseconds
                "clientSideTS", new Date().getTime() / DateUtils.SECOND_IN_MILLIS
        );
    }
}
