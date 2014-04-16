package org.wikipedia.page;

import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.widget.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.editing.*;
import org.wikipedia.events.*;
import org.wikipedia.history.*;
import org.wikipedia.pageimages.*;
import org.wikipedia.savedpages.*;

import java.util.*;

public class PageViewFragment extends Fragment {
    private static final String KEY_TITLE = "title";
    private static final String KEY_PAGE = "page";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCROLL_Y = "scrollY";
    private static final String KEY_CURRENT_HISTORY_ENTRY = "currentHistoryEntry";
    private static final String KEY_QUICK_RETURN_BAR_ID = "quickReturnBarId";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;

    private PageTitle title;
    private ObservableWebView webView;
    private ProgressBar loadProgress;
    private View networkError;
    private View retryButton;
    private SlidingPaneLayout tocSlider;

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
    public PageViewFragment(PageTitle title, HistoryEntry historyEntry, int quickReturnBarId) {
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
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        Utils.crossFade(loadProgress, webView);
    }

    private void populateNonLeadSections() {
        editHandler = new EditHandler(this, bridge, page);
        bridge.sendMessage("startSectionsDisplay", new JSONObject());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_TITLE, title);
        outState.putParcelable(KEY_PAGE, page);
        outState.putInt(KEY_STATE, state);
        outState.putInt(KEY_SCROLL_Y, webView.getScrollY());
        outState.putParcelable(KEY_CURRENT_HISTORY_ENTRY, curEntry);
        outState.putInt(KEY_QUICK_RETURN_BAR_ID, quickReturnBarId);
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
            if (savedInstanceState.containsKey(KEY_PAGE)) {
                page = savedInstanceState.getParcelable(KEY_PAGE);
            }
            state = savedInstanceState.getInt(KEY_STATE);
            scrollY = savedInstanceState.getInt(KEY_SCROLL_Y);
            curEntry = savedInstanceState.getParcelable(KEY_CURRENT_HISTORY_ENTRY);
            quickReturnBarId = savedInstanceState.getInt(KEY_QUICK_RETURN_BAR_ID);
        }
        if (title == null) {
            throw new RuntimeException("No PageTitle passed in to constructor or in instanceState");
        }

        app = (WikipediaApp)getActivity().getApplicationContext();

        webView = (ObservableWebView) getView().findViewById(R.id.page_web_view);
        loadProgress = (ProgressBar) getView().findViewById(R.id.page_load_progress);
        networkError = getView().findViewById(R.id.page_error);
        retryButton = getView().findViewById(R.id.page_error_retry);
        quickReturnBar = getActivity().findViewById(quickReturnBarId);
        tocSlider = (SlidingPaneLayout) getView().findViewById(R.id.page_toc_slider);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Enable Pinch-Zoom
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false);
        }

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();
        Utils.addUtilityMethodsToBridge(getActivity(), bridge);
        Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
        linkHandler = new LinkHandler(getActivity(), bridge, title.getSite());
        api = ((WikipediaApp)getActivity().getApplicationContext()).getAPIForSite(title.getSite());

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.crossFade(networkError, loadProgress);
                performActionForState(state);
            }
        });

        setState(state);
        if (curEntry.getSource() == HistoryEntry.SOURCE_SAVED_PAGE && state < STATE_COMPLETE_FETCH) {
            new SavedPageFetchTask().execute();
        } else {
            performActionForState(state);
        }

        new QuickReturnHandler(webView, quickReturnBar);
    }

    private void setupMessageHandlers() {
        Utils.addUtilityMethodsToBridge(getActivity(), bridge);
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    int index = messagePayload.optInt("index");
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("section", page.getSections().get(index).toJSON());
                    wrapper.put("index", index);
                    wrapper.put("isLast", index == page.getSections().size() - 1);
                    wrapper.put("fragment", page.getTitle().getFragment());
                    bridge.sendMessage("displaySection", wrapper);
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
            Utils.crossFade(webView, loadProgress);
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
                tocHandler = new ToCHandler(tocSlider, quickReturnBar, bridge);
            }
            tocHandler.setupToC(page);
        }
    }

    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask() {
            super(getActivity(), title, "0");
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder =  super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop") + "|lastmodified|normalizedtitle|displaytitle");
            return builder;
        }

        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            Log.d("Wikipedia", result.asObject().toString(4));
            JSONObject mobileView = result.asObject().optJSONObject("mobileview");
            pageProperties = new PageProperties(mobileView);
            if (mobileView.has("redirected")) {
                // Handle redirects properly.
                title = new PageTitle(mobileView.optString("redirected"), title.getSite());
            } else if (mobileView.has("normalizedtitle")) {
                // We care about the normalized title only if we were not redirected
                title = new PageTitle(mobileView.optString("normalizedtitle"), title.getSite());
            }
            return super.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            page = new Page(title, (ArrayList<Section>) result, pageProperties);
            displayLeadSection();
            setState(STATE_INITIAL_FETCH);
            new RestSectionsFetchTask().execute();

            // Add history entry now
            app.getPersister(HistoryEntry.class).persist(curEntry);
            new PageImageSaveTask(app, api, title).execute();
        }

        @Override
        public void onCatch(Throwable caught) {
            if (caught instanceof ApiException) {
                // Should check for the source of the error and have different things turn up
                // But good enough for now
                Utils.crossFade(loadProgress, networkError);
                // Not sure why this is required, but without it tapping retry hides networkError
                // FIXME: INVESTIGATE WHY THIS HAPPENS!
                networkError.setVisibility(View.VISIBLE);
            } else {
                throw new RuntimeException(caught);
            }
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        public RestSectionsFetchTask() {
            super(getActivity(), title, "1-");
        }

        @Override
        public void onFinish(List<Section> result) {
            ArrayList<Section> newSections = (ArrayList<Section>) page.getSections().clone();
            newSections.addAll(result);
            page = new Page(page.getTitle(), newSections, page.getPageProperties());
            populateNonLeadSections();
            setState(STATE_COMPLETE_FETCH);
        }
    }

    private class SavedPageFetchTask extends LoadSavedPageTask {

        public SavedPageFetchTask() {
            super(getActivity(), title);
        }

        @Override
        public void onFinish(Page result) {
            page = result;
            performActionForState(STATE_COMPLETE_FETCH);
            setState(STATE_COMPLETE_FETCH);
        }
    }

    public void savePage() {
        new SavePageTask(getActivity(), bridge, page) {
            @Override
            public void onBeforeExecute() {
                Toast.makeText(getActivity(), R.string.toast_saving_page, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFinish(Void result) {
                Toast.makeText(getActivity(), R.string.toast_saved_page, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private ToCHandler tocHandler;
    public void showToC() {
        tocHandler.show();
    }

    public boolean handleBackPressed() {
        if (tocHandler != null && tocHandler.isVisible()) {
            tocHandler.hide();
            return true;
        }
        return false;
    }
}
