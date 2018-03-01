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

import static org.wikipedia.espresso.Constants.TEST_JSON_ASSET_FOLDER;

@SuppressWarnings("checkstyle:magicnumber")
public class MockInstrumentationInterceptor implements TestStubInterceptor.Callback {
    private static final MediaType MEDIA_JSON = MediaType.parse("application/json");
    private final Context context;

    private static Map<String, String> RESPONSE_MAP = new HashMap<>();
    static {
        RESPONSE_MAP.put("feed/featured",
                TEST_JSON_ASSET_FOLDER + "aggregate_feed_response.json");

        RESPONSE_MAP.put("feed/announcements",
                TEST_JSON_ASSET_FOLDER + "dynamic_announcement_response.json");

        RESPONSE_MAP.put("/mobile-sections-lead/Barack_Obama",
                TEST_JSON_ASSET_FOLDER + "lead_section_response_obama.json");

        RESPONSE_MAP.put("https://upload.wikimedia.org/wikipedia/commons",
                TEST_JSON_ASSET_FOLDER + "empty_image_response.json");

        RESPONSE_MAP.put("mobile-sections-remaining/Barack_Obama",
                TEST_JSON_ASSET_FOLDER + "remaining_sections_response_obama.json");

        RESPONSE_MAP.put("api.php?action=sitematrix&format=json&smtype=language&smlangprop=code%7Cname%7Clocalname",
                TEST_JSON_ASSET_FOLDER + "search_language_list_response.json");

        RESPONSE_MAP.put("srprop=&sroffset=0&srlimit=1&gpslimit=20&pithumbsize=320&gpssearch=Barack%20Obama&srsearch=Barack%20Obama",
                TEST_JSON_ASSET_FOLDER + "obama_article_search_respose.json");
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
