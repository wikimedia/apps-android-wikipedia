package org.wikipedia.page;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.*;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.events.PageStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.pageimages.PageImageSaveTask;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.savedpages.SavePageTask;

import java.util.ArrayList;
import java.util.List;

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

    private Page page;
    private HistoryEntry curEntry;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;

    private WikipediaApp app;
    private Api api;

    private int scrollY;
    private int quickReturnBarId;

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

    private void displayLeadSection(Page page) {
        JSONObject leadSectionPayload = new JSONObject();
        try {
            leadSectionPayload.put("title", page.getTitle().getDisplayText());
            leadSectionPayload.put("leadSectionHTML", page.getSections().get(0).toHTML(true));

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

    private void populateNonLeadSections(Page page) {
        try {
            JSONObject wrapper = new JSONObject();
            JSONArray allSectionsPayload = new JSONArray();
            for (int i=1; i < page.getSections().size(); i++) {
                JSONObject sectionPayload = new JSONObject();
                sectionPayload.putOpt("id", page.getSections().get(i).getId());
                sectionPayload.putOpt("heading", page.getSections().get(i).getHeading());
                sectionPayload.putOpt("content", page.getSections().get(i).toHTML(true));
                allSectionsPayload.put(sectionPayload);
            }
            wrapper.putOpt("sectionHeadings", allSectionsPayload);
            bridge.sendMessage("displaySectionsList", wrapper);
            editHandler = new EditHandler(getActivity(), bridge, page);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
        FrameLayout parentView = (FrameLayout) inflater.inflate(R.layout.fragment_page, container, false);

        webView = (ObservableWebView) parentView.findViewById(R.id.page_web_view);
        loadProgress = (ProgressBar) parentView.findViewById(R.id.page_load_progress);
        networkError = parentView.findViewById(R.id.page_error);
        retryButton = parentView.findViewById(R.id.page_error_retry);

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

        // Enable Pinch-Zoom
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        linkHandler = new LinkHandler(getActivity(), bridge, title.getSite());
        Utils.addUtilityMethodsToBridge(getActivity(), bridge);
        app = (WikipediaApp)getActivity().getApplicationContext();
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

        new QuickReturnHandler(webView, getActivity().findViewById(quickReturnBarId));

        return parentView;
    }

    private void performActionForState(int state) {
        switch (state) {
            case STATE_NO_FETCH:
                new LeadSectionFetchTask().execute();
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask().execute();
                break;
            case STATE_COMPLETE_FETCH:
                displayLeadSection(page);
                // Delay the full section population a little bit
                // To give the webview time to catch up.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        populateNonLeadSections(page);
                        webView.setScrollY(scrollY);
                    }
                }, 500);
                break;
        }
    }

    private void setState(int state) {
        this.state = state;
        app.getBus().post(new PageStateChangeEvent(state));
    }

    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask() {
            super(api, title, "0");
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder =  super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop") + "|lastmodified");
            return builder;
        }

        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            pageProperties = new PageProperties(Utils.parseMWDate(result.asObject().optJSONObject("mobileview").optString("lastmodified")));
            return super.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            page = new Page(title, (ArrayList<Section>) result, pageProperties);
            displayLeadSection(page);
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
            super(api, title, "1-");
        }

        @Override
        public void onFinish(List<Section> result) {
            ArrayList<Section> newSections = (ArrayList<Section>) page.getSections().clone();
            newSections.addAll(result);
            page = new Page(page.getTitle(), newSections, page.getPageProperties());
            populateNonLeadSections(page);
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
}
