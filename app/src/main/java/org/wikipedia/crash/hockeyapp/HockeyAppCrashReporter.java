package org.wikipedia.crash.hockeyapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.ExceptionHandler;

import org.wikipedia.WikipediaApp;
import org.wikipedia.crash.BaseCrashReporter;
import org.wikipedia.crash.CrashReportActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class HockeyAppCrashReporter extends BaseCrashReporter {
    @NonNull private final String appId;
    @NonNull private final CrashListener crashListener;

    public HockeyAppCrashReporter(@NonNull String appId,
                                  @NonNull AutoUploadConsentAccessor consentAccessor) {
        this.appId = appId;
        crashListener = new CrashListener(consentAccessor);
    }

    @Override
    public void registerCrashHandler(@NonNull Context context) {
        L.v("Registering for HockeyApp crash handling.");
        HockeyAppExceptionHandler handler = new HockeyAppExceptionHandler(crashListener, true);
        handler.install();
        CrashManager.initialize(context, appId, crashListener);
    }

    @Override
    public void checkCrashes(@NonNull Activity activity) {
        L.v("Checking for HockeyApp crashes.");
        CrashManager.register(activity, appId, crashListener);
    }

    @Override
    public void log(@NonNull Throwable throwable) {
        ExceptionHandler.saveException(throwable, Thread.currentThread(), crashListener);
    }

    private class CrashListener extends HockeyAppCrashListener {
        @NonNull private final AutoUploadConsentAccessor consentAccessor;
        CrashListener(@NonNull AutoUploadConsentAccessor consentAccessor) {
            this.consentAccessor = consentAccessor;
        }

        @Override
        public String getDescription() {
            super.getDescription();
            return getPropsJson();
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
