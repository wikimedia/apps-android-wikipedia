package org.wikipedia.dataclient.okhttp;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.log.L;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public abstract class OkHttpWebViewClient extends WebViewClient {

    /*
    Note: Any data transformations performed here are only for the benefit of WebViews.
    They should not be made into general Interceptors.
    */

    private static final List<String> SUPPORTED_SCHEMES = Arrays.asList("http", "https");
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_ENCODING = "content-encoding";
    private static final String CONTENT_TYPE_SVG = "image/svg+xml";
    private static final String CONTENT_TYPE_OGG = "application/ogg";

    @NonNull public abstract WikiSite getWikiSite();

    @SuppressWarnings("deprecation") @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (!SUPPORTED_SCHEMES.contains(Uri.parse(url).getScheme())) {
            return null;
        }

        try {
            Response rsp = request(url);
            return new WebResourceResponse(rsp.header(HEADER_CONTENT_TYPE),
                    rsp.header(HEADER_CONTENT_ENCODING), getInputStream(rsp));
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
                    getInputStream(rsp));
        } catch (IOException e) {
            L.e(e);
        }
        return null;
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return ((event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_F)
                || (!event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_F3));
    }

    @NonNull private Response request(String url) throws IOException {
        return OkHttpConnectionFactory.getClient().newCall(new Request.Builder()
                .url(url)
                // TODO: Find a common way to set this header between here and RetrofitFactory.
                .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(getWikiSite()))
                .build())
                .execute();
    }

    @NonNull private Map<String, String> toMap(@NonNull Headers headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); ++i) {
            map.put(headers.name(i), headers.value(i));
        }
        return map;
    }

    @NonNull private InputStream getInputStream(@NonNull Response rsp) throws IOException {
        InputStream inputStream = rsp.body().byteStream();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
            inputStream = new AvailableInputStream(rsp.body().byteStream(),
                    rsp.body().contentLength());
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                && CONTENT_TYPE_SVG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
            return transformSvgFile(inputStream);
        }

        return inputStream;
    }

    /*
    T107775
    In API 18 and below, the system WebView does not perform correct rendering of SVG
    files that specify dimensions with "ex" units. This code rewrites usages of "ex"
    units with "em" before the SVG arrives at the WebView.
    */
    @VisibleForTesting
    @NonNull
    static InputStream transformSvgFile(@NonNull InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("<svg")) {
                line = line.replace("ex\"", "em\"").replace("ex;", "em;");
            }
            sb.append(line);
        }
        inputStream.close();
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}
