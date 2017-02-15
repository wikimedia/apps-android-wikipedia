package org.wikipedia.crash;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.util.log.RemoteExceptionLogger;

public interface CrashReporter extends RemoteExceptionLogger {
    interface AutoUploadConsentAccessor {
        boolean isAutoUploadPermitted();
    }

    CrashReporter putReportProperty(String key, String value);
    void registerCrashHandler(@NonNull Context context);
    void checkCrashes(@NonNull Activity activity);
}
