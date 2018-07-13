package org.wikipedia.analytics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.util.StringUtil;

import java.util.concurrent.TimeUnit;

// https://meta.wikimedia.org/wiki/Schema:MobileWikiAppDailyStats
public class DailyStatsFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppDailyStats";
    private static final int SCHEMA_REVISION = 18115101;

    public DailyStatsFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, SCHEMA_REVISION, Funnel.SAMPLE_LOG_ALL);
    }

    public void log(Context context) {
        log("appInstallAgeDays", getInstallAgeDays(context),
                "languages", StringUtil.listToJsonArrayString(getApp().language().getAppLanguageCodes()),
                "is_anon", !AccountUtil.isLoggedIn());
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
