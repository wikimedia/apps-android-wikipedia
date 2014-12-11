package org.wikipedia.page.bottomcontent;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
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

public class BottomContentHandler implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnContentHeightChangedListener {
    private static final String TAG = "BottomContentHandler";
    private final PageViewFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;
    private PageTitle pageTitle;
    private final PageActivity activity;
    private final WikipediaApp app;
    private float displayDensity;
    private boolean firstTimeShown = false;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;
    private View readMoreContainer;
    private ListView readMoreList;

    private SuggestedPagesFunnel funnel;
    private FullSearchArticlesTask.FullSearchResults readMoreItems;

    public BottomContentHandler(PageViewFragment parentFragment, CommunicationBridge bridge,
                                ObservableWebView webview, LinkHandler linkHandler,
                                ViewGroup hidingView, PageTitle pageTitle) {
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
        webview.addOnContentHeightChangedListener(this);

        pageLastUpdatedText = (TextView)bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = (TextView)bottomContentContainer.findViewById(R.id.page_license_text);
        readMoreContainer = bottomContentContainer.findViewById(R.id.read_more_container);
        readMoreList = (ListView)bottomContentContainer.findViewById(R.id.read_more_list);

        // set up pass-through scroll functionality for the ListView
        readMoreList.setOnTouchListener(new View.OnTouchListener() {
            private int touchSlop = ViewConfiguration.get(readMoreList.getContext())
                                                     .getScaledTouchSlop();
            private boolean slopReached;
            private boolean doingSlopEvent;
            private boolean isPressed = false;
            private float startY;
            private float amountScrolled;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked() & MotionEvent.ACTION_MASK;
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        isPressed = true;
                        startY = event.getY();
                        amountScrolled = 0;
                        slopReached = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isPressed && !doingSlopEvent) {
                            int contentHeight = (int)(webView.getContentHeight() * displayDensity);
                            int maxScroll = contentHeight - webView.getScrollY()
                                            - webView.getHeight();
                            int scrollAmount = Math.min((int) (startY - event.getY()), maxScroll);
                            // manually scroll the WebView that's underneath us...
                            webView.scrollBy(0, scrollAmount);
                            amountScrolled += scrollAmount;
                            if (Math.abs(amountScrolled) > touchSlop && !slopReached) {
                                slopReached = true;
                                // send an artificial Move event that scrolls it by an amount
                                // that's greater than the touch slop, so that the currently
                                // highlighted item is unselected.
                                MotionEvent moveEvent = MotionEvent.obtain(event);
                                moveEvent.setLocation(event.getX(), event.getY() + touchSlop * 2);
                                doingSlopEvent = true;
                                readMoreList.dispatchTouchEvent(moveEvent);
                                doingSlopEvent = false;
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isPressed = false;
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        funnel = new SuggestedPagesFunnel(app, pageTitle.getSite());

        // preload the display density, since it will be used in a lot of places
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;
        // hide ourselves by default
        hide();
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (bottomContentContainer.getVisibility() == View.GONE) {
            return;
        }
        int contentHeight = (int)(webView.getContentHeight() * displayDensity);
        int bottomOffset = contentHeight - scrollY - webView.getHeight();
        int bottomHeight = bottomContentContainer.getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bottomContentContainer.getLayoutParams();
        if (bottomOffset > bottomHeight) {
            if (params.bottomMargin != -bottomHeight) {
                params.bottomMargin = -bottomHeight;
                params.topMargin = 0;
                bottomContentContainer.setLayoutParams(params);
                bottomContentContainer.setVisibility(View.INVISIBLE);
            }
        } else {
            params.bottomMargin = -bottomOffset;
            params.topMargin = -bottomHeight;
            bottomContentContainer.setLayoutParams(params);
            if (bottomContentContainer.getVisibility() != View.VISIBLE) {
                bottomContentContainer.setVisibility(View.VISIBLE);
            }
            if (!firstTimeShown && readMoreItems != null) {
                firstTimeShown = true;
                funnel.logSuggestionsShown(pageTitle, readMoreItems.getPageTitles());
            }
        }
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        if (bottomContentContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY());
    }

    /**
     * Hide the bottom content entirely.
     * It can only be shown again by calling beginLayout()
     */
    public void hide() {
        bottomContentContainer.setVisibility(View.GONE);
    }

    public void beginLayout() {
        setupAttribution();
        if (parentFragment.getFragment().getPage().getPageProperties().isMainPage()) {
            bottomContentContainer.findViewById(R.id.read_more_container).setVisibility(View.GONE);
            layoutContent();
        } else {
            requestReadMoreItems(activity.getLayoutInflater());
        }
    }

    private void layoutContent() {
        if (!parentFragment.isAdded()) {
            return;
        }
        bottomContentContainer.setVisibility(View.INVISIBLE);
        // keep trying until our layout has a height...
        if (bottomContentContainer.getHeight() == 0) {
            final int postDelay = 50;
            bottomContentContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutContent();
                }
            }, postDelay);
            return;
        }

        // calculate the height of the listview, based on the number of items inside it.
        ListAdapter adapter = readMoreList.getAdapter();
        if (adapter != null && adapter.getCount() > 0) {
            ViewGroup.LayoutParams params = readMoreList.getLayoutParams();
            final int itemHeight = (int)parentFragment.getResources().getDimension(R.dimen.defaultListItemSize);
            params.height = adapter.getCount() * itemHeight
                            + (readMoreList.getDividerHeight() * (adapter.getCount() - 1));
            readMoreList.setLayoutParams(params);
        }

        readMoreList.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        bottomContentContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // pad the bottom of the webview, to make room for ourselves
        int totalHeight = bottomContentContainer.getMeasuredHeight();
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(totalHeight / displayDensity));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
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
                readMoreItems = results;
                if (readMoreItems.getResults().size() > 0) {
                    // If there are results, set up section and make sure it's visible
                    setupReadMoreSection(layoutInflater, readMoreItems);
                    readMoreContainer.setVisibility(View.VISIBLE);
                } else {
                    // If there's no results, just hide the section
                    readMoreContainer.setVisibility(View.GONE);
                }
                layoutContent();
            }

            @Override
            public void onCatch(Throwable caught) {
                // Read More titles are expendable.
                Log.w(TAG, "Error while fetching Read More titles.", caught);
                // but lay out the bottom content anyway:
                layoutContent();
            }
        }.execute();
    }

    public PageTitle getTitle() {
        return pageTitle;
    }

    public void setTitle(PageTitle newTitle) {
        pageTitle = newTitle;
    }

    private void setupReadMoreSection(LayoutInflater layoutInflater,
                                      final FullSearchArticlesTask.FullSearchResults results) {
        final ReadMoreAdapter adapter = new ReadMoreAdapter(layoutInflater, results.getResults());
        readMoreList.setAdapter(adapter);
        readMoreList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
            if (!app.showImages() || thumbnail == null) {
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
