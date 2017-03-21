package org.wikipedia.dataclient.okhttp;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpWebViewClient extends WebViewClient {
    private static final List<String> SUPPORTED_SCHEMES = Arrays.asList("http", "https");
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_ENCODING = "content-encoding";

    @SuppressWarnings("deprecation") @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (!SUPPORTED_SCHEMES.contains(Uri.parse(url).getScheme())) {
            return null;
        }

        try {
            Response rsp = request(url);
            return new WebResourceResponse(rsp.header(HEADER_CONTENT_TYPE),
                    rsp.header(HEADER_CONTENT_ENCODING), rsp.body().byteStream());
        } catch (IOException e) {
            L.e(e);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override public WebResourceResponse shouldInterceptRequest(WebView view,
                                                                WebResourceRequest request) {
        if (!SUPPORTED_SCHEMES.contains(request.getUrl().getScheme())) {
            return null;
        }

        try {
            Response rsp = request(request.getUrl().toString());
            return new WebResourceResponse(rsp.header(HEADER_CONTENT_TYPE),
                    rsp.header(HEADER_CONTENT_ENCODING), rsp.code(),
                    StringUtils.defaultIfBlank(rsp.message(), "Unknown error"),
                    toMap(rsp.headers()),
                    rsp.body().byteStream());
        } catch (IOException e) {
            L.e(e);
        }
        return null;
    }

    @NonNull private Response request(String url) throws IOException {
        return OkHttpConnectionFactory.getClient()
                .newCall(new Request.Builder().url(url).build())
                .execute();
    }

    @NonNull private Map<String, String> toMap(@NonNull Headers headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); ++i) {
            map.put(headers.name(i), headers.value(i));
        }
        return map;
    }
}
