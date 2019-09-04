package org.wikipedia.util.log;

import androidx.annotation.NonNull;

public interface RemoteExceptionLogger {
    void log(@NonNull Throwable throwable);
}
