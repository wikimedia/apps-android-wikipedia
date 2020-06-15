package org.wikipedia.dataclient.okhttp;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.PageViewModel;
import org.wikipedia.util.UriUtil;
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

import static org.wikipedia.dataclient.RestService.PAGE_HTML_PREVIEW_ENDPOINT;

public abstract class OkHttpWebViewClient extends WebViewClient {

    /*
    Note: Any data transformations performed here are only for the benefit of WebViews.
    They should not be made into general Interceptors.
    */

    private static final List<String> SUPPORTED_SCHEMES = Arrays.asList("http", "https");
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String CONTENT_TYPE_OGG = "application/ogg";
    private static final String PCS_CSS = "/data/css/mobile/pcs";
    private static final String BASE_CSS = "/data/css/mobile/base";
    private static final String PCS_JS = "/data/javascript/mobile/pcs";

    @NonNull public abstract PageViewModel getModel();
    @NonNull public abstract LinkHandler getLinkHandler();

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (getModel().shouldLoadAsMobileWeb()) {
            // If the page was loaded as Mobile Web, then pass all link clicks through
            // to our own link handler.
            getLinkHandler().onUrlClick(UriUtil.decodeURL(url), null, "");
            return true;
        }
        return false;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Override public WebResourceResponse shouldInterceptRequest(WebView view,
                                                                WebResourceRequest request) {
        if (!SUPPORTED_SCHEMES.contains(request.getUrl().getScheme())) {
            return null;
        }

        if (request.getUrl().toString().contains(PAGE_HTML_PREVIEW_ENDPOINT)) {
            return null;
        }

        WebResourceResponse response;
        try {
            Response rsp = request(request);
            if (CONTENT_TYPE_OGG.equals(rsp.header(HEADER_CONTENT_TYPE))) {
                rsp.close();
                return super.shouldInterceptRequest(view, request);
            } else {
                // noinspection ConstantConditions
                response =  new WebResourceResponse(rsp.body().contentType().type() + "/" + rsp.body().contentType().subtype(),
                            rsp.body().contentType().charset(Charset.defaultCharset()).name(),
                            rsp.code(),
                            StringUtils.defaultIfBlank(rsp.message(), "Unknown error"),
                            toMap(addResponseHeaders(rsp.headers())),
                            getInputStream(rsp));
            }
        } catch (Exception e) {

            //------------------
            // TODO: remove after two releases.
            if (request.getUrl().toString().contains(PCS_CSS)) {
                final int statusCode = 200;
                try {
                    return new WebResourceResponse("text/css", "utf-8", statusCode, "OK",
                            Collections.emptyMap(), WikipediaApp.getInstance().getAssets().open("offline_convert/pcs.css"));
                } catch (IOException ex) {
                    // ignore silently
                }
            } else if (request.getUrl().toString().contains(BASE_CSS)) {
                final int statusCode = 200;
                try {
                    return new WebResourceResponse("text/css", "utf-8", statusCode, "OK",
                            Collections.emptyMap(), WikipediaApp.getInstance().getAssets().open("offline_convert/base.css"));
                } catch (IOException ex) {
                    // ignore silently
                }
            } else if (request.getUrl().toString().contains(PCS_JS)) {
                final int statusCode = 200;
                try {
                    return new WebResourceResponse("text/javascript", "utf-8", statusCode, "OK",
                            Collections.emptyMap(), WikipediaApp.getInstance().getAssets().open("offline_convert/pcs.js"));
                } catch (IOException ex) {
                    // ignore silently
                }
            }
            //------------------

            String reasonCode = TextUtils.isEmpty(e.getMessage()) ? "Unknown error" : UriUtil.encodeURL(e.getMessage());
            if (e instanceof HttpStatusException) {
                response = new WebResourceResponse(null, null, ((HttpStatusException) e).code(), reasonCode, null, null);
            } else {
                response = new WebResourceResponse(null, null, 500, reasonCode, null, null);
            }
            L.e(e);
        }
        return response;
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return ((event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_F)
                || (!event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_F3));
    }

    @NonNull private Response request(WebResourceRequest request) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(request.getUrl().toString())
                .cacheControl(getModel().getCacheControl());
        for (String header : request.getRequestHeaders().keySet()) {
            if (header.equals("If-None-Match") || header.equals("If-Modified-Since")) {
                // Strip away conditional headers from the request coming from the WebView, since
                // we want control of caching for ourselves (it can break OkHttp's caching internals).
                continue;
            }
            builder.header(header, request.getRequestHeaders().get(header));
        }
        return OkHttpConnectionFactory.getClient().newCall(addHeaders(builder).build()).execute();
    }

    private Request.Builder addHeaders(@NonNull Request.Builder builder) {
        // TODO: Find a common way to set this header between here and RetrofitFactory.
        builder.header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(getModel().getTitle().getWikiSite()));
        if (getModel().shouldSaveOffline()) {
            builder.header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE);
        }
        builder.header(OfflineCacheInterceptor.LANG_HEADER, getModel().getTitle().getWikiSite().languageCode());
        builder.header(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(getModel().getTitle().getPrefixedText()));
        if (getModel().getCurEntry() != null && !TextUtils.isEmpty(getModel().getCurEntry().getReferrer())) {
            builder.header("Referer", getModel().getCurEntry().getReferrer());
        }
        return builder;
    }

    private Headers addResponseHeaders(@NonNull Headers headers) {
        // add CORS header to allow requests from all domains.
        return headers.newBuilder().set("Access-Control-Allow-Origin", "*").build();
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
