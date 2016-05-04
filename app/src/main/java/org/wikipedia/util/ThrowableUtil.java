package org.wikipedia.util;

import org.wikipedia.R;
import org.mediawiki.api.json.ApiException;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONException;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.net.ssl.SSLException;

import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

public final class ThrowableUtil {

    // TODO: replace with Apache Commons Lang ExceptionUtils.
    @NonNull
    public static Throwable getInnermostThrowable(@NonNull Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    // TODO: replace with Apache Commons Lang ExceptionUtils.
    public static boolean throwableContainsException(@NonNull Throwable e, Class<?> exClass) {
        Throwable t = e;
        while (t != null) {
            if (t.getClass().equals(exClass)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @NonNull
    public static AppError getAppError(@NonNull Context context, @NonNull Throwable e) {
        Throwable inner = ThrowableUtil.getInnermostThrowable(e);
        AppError result;
        // look at what kind of exception it is...
        if (inner instanceof ApiException) {
            // it's a well-formed error response from the server!
            result = new AppError(getApiError(context, (ApiException) inner),
                                  getApiErrorMessage(context, (ApiException) inner));
        } else if (isNetworkError(e)) {
            // it's a network error...
            result = new AppError(context.getString(R.string.error_network_error),
                                  context.getString(R.string.format_error_server_message,
                                      inner.getLocalizedMessage()));
        } else if (ThrowableUtil.throwableContainsException(e, JSONException.class)) {
            // it's a json exception
            result = new AppError(context.getString(R.string.error_response_malformed),
                                  inner.getLocalizedMessage());
        } else {
            // everything else has fallen through, so just treat it as an "unknown" error
            result = new AppError(context.getString(R.string.error_unknown),
                                  inner.getLocalizedMessage());
        }
        return result;
    }

    public static boolean isRetryable(@NonNull Context ctx, @NonNull Throwable e) {
        return isRetryable(ThrowableUtil.getAppError(ctx, e));
    }

    public static boolean isRetryable(@NonNull ThrowableUtil.AppError e) {
        return !(e.getDetail() != null && e.getDetail().contains("404"));
    }

    private static boolean isNetworkError(@NonNull Throwable e) {
        return ThrowableUtil.throwableContainsException(e, HttpRequest.HttpRequestException.class)
               || ThrowableUtil.throwableContainsException(e, UnknownHostException.class)
               || ThrowableUtil.throwableContainsException(e, TimeoutException.class)
               || ThrowableUtil.throwableContainsException(e, SSLException.class);
    }

    @NonNull
    private static String getApiError(@NonNull Context context, @NonNull ApiException e) {
        String text;
        if ("missingtitle".equals(e.getCode()) || "invalidtitle".equals(e.getCode())) {
            text = context.getResources().getString(R.string.page_does_not_exist_error);
        } else {
            text = context.getString(R.string.error_server_response);
        }
        return text;
    }

    // TODO: migrate this to ApiException.toString()
    @NonNull
    private static String getApiErrorMessage(@NonNull Context c, @NonNull ApiException e) {
        String text;
        if (e.getInfo() != null) {
            // if we have an actual message from the server, then prefer it
            text = c.getString(R.string.format_error_server_message, e.getInfo());
        } else if (e.getCode() != null) {
            // otherwise, just show the error code
            text = c.getString(R.string.format_error_server_code, e.getCode());
        } else {
            // if all else fails, show the message of the exception
            text = e.getMessage();
        }
        return text;
    }

    public static class AppError {
        private String error;
        private String detail;
        public AppError(@NonNull String error, @Nullable String detail) {
            this.error = error;
            this.detail = detail;
        }
        @NonNull
        public String getError() {
            return error;
        }
        @Nullable
        public String getDetail() {
            return detail;
        }
    }

    private ThrowableUtil() { }
}