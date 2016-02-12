package org.wikipedia.dataclient;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.internal.http.HttpMethod;

import java.io.IOException;
import java.io.OutputStream;

import retrofit.client.OkClient;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedOutput;

/** Workaround for https://github.com/square/retrofit/issues/854. */
public class NullBodyAwareOkClient extends OkClient {
    public NullBodyAwareOkClient(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    @Override
    public Response execute(Request request) throws IOException {
        if (HttpMethod.requiresRequestBody(request.getMethod()) && request.getBody() == null) {
            Request newRequest = new Request(request.getMethod(), request.getUrl(),
                    request.getHeaders(), EmptyOutput.INSTANCE);
            return super.execute(newRequest);
        }

        return super.execute(request);
    }

    private static final class EmptyOutput implements TypedOutput {
        static final TypedOutput INSTANCE = new EmptyOutput();

        private EmptyOutput() { }

        @Override
        public String fileName() {
            return null;
        }

        @Override
        public String mimeType() {
            return "application/json";
        }

        @Override
        public long length() {
            return 0;
        }

        @Override public void writeTo(OutputStream out) throws IOException { }
    }
}