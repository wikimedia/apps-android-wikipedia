package org.wikipedia.dataclient.okhttp;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageViewModel;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String PCS_CSS_BASE = "/data/css/mobile/base";
    private static final String PCS_CSS_SITE = "/data/css/mobile/site";

    @NonNull public abstract PageViewModel getModel();

    @SuppressWarnings("deprecation") @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (!SUPPORTED_SCHEMES.contains(Uri.parse(url).getScheme())) {
            return null;
        }

        try {
            Response rsp = request(url);
            if (CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
                rsp.close();
                return super.shouldInterceptRequest(view, url);
            } else {
                // noinspection ConstantConditions
                return new WebResourceResponse(rsp.body().contentType().type() + "/" + rsp.body().contentType().subtype(),
                        rsp.body().contentType().charset(Charset.defaultCharset()).name(),
                        rsp.body().byteStream());
            }
        } catch (Exception e) {

            if (url.contains(PCS_CSS_BASE)) {
                // This means that we failed to fetch the base CSS for our page (probably due to
                // being offline), so replace it with our pre-packaged fallback.
                try {
                    return new WebResourceResponse("text/css", "utf-8",
                            WikipediaApp.getInstance().getAssets().open("styles.css"));
                } catch (IOException ex) {
                    // ignore silently
                }
            }

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
            Response rsp = request(request);
            if (CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
                rsp.close();
                return super.shouldInterceptRequest(view, request);
            } else {
                // noinspection ConstantConditions
                return new WebResourceResponse(rsp.body().contentType().type() + "/" + rsp.body().contentType().subtype(),
                        rsp.body().contentType().charset(Charset.defaultCharset()).name(),
                        rsp.code(),
                        StringUtils.defaultIfBlank(rsp.message(), "Unknown error"),
                        toMap(rsp.headers()),
                        getInputStream(rsp));
            }
        } catch (Exception e) {

            if (request.getUrl().toString().contains(PCS_CSS_BASE)) {
                // This means that we failed to fetch the base CSS for our page (probably due to
                // being offline), so replace it with our pre-packaged fallback.
                final int statusCode = 200;
                try {
                    return new WebResourceResponse("text/css", "utf-8", statusCode, "OK",
                            Collections.emptyMap(),
                            WikipediaApp.getInstance().getAssets().open("styles.css"));
                } catch (IOException ex) {
                    // ignore silently
                }
            }

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
        Request.Builder builder = new Request.Builder()
                .url(url)
                .cacheControl(getModel().getCacheControl());
        return OkHttpConnectionFactory.getClient().newCall(addHeaders(builder).build()).execute();
    }

    @TargetApi(21)
    @NonNull private Response request(WebResourceRequest request) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(request.getUrl().toString())
                .cacheControl(getModel().getCacheControl());
        for (String header : request.getRequestHeaders().keySet()) {
            builder.header(header, request.getRequestHeaders().get(header));
        }
        return OkHttpConnectionFactory.getClient().newCall(addHeaders(builder).build()).execute();
    }

    private Request.Builder addHeaders(@NonNull Request.Builder builder) {
        // TODO: Find a common way to set this header between here and RetrofitFactory.
        builder.header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(getModel().getTitle().getWikiSite()));
        builder.header(OfflineCacheInterceptor.SAVE_HEADER, getModel().shouldSaveOffline()
                ? OfflineCacheInterceptor.SAVE_HEADER_SAVE : OfflineCacheInterceptor.SAVE_HEADER_NONE);
        if (getModel().getCurEntry() != null && !TextUtils.isEmpty(getModel().getCurEntry().getReferrer())) {
            builder.header("Referer", getModel().getCurEntry().getReferrer());
        }
        return builder;
    }

    @NonNull private Map<String, String> toMap(@NonNull Headers headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); ++i) {
            map.put(headers.name(i), headers.value(i));
        }
        return map;
    }

    @NonNull private InputStream getInputStream(@NonNull Response rsp) {
        InputStream inputStream = rsp.body().byteStream();

        if (CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
            inputStream = new AvailableInputStream(rsp.body().byteStream(),
                    rsp.body().contentLength());
        }

        return inputStream;
    }
}
