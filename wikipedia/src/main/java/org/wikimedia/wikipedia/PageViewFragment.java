package org.wikimedia.wikipedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;

import java.util.ArrayList;
import java.util.List;

public class PageViewFragment extends Fragment {
    private static final String KEY_TITLE = "title";
    private static final String KEY_PAGE = "page";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCROLL_Y = "scrollY";

    private static final int STATE_NO_FETCH = 1;
    private static final int STATE_INITIAL_FETCH = 2;
    private static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;

    private PageTitle title;
    private WebView webView;
    private ProgressBar loadProgress;

    private Page page;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;

    private Api api;

    private int scrollY;

    public PageViewFragment(PageTitle title) {
        this.title = title;
    }

    public PageViewFragment() {
    }

    private void displayLeadSection(Page page) {
        JSONObject leadSectionPayload = new JSONObject();
        try {
            leadSectionPayload.put("title", page.getTitle().getPrefixedText());
            leadSectionPayload.put("leadSectionHTML", page.getSections().get(0).toHTML(true));
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        bridge.sendMessage("displayLeadSection", leadSectionPayload);

        webView.setAlpha(0f);
        webView.setVisibility(View.VISIBLE);
        webView.animate()
                .alpha(1.0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(null);

        loadProgress.animate()
                .alpha(0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadProgress.setVisibility(View.GONE);
                    }
                });
    }

    private void populateAllSections(Page page) {
        try {
            JSONObject wrapper = new JSONObject();
            JSONArray allSectionsPayload = new JSONArray();
            for (int i=1; i < page.getSections().size(); i++) {
                JSONObject sectionPayload = new JSONObject();
                sectionPayload.putOpt("index", i);
                sectionPayload.putOpt("heading", page.getSections().get(i).getHeading());
                sectionPayload.putOpt("content", page.getSections().get(i).toHTML(true));
                allSectionsPayload.put(sectionPayload);
            }
            wrapper.putOpt("sectionHeadings", allSectionsPayload);
            bridge.sendMessage("displaySectionsList", wrapper);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        FrameLayout parentView = (FrameLayout) inflater.inflate(R.layout.fragment_page, container, false);

        webView = (WebView) parentView.findViewById(R.id.pageWebView);
        loadProgress = (ProgressBar) parentView.findViewById(R.id.pageLoadProgress);

        // Enable Pinch-Zoom
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        linkHandler = new LinkHandler(getActivity(), bridge, title.getSite());
        api = ((WikipediaApp)getActivity().getApplicationContext()).getAPIForSite(title.getSite());

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_TITLE)) {
            title = savedInstanceState.getParcelable(KEY_TITLE);
            if (savedInstanceState.containsKey(KEY_PAGE)) {
                page = savedInstanceState.getParcelable(KEY_PAGE);
            }
            state = savedInstanceState.getInt(KEY_STATE);
            scrollY = savedInstanceState.getInt(KEY_SCROLL_Y);
        }
        if (title == null) {
            throw new RuntimeException("No PageTitle passed in to constructor or in instanceState");
        }


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
                        populateAllSections(page);
                        webView.setScrollY(scrollY);
                    }
                }, 500);
                break;
        }
        return parentView;
    }

    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask() {
            super(api, title, "0");
        }

        @Override
        public void onFinish(List<Section> result) {
            page = new Page(title, (ArrayList<Section>) result);
            displayLeadSection(page);
            state = STATE_INITIAL_FETCH;
            new RestSectionsFetchTask().execute();
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        public RestSectionsFetchTask() {
            super(api,  title, "1-");
        }

        @Override
        public void onFinish(List<Section> result) {
            result.remove(0); // Remove when bug 57402 is fixed
            ArrayList<Section> newSections = (ArrayList<Section>) page.getSections().clone();
            newSections.addAll(result);
            page = new Page(page.getTitle(), newSections);
            populateAllSections(page);
            state = STATE_COMPLETE_FETCH;
        }
    }
}
