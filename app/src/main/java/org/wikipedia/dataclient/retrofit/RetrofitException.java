package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import retrofit2.Response;

/**
 *  This is RetrofitError converted to Retrofit 2
 */
public class RetrofitException extends RuntimeException {
    public static RetrofitException httpError(Response<?> response) {
        return httpError(response.raw().request().url().toString(), response);
    }

    public static RetrofitException httpError(String url, Response<?> response) {
        String message = response.code() + " " + response.message();
        return new RetrofitException(message, url, response.code(), Kind.HTTP, null);
    }

    public static RetrofitException httpError(@NonNull okhttp3.Response response) {
        String message = response.code() + " " + response.message();
        return new RetrofitException(message, response.request().url().toString(), response.code(), Kind.HTTP,
                null);
    }

    public static RetrofitException networkError(IOException exception) {
        return new RetrofitException(exception.getMessage(), null, null, Kind.NETWORK, exception);
    }

    public static RetrofitException unexpectedError(Throwable exception) {
        return new RetrofitException(exception.getMessage(), null, null, Kind.UNEXPECTED, exception);
    }

    /** Identifies the event kind which triggered a {@link RetrofitException}. */
    public enum Kind {
        /** An {@link IOException} occurred while communicating to the server. */
        NETWORK,
        /** A non-200 HTTP status code was received from the server. */
        HTTP,
        /**
         * An internal error occurred while attempting to execute a request. It is best practice to
         * re-throw this exception so your application crashes.
         */
        UNEXPECTED
    }

    private final String url;
    @Nullable private final Integer code;
    private final Kind kind;

    RetrofitException(String message, String url, @Nullable Integer code, Kind kind, Throwable exception) {
        super(message, exception);
        this.url = url;
        this.code = code;
        this.kind = kind;
    }

    /** The request URL which produced the error. */
    public String getUrl() {
        return url;
    }

    /** HTTP status code. */
    @Nullable public Integer getCode() {
        return code;
    }

    /** The event kind which triggered this error. */
    public Kind getKind() {
        return kind;
    }
}
