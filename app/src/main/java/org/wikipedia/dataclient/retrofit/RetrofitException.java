package org.wikipedia.dataclient.retrofit;

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
        return new RetrofitException(message, url, response, Kind.HTTP, null);
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
    private final Response<?> response;
    private final Kind kind;

    RetrofitException(String message, String url, Response<?> response, Kind kind, Throwable exception) {
        super(message, exception);
        this.url = url;
        this.response = response;
        this.kind = kind;
    }

    /** The request URL which produced the error. */
    public String getUrl() {
        return url;
    }

    /** Response object containing status code, headers, body, etc. */
    public Response<?> getResponse() {
        return response;
    }

    /** The event kind which triggered this error. */
    public Kind getKind() {
        return kind;
    }
}
