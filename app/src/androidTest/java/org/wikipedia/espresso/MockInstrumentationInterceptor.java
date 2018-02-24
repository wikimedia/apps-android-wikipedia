package org.wikipedia.espresso;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.dataclient.okhttp.TestStubInterceptor;
import org.wikipedia.util.FileUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("checkstyle:magicnumber")
public class MockInstrumentationInterceptor implements TestStubInterceptor.Callback {
    private static final MediaType MEDIA_JSON = MediaType.parse("application/json");
    private final Context context;

    private static Map<String, String> RESPONSE_MAP = new HashMap<>();
    static {
        RESPONSE_MAP.put("feed/featured", "espresso/aggregate_feed_response.json");
        RESPONSE_MAP.put("feed/announcements", "espresso/dynamic_announcement_response.json");
        RESPONSE_MAP.put("mobile-sections-lead/Barack_Obama", "espresso/lead_section_response_obama.json");
        RESPONSE_MAP.put("https://upload.wikimedia.org/wikipedia/commons", "espresso/empty_image_response.json");
    }

    public MockInstrumentationInterceptor(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public Response getResponse(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        String responseFile = getResponseFileByUrl(request.url().toString());
        if (!TextUtils.isEmpty(responseFile)) {
            String json = FileUtil.readFile(context.getAssets().open(responseFile));
            return new Response.Builder()
                    .body(ResponseBody.create(MEDIA_JSON, json))
                    .message("Success")
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .build();

        } else if (request.url().toString().contains("/beacon/event")) {
            // event logging message, which should always return no content
            return new Response.Builder().request(request).protocol(Protocol.HTTP_2)
                    .body(ResponseBody.create(MEDIA_JSON, ""))
                    .message("No content (test)").code(204).build();
        } else {
            // The request doesn't exist in our collection, so return 404
            return new Response.Builder().request(request).protocol(Protocol.HTTP_2)
                    .body(ResponseBody.create(MEDIA_JSON, ""))
                    .message("Not found (test)").code(404).build();
        }
    }

    private static String getResponseFileByUrl(String url) {
        for (String key : RESPONSE_MAP.keySet()) {
            if (url.contains(key)) {
                return RESPONSE_MAP.get(key);
            }
        }
        return "";
    }
}
