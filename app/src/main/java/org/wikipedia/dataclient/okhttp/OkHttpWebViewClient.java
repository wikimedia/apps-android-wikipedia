package org.wikipedia.dataclient.okhttp;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
    private static final String CONTENT_TYPE_OGG = "application/ogg";

    @NonNull public abstract WikiSite getWikiSite();

    @SuppressWarnings("deprecation") @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (!SUPPORTED_SCHEMES.contains(Uri.parse(url).getScheme())) {
            return null;
        }

        try {
            Response rsp = request(url);
            // noinspection ConstantConditions
            return new WebResourceResponse(rsp.body().contentType().type() + "/" + rsp.body().contentType().subtype(),
                    rsp.body().contentType().charset(Charset.defaultCharset()).name(),
                    getInputStream(rsp));
        } catch (Exception e) {
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
            // noinspection ConstantConditions
            return new WebResourceResponse(rsp.body().contentType().type() + "/" + rsp.body().contentType().subtype(),
                    rsp.body().contentType().charset(Charset.defaultCharset()).name(),
                    rsp.code(),
                    StringUtils.defaultIfBlank(rsp.message(), "Unknown error"),
                    toMap(rsp.headers()),
                    getInputStream(rsp));
        } catch (Exception e) {
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

        if (CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
            inputStream = new AvailableInputStream(rsp.body().byteStream(),
                    rsp.body().contentLength());
        }

        return inputStream;
    }
}
