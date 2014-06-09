package org.wikipedia.page;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ObservableWebView;
import org.wikipedia.PageTitle;
import org.wikipedia.QuickReturnHandler;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.savedpages.SavePageTask;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleLoader;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikipedia.events.PageStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.pageimages.PageImageSaveTask;
import org.wikipedia.search.SearchArticlesFragment;
import org.wikipedia.views.DisableableDrawerLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PageViewFragment extends Fragment {
    private static final String KEY_TITLE = "title";
    private static final String KEY_PAGE = "page";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCROLL_Y = "scrollY";
    private static final String KEY_CURRENT_HISTORY_ENTRY = "currentHistoryEntry";
    private static final String KEY_QUICK_RETURN_BAR_ID = "quickReturnBarId";
    private static final String KEY_PAGER_INDEX = "pagerIndex";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;

    /**
     * Indicates that the full state of this fragment will be saved when onSaveInstanceState
     * is called, including the heavy Page object that contains the full page text. This is good
     * when the fragment is destroyed due to the activity being closed.
     */
    public static final int SAVE_STATE_FULL = 0;

    /**
     * Indicates that only a partial state of this fragment will be saved when onSaveInstanceState
     * is called, including the Title, HistoryItem, and scroll position. This is used when the
     * fragment is destroyed by the ViewPager when the user slides it out of sight. The next time
     * the fragment is recreated, we'll fetch the page contents from the network again.
     */
    public static final int SAVE_STATE_TITLE = 1;

    /**
     * Indicates that none of this fragment's state will be saved when onSaveInstanceState
     * is called. This is used when the fragment is destroyed due to the user going "back"
     * in the ViewPager.
     */
    public static final int SAVE_STATE_NONE = 2;

    /**
     * Determines how much of this fragment's state will be saved when it's destroyed.
     * (when onSaveInstanceState is called)
     */
    private int saveState = SAVE_STATE_FULL;

    /**
     * Stores this fragment's position in the ViewPager in the parent activity.
     */
    private int pagerIndex;

    private SearchArticlesFragment searchArticlesFragment;

    private PageTitle title;
    private ObservableWebView webView;
    private ProgressBar loadProgress;
    private View networkError;
    private View retryButton;
    private View pageDoesNotExistError;
    private DisableableDrawerLayout tocDrawer;
    private View pageFragmentContainer;

    private Page page;
    private HistoryEntry curEntry;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;

    private WikipediaApp app;
    private Api api;

    private int scrollY;
    private int quickReturnBarId;

    private View quickReturnBar;

    // Pass in the id rather than the View object itself for the quickReturn bar, to help it survive rotates
    public PageViewFragment(int pagerIndex, PageTitle title, HistoryEntry historyEntry, int quickReturnBarId) {
        this.pagerIndex = pagerIndex;
        this.title = title;
        this.curEntry = historyEntry;
        this.quickReturnBarId = quickReturnBarId;
    }

    public PageViewFragment() {
    }

    public PageTitle getTitle() {
        return title;
    }

    public Page getPage() {
        return page;
    }

    public HistoryEntry getHistoryEntry() {
        return curEntry;
    }

    public int getScrollY() {
        if (webView != null) {
            scrollY = webView.getScrollY();
        }
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
        if (webView != null) {
            webView.scrollTo(0, scrollY);
        }
    }

    public void setSaveState(int saveState) {
        this.saveState = saveState;
    }

    public int getPagerIndex() {
        return pagerIndex;
    }

    /*
    Hide the entire fragment. This is necessary when displaying a new page fragment on top
    of a previous one -- some devices have issues with rendering "heavy" components
    (like WebView) when overlaid on top of many other Views.
     */
    public void hide() {
        if (pageFragmentContainer == null) {
            return;
        }
        pageFragmentContainer.setVisibility(View.GONE);
    }

    /*
    Make this fragment visible. Make sure to call this when going "back" through the
    stack of fragments
     */
    public void show() {
        if (pageFragmentContainer == null) {
            return;
        }
        pageFragmentContainer.setVisibility(View.VISIBLE);
        //refresh the fragment's state (ensures correct state of overflow menu)
        setState(state);
    }

    private void displayLeadSection() {
        JSONObject leadSectionPayload = new JSONObject();
        try {
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());

            bridge.sendMessage("displayLeadSection", leadSectionPayload);

            JSONObject attributionPayload = new JSONObject();
            String lastUpdatedText = getString(R.string.last_updated_text, Utils.formatDateRelative(page.getPageProperties().getLastModified()));
            attributionPayload.put("historyText", lastUpdatedText);
            attributionPayload.put("historyTarget", page.getTitle().getUriForAction("history"));
            attributionPayload.put("licenseHTML", getString(R.string.content_license_html));
            bridge.sendMessage("displayAttribution", attributionPayload);

            if (!page.getPageProperties().canEdit()) {
                bridge.sendMessage("setPageProtected", new JSONObject());
            }

            if (page.getPageProperties().isMainPage()) {
                bridge.sendMessage("setMainPage", new JSONObject());
            }
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        ViewAnimations.crossFade(loadProgress, webView);
    }

    private void populateNonLeadSections() {
        bridge.sendMessage("startSectionsDisplay", new JSONObject());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (saveState != SAVE_STATE_NONE) {
            outState.putInt(KEY_PAGER_INDEX, pagerIndex);
            outState.putParcelable(KEY_TITLE, title);
            outState.putInt(KEY_SCROLL_Y, webView.getScrollY());
            outState.putParcelable(KEY_CURRENT_HISTORY_ENTRY, curEntry);
            outState.putInt(KEY_QUICK_RETURN_BAR_ID, quickReturnBarId);
            if (saveState == SAVE_STATE_FULL) {
                outState.putParcelable(KEY_PAGE, page);
                outState.putInt(KEY_STATE, state);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_TITLE)) {
            title = savedInstanceState.getParcelable(KEY_TITLE);
            curEntry = savedInstanceState.getParcelable(KEY_CURRENT_HISTORY_ENTRY);
            scrollY = savedInstanceState.getInt(KEY_SCROLL_Y);
            quickReturnBarId = savedInstanceState.getInt(KEY_QUICK_RETURN_BAR_ID);
            pagerIndex = savedInstanceState.getInt(KEY_PAGER_INDEX);
            if (savedInstanceState.containsKey(KEY_PAGE)) {
                page = savedInstanceState.getParcelable(KEY_PAGE);
                state = savedInstanceState.getInt(KEY_STATE);
            }
        }
        if (title == null) {
            throw new RuntimeException("No PageTitle passed in to constructor or in instanceState");
        }

        app = (WikipediaApp)getActivity().getApplicationContext();

        searchArticlesFragment = (SearchArticlesFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        pageFragmentContainer = getView().findViewById(R.id.page_fragment_container);
        webView = (ObservableWebView) getView().findViewById(R.id.page_web_view);
        loadProgress = (ProgressBar) getView().findViewById(R.id.page_load_progress);
        networkError = getView().findViewById(R.id.page_error);
        retryButton = getView().findViewById(R.id.page_error_retry);
        pageDoesNotExistError = getView().findViewById(R.id.page_does_not_exist);
        quickReturnBar = getActivity().findViewById(quickReturnBarId);
        tocDrawer = (DisableableDrawerLayout) getView().findViewById(R.id.page_toc_drawer);
        // disable TOC drawer until the page is loaded
        tocDrawer.setSlidingEnabled(false);
        searchArticlesFragment.setTocEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Enable Pinch-Zoom
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false);
        }

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();
        Utils.addUtilityMethodsToBridge(getActivity(), bridge);
        Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
        bridge.injectStyleBundle(app.getStyleLoader().getAvailableBundle(StyleLoader.BUNDLE_PAGEVIEW, title.getSite()));
        linkHandler = new LinkHandler(getActivity(), bridge, title.getSite()){
            @Override
            public void onInternalLinkClicked(PageTitle title) {
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
                app.getBus().post(new NewWikiPageNavigationEvent(title, historyEntry));
            }
        };
        api = ((WikipediaApp)getActivity().getApplicationContext()).getAPIForSite(title.getSite());

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewAnimations.crossFade(networkError, loadProgress);
                performActionForState(state);
            }
        });

        editHandler = new EditHandler(this, bridge);

        if (Utils.isLowResolutionDevice(app)) {
            new QuickReturnHandler(webView, quickReturnBar);
        }

        setState(state);
        performActionForState(state);
    }

    private void setupMessageHandlers() {
        Utils.addUtilityMethodsToBridge(getActivity(), bridge);
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    int index = messagePayload.optInt("index");
                    if (index >= page.getSections().size()) {
                        // Page has only one section yo
                        bridge.sendMessage("noMoreSections", new JSONObject());
                    } else {
                        JSONObject wrapper = new JSONObject();
                        wrapper.put("section", page.getSections().get(index).toJSON());
                        wrapper.put("index", index);
                        wrapper.put("isLast", index == page.getSections().size() - 1);
                        wrapper.put("fragment", page.getTitle().getFragment());
                        bridge.sendMessage("displaySection", wrapper);
                    }
                } catch (JSONException e) {
                    // Won't happen
                    throw new RuntimeException(e);
                }
            }
        });
        if (app.getRemoteConfig().getConfig().has("disableAnonEditing")
                && app.getRemoteConfig().getConfig().optBoolean("disableAnonEditing")
                && !app.getUserInfoStorage().isLoggedIn()) {
            bridge.sendMessage("hideEditButtons", new JSONObject());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            ViewAnimations.crossFade(webView, loadProgress);
            setState(STATE_NO_FETCH);
            performActionForState(state);
        }
    }

    private void performActionForState(int forState) {
        switch (forState) {
            case STATE_NO_FETCH:
                new LeadSectionFetchTask().execute();
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask().execute();
                break;
            case STATE_COMPLETE_FETCH:
                editHandler.setPage(page);
                displayLeadSection();
                populateNonLeadSections();
                webView.scrollTo(0, scrollY);
                break;
            default:
                // This should never happen
                throw new RuntimeException("Unknown state encountered " + state);
        }
    }

    private void setState(int state) {
        this.state = state;
        app.getBus().post(new PageStateChangeEvent(state));
        // FIXME: Move this out into a PageComplete event of sorts
        if (state == STATE_COMPLETE_FETCH) {
            if (tocHandler == null) {
                tocHandler = new ToCHandler(tocDrawer,
                        Utils.isLowResolutionDevice(app) ? quickReturnBar : null,
                        bridge);
            }
            tocHandler.setupToC(page);
            searchArticlesFragment.setTocEnabled(true);

            //if the article has only one section, then hide the ToC button
            searchArticlesFragment.setTocHidden(page.getSections().size() <= 1);
        }
    }

    /**
     * Saving a history item needs to be in its own task, since the operation may
     * actually block for several seconds, and should not be on the main thread.
     */
    private class HistorySaveTask extends SaneAsyncTask<Void> {
        private final HistoryEntry entry;
        public HistorySaveTask(HistoryEntry entry) {
            super(SINGLE_THREAD);
            this.entry = entry;
        }

        @Override
        public Void performTask() throws Throwable {
            app.getPersister(HistoryEntry.class).persist(entry);
            return null;
        }

        @Override
        public void onCatch(Throwable caught) {
            Log.d("HistorySaveTask", caught.getMessage());
        }
    }

    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask() {
            super(getActivity(), title, "0");
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder =  super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop") + "|lastmodified|normalizedtitle|displaytitle|protection|editable");
            builder.param("appInstallID", app.getAppInstallReadActionID());
            return builder;
        }

        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            JSONObject mobileView = result.asObject().optJSONObject("mobileview");
            if (mobileView != null) {
                pageProperties = PageProperties.parseJSON(mobileView);
                if (mobileView.has("redirected")) {
                    // Handle redirects properly.
                    title = new PageTitle(mobileView.optString("redirected"), title.getSite());
                } else if (mobileView.has("normalizedtitle")) {
                    // We care about the normalized title only if we were not redirected
                    title = new PageTitle(mobileView.optString("normalizedtitle"), title.getSite());
                }
            }
            return super.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            // have we been unwittingly detached from our Activity?
            if (!isAdded()) {
                Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                return;
            }
            page = new Page(title, (ArrayList<Section>) result, pageProperties);
            editHandler.setPage(page);
            displayLeadSection();
            setState(STATE_INITIAL_FETCH);
            new RestSectionsFetchTask().execute();

            // Add history entry now
            new HistorySaveTask(curEntry).execute();

            // Save image for this page title
            new PageImageSaveTask(app, app.getAPIForSite(title.getSite()), title).execute();
        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught);
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        public RestSectionsFetchTask() {
            super(getActivity(), title, "1-");
        }

        @Override
        public void onFinish(List<Section> result) {
            // have we been unwittingly detached from our Activity?
            if (!isAdded()) {
                Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                return;
            }
            ArrayList<Section> newSections = (ArrayList<Section>) page.getSections().clone();
            newSections.addAll(result);
            page = new Page(page.getTitle(), newSections, page.getPageProperties());
            editHandler.setPage(page);
            populateNonLeadSections();
            setState(STATE_COMPLETE_FETCH);
        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught);
        }
    }

    private void commonSectionFetchOnCatch(Throwable caught) {
        // in any case, make sure the TOC drawer is closed and disabled
        tocDrawer.setSlidingEnabled(false);
        searchArticlesFragment.setTocEnabled(false);

        if (caught instanceof SectionsFetchException) {
            if (((SectionsFetchException)caught).getCode().equals("missingtitle")
                    || ((SectionsFetchException)caught).getCode().equals("invalidtitle")) {
                ViewAnimations.crossFade(loadProgress, pageDoesNotExistError);
            }
        } else if (caught instanceof ApiException) {
            // Check for the source of the error and have different things turn up
            ViewAnimations.crossFade(loadProgress, networkError);
            // Not sure why this is required, but without it tapping retry hides networkError
            // FIXME: INVESTIGATE WHY THIS HAPPENS!
            networkError.setVisibility(View.VISIBLE);
        } else {
            throw new RuntimeException(caught);
        }
    }

    public void savePage() {
        new SavePageTask(getActivity(), title) {
            @Override
            public void onFinish(Void result) {
                Toast.makeText(getActivity(), R.string.toast_saved_page, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private ToCHandler tocHandler;
    public void toggleToC() {
        // tocHandler could still be null while the page is loading
        if (tocHandler == null) {
            return;
        }
        if (tocHandler.isVisible()) {
            tocHandler.hide();
        } else {
            tocHandler.show();
        }
    }

    public boolean handleBackPressed() {
        if (tocHandler != null && tocHandler.isVisible()) {
            tocHandler.hide();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PageViewFragment", "Fragment destroyed.");
    }
}
