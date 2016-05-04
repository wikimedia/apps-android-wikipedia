package org.wikipedia.dataclient.retrofit;

import retrofit2.Response;
import retrofit2.Retrofit;

public final class RetrofitExceptionBuilder {
    public static RetrofitException build(Response response, Retrofit retrofit) {
        return RetrofitException.httpError(response.raw().request().url().toString(), response, retrofit);
    }

    private RetrofitExceptionBuilder() {
    }
}
