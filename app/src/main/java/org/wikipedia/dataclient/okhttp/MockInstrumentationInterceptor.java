package org.wikipedia.dataclient.okhttp;

import android.content.Context;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("checkstyle:magicnumber")
public class MockInstrumentationInterceptor implements Interceptor {
    private static final MediaType MEDIA_JSON = MediaType.parse("application/json");
    private static Map<String, String> RESPONSE_MAP = new HashMap<>();
    private static Context INSTRUMENTATION_CONTEXT;

    MockInstrumentationInterceptor() {
    }

    public static void setInstrumentationContext(Context instrumentationContext) {
        MockInstrumentationInterceptor.INSTRUMENTATION_CONTEXT = instrumentationContext;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        populateList();
        Request request = chain.request();
        if (isTestMode() && INSTRUMENTATION_CONTEXT != null) {

            String jsonKey = getJsonKey(request.url().toString());
            if (!TextUtils.isEmpty(jsonKey)) {
                InputStream referenceInputStream = INSTRUMENTATION_CONTEXT.getAssets().open(RESPONSE_MAP.get(jsonKey));
                String json = readFileFromIS(referenceInputStream);
                return new Response.Builder()
                        .body(ResponseBody.create(MEDIA_JSON, json))
                        .message("Success")
                        .request(request)
                        .protocol(Protocol.HTTP_2)
                        .code(200)
                        .build();

            }
        }
        return chain.proceed(request);

    }

    private String readFileFromIS(InputStream iStream) {
        ByteArrayOutputStream byteStream = null;
        try {
            byte[] buffer = new byte[iStream.available()];
            iStream.read(buffer);
            byteStream = new ByteArrayOutputStream();
            byteStream.write(buffer);
            byteStream.close();
            iStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toString();
    }

    private static boolean isTestMode() {
        boolean result;
        try {
            Class.forName("org.wikipedia.espresso.InstrumentationTestSuite");
            result = true;
        } catch (final Exception e) {
            result = false;
        }
        return result;
    }

    private static void populateList() {
        RESPONSE_MAP.clear();

        RESPONSE_MAP.put("feed/featured", "espresso/aggregate_feed_response.json");
        RESPONSE_MAP.put("feed/announcements", "espresso/dynamic_announcement_response.json");
        RESPONSE_MAP.put("mobile-sections-lead/Barack_Obama", "espresso/lead_section_response_obama.json");
        RESPONSE_MAP.put("https://upload.wikimedia.org/wikipedia/commons", "espresso/empty_image_response.json");
    }

    private static String getJsonKey(String request) {
        Iterator it = RESPONSE_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (request.contains(pair.getKey().toString())) {
                return pair.getKey().toString();
            }
            it.remove(); // To avoid a ConcurrentModificationException
        }
        return "";
    }
}
