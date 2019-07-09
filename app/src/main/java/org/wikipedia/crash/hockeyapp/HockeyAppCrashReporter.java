package org.wikipedia.crash.hockeyapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.ExceptionHandler;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.crash.CrashReportActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;
import org.wikipedia.util.log.RemoteExceptionLogger;

import java.util.HashMap;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class HockeyAppCrashReporter implements RemoteExceptionLogger {
    public interface AutoUploadConsentAccessor {
        boolean isAutoUploadPermitted();
    }

    @NonNull private final String appId;
    @NonNull private final CrashListener crashListener;
    @NonNull private final Map<String, String> props = new HashMap<>();

    public HockeyAppCrashReporter(@NonNull String appId,
                                  @NonNull AutoUploadConsentAccessor consentAccessor) {
        this.appId = appId;
        crashListener = new CrashListener(consentAccessor);
        new HockeyAppExceptionHandler(crashListener, true).install();
    }

    public void checkCrashes(@NonNull Activity activity) {
        L.v("Checking for HockeyApp crashes.");
        CrashManager.register(activity, appId, crashListener);
    }

    @Override
    public void log(@NonNull Throwable throwable) {
        ExceptionHandler.saveException(throwable, Thread.currentThread(), crashListener);
    }

    /**
     * HockeyApp doesn't seem to offer custom properties, so these are bundled as JSON in the report
     * description. Since these properties are not associated with a crash instance and not
     * preserved across application death, and crashes may enqueue, it's possible they may be
     * inaccurate. However, these properties are used in one place presently and the current
     * implementation should be adequate.
     */
    public HockeyAppCrashReporter putReportProperty(String key, String value) {
        getProps().put(key, value);
        return this;
    }

    @NonNull protected Map<String, String> getProps() {
        return props;
    }

    private class CrashListener extends HockeyAppCrashListener {
        @NonNull private final AutoUploadConsentAccessor consentAccessor;
        CrashListener(@NonNull AutoUploadConsentAccessor consentAccessor) {
            this.consentAccessor = consentAccessor;
        }

        @Override
        public String getDescription() {
            super.getDescription();
            return new JSONObject(getProps()).toString();
        }

        @Override
        public boolean shouldAutoUploadCrashes() {
            super.shouldAutoUploadCrashes();
            return consentAccessor.isAutoUploadPermitted();
        }

        @Override
        public void onCrashesSent() {
            super.onCrashesSent();
            L.v("Crash report(s) sent.");
        }

        @Override
        public boolean ignoreDefaultHandler() {
            super.ignoreDefaultHandler();
            return true;
        }

        @Override
        public void onCrashesNotSent() {
            super.onCrashesNotSent();
            L.d("Crash report(s) not sent.");
        }

        @Override
        public void onCrash() {
            if (!Prefs.crashedBeforeActivityCreated()) {
                Prefs.crashedBeforeActivityCreated(true);
                launchCrashReportActivity();
            } else {
                L.i("Crashed before showing UI. Skipping reboot.");
            }
            terminateApp();
        }

        private void launchCrashReportActivity() {
            Context context = WikipediaApp.getInstance();
            int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK;
            Intent intent = new Intent(context, CrashReportActivity.class).addFlags(flags);
            context.startActivity(intent);
        }

        private void terminateApp() {
            Runtime.getRuntime().exit(0);
        }
    }
}
