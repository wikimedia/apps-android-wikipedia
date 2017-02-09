package org.wikipedia.analytics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

import java.util.concurrent.TimeUnit;

// https://meta.wikimedia.org/wiki/Schema:MobileWikiAppDailyStats
public class DailyStatsFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppDailyStats";
    private static final int SCHEMA_REVISION = 12637385;

    public DailyStatsFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, SCHEMA_REVISION, Funnel.SAMPLE_LOG_100);
    }

    public void log(Context context) {
        log(getInstallAgeDays(context));
    }

    public void log(long appInstallAgeDays) {
        log("appInstallAgeDays", appInstallAgeDays);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    private long getInstallAgeDays(Context context) {
        return TimeUnit.MILLISECONDS.toDays(getInstallAge(context));
    }

    private long getInstallAge(Context context) {
        return getAbsoluteTime() - getInstallTime(context);
    }

    /** @return The absolute time since initial app install in milliseconds. */
    private long getInstallTime(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private long getAbsoluteTime() {
        return System.currentTimeMillis();
    }
}
