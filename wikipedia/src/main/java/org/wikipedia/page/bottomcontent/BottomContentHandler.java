package org.wikipedia.page.bottomcontent;

import android.graphics.Point;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SuggestedPagesFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageViewFragment;
import org.wikipedia.page.SuggestionsTask;
import org.wikipedia.search.FullSearchArticlesTask;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.wikidata.WikidataCache;
import org.wikipedia.wikidata.WikidataDescriptionFeeder;

import java.util.List;
import java.util.Map;

public class BottomContentHandler implements ObservableWebView.OnScrollChangeListener {
    private final PageViewFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;
    private final PageTitle pageTitle;
    private final PageActivity activity;
    private final WikipediaApp app;

    private int displayHeight;
    private float displayDensity;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;

    public BottomContentHandler(final PageViewFragment parentFragment, CommunicationBridge bridge,
                                ObservableWebView webview, LinkHandler linkHandler, ViewGroup hidingView,
                                PageTitle pageTitle, boolean isMainPage) {
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webview;
        this.linkHandler = linkHandler;
        this.pageTitle = pageTitle;
        activity = parentFragment.getFragment().getActivity();
        app = (WikipediaApp) activity.getApplicationContext();
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;

        bottomContentContainer = hidingView;
        webview.addOnScrollChangeListener(this);

        pageLastUpdatedText = (TextView)bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = (TextView)bottomContentContainer.findViewById(R.id.page_license_text);

        // preload the display density, since it will be used in a lot of places
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;

        // get the screen height, using correct methods for different API versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            parentFragment.getActivity().getWindowManager().getDefaultDisplay().getSize(size);
            displayHeight = (int)(size.y / displayDensity);
        } else {
            displayHeight = (int)(parentFragment.getActivity()
                    .getWindowManager().getDefaultDisplay().getHeight() / displayDensity);
        }

        if (isMainPage) {
            bottomContentContainer.findViewById(R.id.read_more_container).setVisibility(View.GONE);
        } else {
            requestReadMoreItems(activity.getLayoutInflater());
        }

        setupAttribution();

        // give it a chance to redraw, and then see if it fits
        bottomContentContainer.post(new Runnable() {
            @Override
            public void run() {
                if (!parentFragment.isAdded()) {
                    return;
                }
                layoutContent();
            }
        });
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        int contentHeight = (int)(webView.getContentHeight() * displayDensity);
        int screenHeight = (int)(displayHeight * displayDensity);
        final int bottomOffsetExtra = 25;
        int bottomOffset = contentHeight - scrollY - screenHeight
                + (int)(bottomOffsetExtra * displayDensity);
        int bottomHeight = bottomContentContainer.getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bottomContentContainer.getLayoutParams();
        if (bottomOffset > bottomHeight) {
            if (params.bottomMargin != -bottomHeight) {
                params.bottomMargin = -bottomHeight;
                bottomContentContainer.setLayoutParams(params);
            }
        } else {
            params.bottomMargin = -bottomOffset;
            bottomContentContainer.setLayoutParams(params);
        }
    }

    private void layoutContent() {
        // pad the bottom of the webview, to make room for ourselves
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(bottomContentContainer.getHeight() / displayDensity));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);

        // and make it visible!
        bottomContentContainer.setVisibility(View.VISIBLE);
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY());
    }

    private void setupAttribution() {
        Page page = parentFragment.getFragment().getPage();
        String lastUpdatedHtml = "<a href=\"" + page.getTitle().getUriForAction("history")
                + "\">" + parentFragment.getString(R.string.last_updated_text,
                Utils.formatDateRelative(page.getPageProperties().getLastModified())
                + "</a>");
        pageLastUpdatedText.setText(Html.fromHtml(lastUpdatedHtml));
        pageLastUpdatedText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
        pageLicenseText.setText(Html.fromHtml(parentFragment.getString(R.string.content_license_html)));
        pageLicenseText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
    }

    private void requestReadMoreItems(final LayoutInflater layoutInflater) {
        new SuggestionsTask(app.getAPIForSite(pageTitle.getSite()), pageTitle.getSite(),
                pageTitle.getPrefixedText()) {
            @Override
            public void onFinish(FullSearchResults results) {
                setupReadMoreSection(bottomContentContainer, layoutInflater, results);
            }

            @Override
            public void onCatch(Throwable caught) {
                super.onCatch(caught);
            }
        }.execute();
    }

    private void setupReadMoreSection(View parentView, LayoutInflater layoutInflater,
                                      final FullSearchArticlesTask.FullSearchResults results) {
        final SuggestedPagesFunnel funnel = new SuggestedPagesFunnel(app, pageTitle.getSite());
        final ReadMoreAdapter adapter = new ReadMoreAdapter(layoutInflater, results.getResults());
        ListView list = (ListView) parentView.findViewById(R.id.read_more_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) adapter.getItem(position);
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
                // always add the description of the item to the cache so we don't even try to get it again
                app.getWikidataCache().put(title.toString(), title.getDescription());
                activity.displayNewPage(title, historyEntry);
                funnel.logSuggestionClicked(pageTitle, results.getPageTitles(), position);
            }
        });

        WikidataDescriptionFeeder.retrieveWikidataDescriptions(results.getResults(), app,
                new WikidataCache.OnWikidataReceiveListener() {
                    @Override
                    public void onWikidataReceived(Map<PageTitle, String> result) {
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onWikidataFailed(Throwable caught) {
                        // Don't actually do anything.
                        // Descriptions are expendable
                    }
                });

        funnel.logSuggestionsShown(pageTitle, results.getPageTitles());
    }

    private final class ReadMoreAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<PageTitle> results;

        private ReadMoreAdapter(LayoutInflater inflater, List<PageTitle> results) {
            this.inflater = inflater;
            this.results = results;
        }

        @Override
        public int getCount() {
            return results == null ? 0 : results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_search_result, parent, false);
            }
            TextView pageTitleText = (TextView) convertView.findViewById(R.id.result_title);
            PageTitle result = (PageTitle) getItem(position);
            pageTitleText.setText(result.getDisplayText());

            TextView descriptionText = (TextView) convertView.findViewById(R.id.result_description);
            descriptionText.setText(result.getDescription());

            ImageView imageView = (ImageView) convertView.findViewById(R.id.result_image);
            String thumbnail = result.getThumbUrl();
            if (thumbnail == null) {
                Picasso.with(parent.getContext())
                        .load(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            } else {
                Picasso.with(parent.getContext())
                        .load(thumbnail)
                        .placeholder(R.drawable.ic_pageimage_placeholder)
                        .error(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            }

            return convertView;
        }
    }
}
