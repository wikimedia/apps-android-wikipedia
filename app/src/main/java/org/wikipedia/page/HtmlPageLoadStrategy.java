package org.wikipedia.page;

import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.support.annotation.NonNull;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Our new page load strategy, which loads the page via webView#loadUrl.
 * The CSS is also provided by the service.
 * <p/>
 * The experimental service is available on labs under:
 * https://appservice.wmflabs.org/en.m.wikipedia.org/v1/mobile/app/page/html/:title
 * Or you can switch to your local installation by using something similar to the
 * commented out SERVICE_URI_START value.
 * <p/>
 * There is still plenty of work to be done to make this look and behave nicer:
 * TODO: add more JS (probably in the service)
 * (^ I hope this would also improve CSS, and allow us to bring back night mode and editing)
 * TODO: enwiki hard-coded. Use correct site/domain
 * TODO: bottom content (Read more/next, attributions)
 * TODO: lead image
 * TODO: save pages/load save pages
 * TODO: cache pages/load from cache??? (Maybe we can skip that by just using the web cache)
 * TODO: ... and probably more ...
 */
public class HtmlPageLoadStrategy implements PageLoadStrategy {
    // to hit the service on local deployment; update the hostname for your needs
//    private static final String SERVICE_URI_START = "http://besiair:6927/";
    private static final String SERVICE_URI_START = "https://appservice.wmflabs.org/";
    private static final String SERVICE_URI
            = SERVICE_URI_START + "en.m.wikipedia.org/v1/mobile/app/page/html/";
    private static final String WIKI = SERVICE_URI_START + "wiki/";

    // Note: several commented out variables/statements are carried over from the JsonPageLoadStrategy.

    /**
     * Whether to write the page contents to cache as soon as it's loaded.
     */
//    private boolean cacheOnComplete = true;

//    private int sectionTargetFromIntent;

    // copied fields
    private PageViewModel model;
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private PageActivity activity;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
//    private WikipediaApp app;
//    private LeadImagesHandler leadImagesHandler;
//    private SearchBarHideHandler searchBarHideHandler;

//    private BottomContentInterface bottomContentHandler;

    private boolean isLoading;

    @Override
    public void setup(PageViewModel model, PageFragment fragment,
                      SwipeRefreshLayoutWithScroll refreshView, ObservableWebView webView,
                      CommunicationBridge bridge, SearchBarHideHandler searchBarHideHandler,
                      LeadImagesHandler leadImagesHandler) {
        this.model = model;
        this.fragment = fragment;
        activity = (PageActivity) fragment.getActivity();
//        this.app = (WikipediaApp) activity.getApplicationContext();
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
//        this.searchBarHideHandler = searchBarHideHandler;
//        this.leadImagesHandler = leadImagesHandler;
    }

    @Override
    public void onActivityCreated(@NonNull List<PageBackStackItem> backStack) {
        setupSpecificMessageHandlers();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(WIKI)) {
                    loadNewPage(getServiceUrlFor(extractTitleStringFrom(url)));
                    return false;
                } else if (url.startsWith(SERVICE_URI_START)) {
                    return false;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            /**
             * Notify the host application of a resource request and allow the
             * application to return the data.  If the return value is null, the WebView
             * will continue to load the resource as usual.  Otherwise, the return
             * response and data will be used.  NOTE: This method is called on a thread
             * other than the UI thread so clients should exercise caution
             * when accessing private data or the view system.
             *
             * @param view    The {@link android.webkit.WebView} that is requesting the
             *                resource.
             * @param request Object containing the details of the request.
             * @return A {@link android.webkit.WebResourceResponse} containing the
             * response information or null if the WebView should load the
             * resource itself.
             */
            // see http://stackoverflow.com/questions/8273991/webview-shouldinterceptrequest-example
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                isLoading = true;
                activity.updateProgressBar(true, true, 0);
                activity.supportInvalidateOptionsMenu();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                isLoading = false;
                activity.updateProgressBar(false, false, 0);
                activity.supportInvalidateOptionsMenu();
                refreshView.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler,
                                           SslError error) {
                super.onReceivedSslError(view, handler, error);
            }
        });
    }

    private void setupSpecificMessageHandlers() {
        bridge.addListener("onGetAppMeta1", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
//                    stagedScrollY = messagePayload.getInt("stagedScrollY");
//                    loadPageOnWebViewReady(messagePayload.getBoolean("tryFromCache"));

                    PageProperties pageProperties = new PageProperties(messagePayload);
                    PageTitle title = model.getTitle();
                    model.setTitle(title);
                    model.setPage(new Page(title, extractToCListFromJSONArray(messagePayload.getJSONArray("toc")), pageProperties));
                    fragment.setupToC(model, !webView.canGoBack());
                } catch (JSONException e) {
                    //nope
                }
            }
        });
    }

    private ArrayList<Section> extractToCListFromJSONArray(JSONArray jArray) throws JSONException {
        ArrayList<Section> sections = new ArrayList<>();
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                sections.add(Section.fromJson(jArray.getJSONObject(i)));
            }
        }
        return sections;
    }

    @Override
    public void backFromEditing(Intent data) {
        // Retrieve section ID from intent, and find correct section, so we know where to scroll to
//        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);

        // TODO: implement onLoad? or can we add that to the URL?
    }

    private String getServiceUrlFor(PageTitle title) {
        return getServiceUrlFor(title.getPrefixedText()); // TODO: handle different domains
    }

    private String getServiceUrlFor(String titleString) {
        return SERVICE_URI + titleString;
    }

    private String extractTitleStringFrom(String url) {
        // The title is in the last part of the URL
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private void loadNewPage(String url) {
        model.setTitle(new PageTitle(extractTitleStringFrom(url), model.getTitle().getSite()));
        webView.loadUrl(getServiceUrlFor(model.getTitle()));
    }

    @Override
    public void onDisplayNewPage(boolean pushBackStack, boolean tryFromCache, int stagedScrollY) {
        webView.loadUrl(getServiceUrlFor(model.getTitle()));
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public void onHidePageContent() {
        // nothing to do here. The bottom content should come from the service eventually.
    }

    @Override
    public boolean onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setEditHandler(EditHandler editHandler) {
        // TODO: use editHandler in this class
    }

    @Override
    public void setBackStack(@NonNull List<PageBackStackItem> backStack) {
        // TODO: implement switching of backstacks from multiple tabs.
    }

    @Override
    public void updateCurrentBackStackItem() {
    }

    @Override
    public void loadPageFromBackStack() {
    }
}
