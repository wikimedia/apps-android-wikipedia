package org.wikipedia.crash;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Wrapper around {@link Exception} used to send a JSON payload via {@link CrashReporter}. */
public class RemoteLogException extends Exception {
    @NonNull private final Map<String, String> props = new HashMap<>();

    public RemoteLogException(@Nullable String message) {
        this(message, null);
    }

    public RemoteLogException(@Nullable Throwable throwable) {
        this(null, throwable);
    }

    public RemoteLogException(@Nullable String message, @Nullable Throwable throwable) {
        super(throwable);
        putMessage(message == null && throwable != null ? throwable.getMessage() : message);
    }

    public RemoteLogException put(String key, String value) {
        props.put(key, value);
        return this;
    }

    @Override
    public String getMessage() {
        super.getMessage();
        return propsToJsonMsg();
    }

    private void putMessage(@Nullable String message) {
        if (message != null) {
            props.put("message", message);
        }
    }

    private String propsToJsonMsg() {
        return new JSONObject(props).toString();
    }
}
