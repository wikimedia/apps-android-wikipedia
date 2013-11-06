package org.wikimedia.wikipedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;

public class PageViewFragment extends Fragment {
    public static final String KEY_TITLE = "title";
    public static final String KEY_PAGE = "page";

    private PageTitle title;
    private WebView webView;
    private ProgressBar loadProgress;

    private Page page;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;

    public PageViewFragment(PageTitle title) {
        this.title = title;
    }

    public PageViewFragment() {
    }

    private void displayPage(Page page) {
        JSONObject leadSectionPayload = new JSONObject();
        try {
            leadSectionPayload.put("title", page.getTitle().getPrefixedText());
            leadSectionPayload.put("leadSectionHTML", page.getSections().get(0).toHTML());
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_TITLE, title);
        outState.putParcelable(KEY_PAGE, page);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        FrameLayout parentView = (FrameLayout) inflater.inflate(R.layout.fragment_page, container, false);

        webView = (WebView) parentView.findViewById(R.id.pageWebView);
        loadProgress = (ProgressBar) parentView.findViewById(R.id.pageLoadProgress);

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        linkHandler = new LinkHandler(getActivity(), bridge);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_TITLE)) {
            title = savedInstanceState.getParcelable(KEY_TITLE);
            if (savedInstanceState.containsKey(KEY_PAGE)) {
                page = savedInstanceState.getParcelable(KEY_PAGE);
            }
        }
        if (title == null) {
            throw new RuntimeException("No PageTitle passed in to constructor or in instanceState");
        }

        if (page == null) {
            Api api = ((WikipediaApp)getActivity().getApplicationContext()).getPrimarySiteAPI();
            new PageFetchTask(api, title) {
                @Override
                public void onFinish(Page result) {
                    page = result;
                    displayPage(result);
                }
            }.execute();
        } else {
            displayPage(page);
        }
        return parentView;
    }
}
