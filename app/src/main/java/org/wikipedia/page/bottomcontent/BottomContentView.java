package org.wikipedia.page.bottomcontent;

import android.content.Context;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageContainerLongPressHandler;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.search.FullTextSearchClient;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GeoUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ConfigurableTextView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.LinearLayoutOverWebView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;

import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.L10nUtil.formatDateRelative;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class BottomContentView extends LinearLayoutOverWebView
        implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnContentHeightChangedListener {

    private PageFragment parentFragment;
    private WebView webView;
    private boolean firstTimeShown = false;
    private int prevLayoutHeight;
    private Page page;

    @BindView(R.id.page_languages_container) View pageLanguagesContainer;
    @BindView(R.id.page_languages_divider) View pageLanguagesDivider;
    @BindView(R.id.page_languages_count_text) TextView pageLanguagesCount;
    @BindView(R.id.page_edit_history_container) View pageEditHistoryContainer;
    @BindView(R.id.page_edit_history_divider) View pageEditHistoryDivider;
    @BindView(R.id.page_last_updated_text) TextView pageLastUpdatedText;
    @BindView(R.id.page_talk_container) View pageTalkContainer;
    @BindView(R.id.page_talk_divider) View pageTalkDivider;
    @BindView(R.id.page_view_map_container) View pageMapContainer;
    @BindView(R.id.page_license_text) TextView pageLicenseText;
    @BindView(R.id.page_external_link) TextView pageExternalLink;
    @BindView(R.id.read_more_container) View readMoreContainer;
    @BindView(R.id.read_more_list) ListView readMoreList;

    private SuggestedPagesFunnel funnel;
    private ReadMoreAdapter readMoreAdapter = new ReadMoreAdapter();
    private SearchResults readMoreItems;
    private CommunicationBridge bridge;

    public BottomContentView(Context context) {
        super(context);
        init();
    }

    public BottomContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BottomContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.fragment_page_bottom_content, this);
        ButterKnife.bind(this);
    }

    public void setup(PageFragment parentFragment, CommunicationBridge bridge, ObservableWebView webview) {
        this.parentFragment = parentFragment;
        this.webView = webview;
        this.bridge = bridge;
        setWebView(webview);

        webview.addOnScrollChangeListener(this);
        webview.addOnContentHeightChangedListener(this);

        pageExternalLink.setPaintFlags(pageExternalLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        if (parentFragment.callback() != null) {
            ListViewContextMenuListener contextMenuListener
                    = new LongPressHandler(parentFragment);

            new org.wikipedia.LongPressHandler(readMoreList, HistoryEntry.SOURCE_INTERNAL_LINK,
                    contextMenuListener);
        }

        addOnLayoutChangeListener((View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
            if (prevLayoutHeight == getHeight()) {
                return;
            }
            prevLayoutHeight = getHeight();
            padWebView();
        });

        readMoreList.setAdapter(readMoreAdapter);
        readMoreList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            PageTitle title = readMoreAdapter.getItem(position).getPageTitle();
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
            parentFragment.loadPage(title, historyEntry);
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems.getResults(), position);
        });

        // hide ourselves by default
        hide();
    }

    public void setPage(@NonNull Page page) {
        this.page = page;
        funnel = new SuggestedPagesFunnel(WikipediaApp.getInstance());
        firstTimeShown = false;

        setConditionalLayoutDirection(readMoreList, page.getTitle().getWikiSite().languageCode());

        setupAttribution();
        setupPageButtons();

        if (page.couldHaveReadMoreSection()) {
            preRequestReadMoreItems();
        } else {
            hideReadMore();
        }
        setVisibility(View.INVISIBLE);
        perturb();
    }

    @OnClick(R.id.page_external_link) void onExternalLinkClick(View v) {
        visitInExternalBrowser(parentFragment.getContext(), Uri.parse(page.getTitle().getMobileUri()));
    }

    @OnClick(R.id.page_languages_container) void onLanguagesClick(View v) {
        parentFragment.startLangLinksActivity();
    }

    @OnClick(R.id.page_edit_history_container) void onEditHistoryClick(View v) {
        visitInExternalBrowser(parentFragment.getContext(), Uri.parse(page.getTitle().getUriForAction("history")));
    }

    @OnClick(R.id.page_talk_container) void onTalkClick(View v) {
        PageTitle title = page.getTitle();
        PageTitle talkPageTitle = new PageTitle("Talk", title.getPrefixedText(), title.getWikiSite());
        visitInExternalBrowser(parentFragment.getContext(), Uri.parse(talkPageTitle.getMobileUri()));
    }

    @OnClick(R.id.page_view_map_container) void onViewMapClick(View v) {
        GeoUtil.sendGeoIntent(parentFragment.getActivity(), page.getPageProperties().getGeo(), page.getDisplayTitle());
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (getVisibility() == View.GONE) {
            return;
        }
        int contentHeight = (int)(webView.getContentHeight() * DimenUtil.getDensityScalar());
        int bottomOffset = contentHeight - scrollY - webView.getHeight();
        int bottomHeight = getHeight();
        if (bottomOffset > bottomHeight) {
            setTranslationY(bottomHeight);
            if (getVisibility() != View.INVISIBLE) {
                setVisibility(View.INVISIBLE);
            }
        } else {
            setTranslationY(bottomOffset);
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
            if (!firstTimeShown && readMoreItems != null) {
                firstTimeShown = true;
                funnel.logSuggestionsShown(page.getTitle(), readMoreItems.getResults());
            }
        }
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        perturb();
    }

    /**
     * Hide the bottom content entirely.
     * It can only be shown again by calling beginLayout()
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    private void perturb() {
        webView.post(() -> {
            if (!parentFragment.isAdded()) {
                return;
            }
            // trigger a manual scroll event to update our position relative to the WebView.
            onScrollChanged(webView.getScrollY(), webView.getScrollY(), false);
        });
    }

    private void padWebView() {
        // pad the bottom of the webview, to make room for ourselves
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(getHeight() / DimenUtil.getDensityScalar()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
    }

    private void setupPageButtons() {
        // Don't display edit history for main page or file pages, because it's always wrong
        pageEditHistoryContainer.setVisibility((page.isMainPage() || page.isFilePage()) ? GONE : VISIBLE);
        pageLastUpdatedText.setText(parentFragment.getString(R.string.last_updated_text,
                formatDateRelative(page.getPageProperties().getLastModified())));
        pageLastUpdatedText.setVisibility(View.VISIBLE);
        pageTalkContainer.setVisibility(page.getTitle().namespace() == Namespace.TALK ? GONE : VISIBLE);

        /**
         * TODO: It only updates the count when the article is in Chinese.
         * If an article is also available in Chinese, the count will be less one.
         * @see LangLinksActivity.java updateLanguageEntriesSupported()
         */
        int getLanguageCount = L10nUtil.getUpdatedLanguageCountIfNeeded(page.getTitle().getWikiSite().languageCode(),
                page.getPageProperties().getLanguageCount());

        pageLanguagesContainer.setVisibility(getLanguageCount == 0 ? GONE : VISIBLE);
        pageLanguagesCount.setText(parentFragment.getString(R.string.language_count_link_text, getLanguageCount));

        pageMapContainer.setVisibility(page.getPageProperties().getGeo() == null ? GONE : VISIBLE);

        pageLanguagesDivider.setVisibility(pageLanguagesContainer.getVisibility());
        pageTalkDivider.setVisibility(pageMapContainer.getVisibility());
    }

    private void setupAttribution() {
        pageLicenseText.setText(StringUtil.fromHtml(String
                .format(parentFragment.getContext().getString(R.string.content_license_html),
                        parentFragment.getContext().getString(R.string.cc_by_sa_3_url))));
        pageLicenseText.setMovementMethod(new LinkMovementMethod());
    }

    private String compilationInfoString(Page page) {
        return String.format(parentFragment.getString(R.string.page_offline_notice_compilation_download_date),
                page.getCompilationName(), getShortDateString(page.getCompilationDate()));
    }

    private void preRequestReadMoreItems() {
        if (page.isMainPage()) {
            new MainPageReadMoreTopicTask() {
                @Override
                public void onFinish(HistoryEntry entry) {
                    requestReadMoreItems(entry);
                }

                @Override
                public void onCatch(Throwable caught) {
                    // Read More titles are expendable.
                    L.w("Error while getting Read More topic for main page.", caught);
                }
            }.execute();
        } else {
            requestReadMoreItems(new HistoryEntry(page.getTitle(), HistoryEntry.SOURCE_INTERNAL_LINK));
        }
    }

    private void requestReadMoreItems(final HistoryEntry entry) {
        if (entry == null || TextUtils.isEmpty(entry.getTitle().getPrefixedText())) {
            hideReadMore();
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
                            readMoreAdapter.setResults(results.getResults());
                            showReadMore();
                        } else {
                            // If there's no results, just hide the section
                            hideReadMore();
                        }
                    }

                    @Override
                    public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                        // Read More titles are expendable.
                        L.w("Error while fetching Read More titles.", caught);
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
                                                         page.getTitle().getWikiSite().languageCode());
        }
        readMoreContainer.setVisibility(View.VISIBLE);
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
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems.getResults(), lastPosition);
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            super.onOpenInNewTab(title, entry);
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems.getResults(), lastPosition);
        }
    }

    private final class ReadMoreAdapter extends BaseAdapter {
        private List<SearchResult> results;

        public void setResults(List<SearchResult> results) {
            this.results = results;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results == null ? 0 : Math.min(results.size(), Constants.MAX_SUGGESTION_RESULTS);
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
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_page_list_entry, parent, false);
            }
            TextView pageTitleText = convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = getItem(position);
            pageTitleText.setText(result.getPageTitle().getDisplayText());

            GoneIfEmptyTextView descriptionText = convertView.findViewById(R.id.page_list_item_description);
            descriptionText.setText(StringUtils.capitalize(result.getPageTitle().getDescription()));

            ViewUtil.loadImageUrlInto(convertView.findViewById(R.id.page_list_item_image), result.getPageTitle().getThumbUrl());
            return convertView;
        }
    }
}
