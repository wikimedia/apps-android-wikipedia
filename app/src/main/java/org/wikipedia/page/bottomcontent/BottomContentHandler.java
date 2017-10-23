package org.wikipedia.page.bottomcontent;

import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.LongPressHandler.ListViewContextMenuListener;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SuggestedPagesFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageContainerLongPressHandler;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.search.FullTextSearchClient;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ConfigurableListView;
import org.wikipedia.views.ConfigurableTextView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import retrofit2.Call;

import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.L10nUtil.formatDateRelative;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class BottomContentHandler implements BottomContentInterface,
        ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnContentHeightChangedListener {

    private final PageFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;
    private PageTitle pageTitle;
    private final WikipediaApp app;
    private boolean firstTimeShown = false;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;
    private View readMoreContainer;
    private ConfigurableListView readMoreList;

    private SuggestedPagesFunnel funnel;
    private SearchResults readMoreItems;

    public BottomContentHandler(final PageFragment parentFragment,
                                CommunicationBridge bridge, ObservableWebView webview,
                                LinkHandler linkHandler, ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webview;
        this.linkHandler = linkHandler;
        app = WikipediaApp.getInstance();

        bottomContentContainer = hidingView;
        webview.addOnScrollChangeListener(this);
        webview.addOnContentHeightChangedListener(this);

        pageLastUpdatedText = bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = bottomContentContainer.findViewById(R.id.page_license_text);
        readMoreContainer = bottomContentContainer.findViewById(R.id.read_more_container);
        readMoreList = bottomContentContainer.findViewById(R.id.read_more_list);

        TextView pageExternalLink = bottomContentContainer.findViewById(R.id.page_external_link);
        pageExternalLink.setPaintFlags(pageExternalLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        pageExternalLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visitInExternalBrowser(parentFragment.getContext(), Uri.parse(pageTitle.getMobileUri()));
            }
        });

        if (parentFragment.callback() != null) {
            ListViewContextMenuListener contextMenuListener
                    = new LongPressHandler(parentFragment);

            new org.wikipedia.LongPressHandler(readMoreList, HistoryEntry.SOURCE_INTERNAL_LINK,
                    contextMenuListener);
        }

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
                            int contentHeight = (int)(webView.getContentHeight() * DimenUtil.getDensityScalar());
                            int maxScroll = contentHeight - webView.getScrollY() - webView.getHeight();
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

        // hide ourselves by default
        hide();
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (bottomContentContainer.getVisibility() == View.GONE) {
            return;
        }
        int contentHeight = (int)(webView.getContentHeight() * DimenUtil.getDensityScalar());
        int bottomOffset = contentHeight - scrollY - webView.getHeight();
        int bottomHeight = bottomContentContainer.getHeight();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomContentContainer.getLayoutParams();
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
                funnel.logSuggestionsShown(pageTitle, readMoreItems.getResults());
            }
        }
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        if (bottomContentContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY(), false);
    }

    /**
     * Hide the bottom content entirely.
     * It can only be shown again by calling beginLayout()
     */
    @Override
    public void hide() {
        bottomContentContainer.setVisibility(View.GONE);
    }

    @Override
    public void beginLayout() {
        firstTimeShown = false;
        setupAttribution();
        if (parentFragment.getPage().couldHaveReadMoreSection()) {
            preRequestReadMoreItems(parentFragment.getActivity().getLayoutInflater());
        } else {
            bottomContentContainer.findViewById(R.id.read_more_container).setVisibility(View.GONE);
            layoutContent();
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
            View item = View.inflate(readMoreList.getContext(), R.layout.item_page_list_entry, null);
            item.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            ViewGroup.LayoutParams params = readMoreList.getLayoutParams();
            params.height = adapter.getCount() * item.getMeasuredHeight();
            readMoreList.setLayoutParams(params);
        }

        readMoreList.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        bottomContentContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // pad the bottom of the webview, to make room for ourselves
        int totalHeight = bottomContentContainer.getMeasuredHeight();
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(totalHeight / DimenUtil.getDensityScalar()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
    }

    private void setupAttribution() {
        Page page = parentFragment.getPage();
        pageLicenseText.setText(StringUtil.fromHtml(String
                .format(parentFragment.getContext().getString(R.string.content_license_html),
                        parentFragment.getContext().getString(R.string.cc_by_sa_3_url))));
        pageLicenseText.setMovementMethod(new LinkMovementMethod());

        // Don't display last updated message for main page or file pages, because it's always wrong
        if (page.isMainPage() || page.isFilePage()) {
            pageLastUpdatedText.setVisibility(View.GONE);
        } else {
            PageTitle title = page.getTitle();
            String dateMessage = getDateMessage(page);
            // TODO: Hide the Talk link if already on a talk page
            PageTitle talkPageTitle = new PageTitle("Talk", title.getPrefixedText(), title.getWikiSite());
            String discussionHtml = "<a href=\"" + talkPageTitle.getCanonicalUri() + "\">"
                    + parentFragment.getContext().getString(R.string.talk_page_link_text) + "</a>";
            pageLastUpdatedText.setText(StringUtil.fromHtml(dateMessage + " &mdash; " + discussionHtml));
            pageLastUpdatedText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
            pageLastUpdatedText.setVisibility(View.VISIBLE);
        }
    }

    // Returns an HTML string consisting of the onwiki last modified date for network or cached
    // content, or, for a ZIM compilation, a plain string (nothing to link) with the ZIM file's
    // local last modified date (most likely, the download date).
    private String getDateMessage(Page page) {
        return page.isFromOfflineCompilation() ? compilationInfoString(page) : lastUpdatedHtml(page);
    }

    private String compilationInfoString(Page page) {
        return String.format(parentFragment.getString(R.string.page_offline_notice_compilation_download_date),
                page.getCompilationName(), getShortDateString(page.getCompilationDate()));
    }

    private String lastUpdatedHtml(Page page) {
        return "<a href=\"" + page.getTitle().getUriForAction("history") + "\">"
                + parentFragment.getContext().getString(R.string.last_updated_text,
                    formatDateRelative(page.getPageProperties().getLastModified()) + "</a>");
    }

    private void preRequestReadMoreItems(final LayoutInflater layoutInflater) {
        if (parentFragment.getPage().isMainPage()) {
            new MainPageReadMoreTopicTask(app) {
                @Override
                public void onFinish(HistoryEntry entry) {
                    requestReadMoreItems(layoutInflater, entry);
                }

                @Override
                public void onCatch(Throwable caught) {
                    // Read More titles are expendable.
                    L.w("Error while getting Read More topic for main page.", caught);
                    // but lay out the bottom content anyway:
                    layoutContent();
                }
            }.execute();
        } else {
            requestReadMoreItems(layoutInflater, new HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK));
        }
    }

    private void requestReadMoreItems(final LayoutInflater layoutInflater,
                                      final HistoryEntry entry) {
        if (entry == null || TextUtils.isEmpty(entry.getTitle().getPrefixedText())) {
            hideReadMore();
            layoutContent();
            return;
        }
        final long timeMillis = System.currentTimeMillis();
        new FullTextSearchClient().requestMoreLike(entry.getTitle().getWikiSite(),
                entry.getTitle().getPrefixedText(), null, null,
                Constants.MAX_SUGGESTION_RESULTS * 2, new FullTextSearchClient.Callback() {
                    @Override
                    public void success(@NonNull Call<MwQueryResponse> call,
                                        @NonNull SearchResults results) {
                        funnel.setLatency(System.currentTimeMillis() - timeMillis);
                        readMoreItems = SearchResults.filter(results, entry.getTitle().getPrefixedText(), true);
                        if (!readMoreItems.getResults().isEmpty()) {
                            // If there are results, set up section and make sure it's visible
                            setUpReadMoreSection(layoutInflater, readMoreItems);
                            showReadMore();
                        } else {
                            // If there's no results, just hide the section
                            hideReadMore();
                        }
                        layoutContent();
                    }

                    @Override
                    public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                        // Read More titles are expendable.
                        L.w("Error while fetching Read More titles.", caught);
                        // but lay out the bottom content anyway:
                        layoutContent();
                    }
                }
        );
    }

    private void hideReadMore() {
        readMoreContainer.setVisibility(View.GONE);
    }

    private void showReadMore() {
        if (parentFragment.isAdded()) {
            ((ConfigurableTextView) readMoreContainer.findViewById(R.id.read_more_header))
                    .setText(getStringForArticleLanguage(parentFragment.getTitle(), R.string.read_more_section),
                                                         pageTitle.getWikiSite().languageCode());
        }
        readMoreContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public PageTitle getTitle() {
        return pageTitle;
    }

    @Override
    public void setTitle(PageTitle newTitle) {
        pageTitle = newTitle;
        funnel = new SuggestedPagesFunnel(app);
    }

    private void setUpReadMoreSection(LayoutInflater layoutInflater, final SearchResults results) {
        final ReadMoreAdapter adapter = new ReadMoreAdapter(layoutInflater, results.getResults());
        readMoreList.setAdapter(adapter, pageTitle.getWikiSite().languageCode());
        readMoreList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = adapter.getItem(position).getPageTitle();
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
                parentFragment.loadPage(title, historyEntry);
                funnel.logSuggestionClicked(pageTitle, results.getResults(), position);
            }
        });
        adapter.notifyDataSetChanged();
    }

    private class LongPressHandler extends PageContainerLongPressHandler
            implements ListViewContextMenuListener {
        private int lastPosition;
        LongPressHandler(@NonNull PageFragment fragment) {
            super(fragment);
        }

        @Override
        public PageTitle getTitleForListPosition(int position) {
            lastPosition = position;
            return ((SearchResult) readMoreList.getAdapter().getItem(position)).getPageTitle();
        }

        @Override
        public void onOpenLink(PageTitle title, HistoryEntry entry) {
            super.onOpenLink(title, entry);
            funnel.logSuggestionClicked(pageTitle, readMoreItems.getResults(), lastPosition);
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            super.onOpenInNewTab(title, entry);
            funnel.logSuggestionClicked(pageTitle, readMoreItems.getResults(), lastPosition);
        }
    }

    private final class ReadMoreAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<SearchResult> results;

        private ReadMoreAdapter(LayoutInflater inflater, List<SearchResult> results) {
            this.inflater = inflater;
            this.results = results;
        }

        @Override
        public int getCount() {
            return results == null ? 0 : results.size();
        }

        @Override
        public SearchResult getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_page_list_entry, parent, false);
            }
            TextView pageTitleText = convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = getItem(position);
            pageTitleText.setText(result.getPageTitle().getDisplayText());

            GoneIfEmptyTextView descriptionText = convertView.findViewById(R.id.page_list_item_description);
            descriptionText.setText(StringUtils.capitalize(result.getPageTitle().getDescription()));

            ViewUtil.loadImageUrlInto((SimpleDraweeView) convertView.findViewById(R.id.page_list_item_image), result.getPageTitle().getThumbUrl());
            return convertView;
        }
    }
}
