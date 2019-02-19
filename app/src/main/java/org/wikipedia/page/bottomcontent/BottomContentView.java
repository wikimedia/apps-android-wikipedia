package org.wikipedia.page.bottomcontent;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageContainerLongPressHandler;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GeoUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ConfigurableTextView;
import org.wikipedia.views.LinearLayoutOverWebView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.PageItemView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.util.L10nUtil.formatDateRelative;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.UriUtils.visitInExternalBrowser;

public class BottomContentView extends LinearLayoutOverWebView
        implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnContentHeightChangedListener {

    private PageFragment parentFragment;
    private WebView webView;
    private boolean firstTimeShown = false;
    private boolean webViewPadded = false;
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
    private List<RbPageSummary> readMoreItems;
    private CommunicationBridge bridge;
    private CompositeDisposable disposables = new CompositeDisposable();

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
                if (!webViewPadded) {
                    padWebView();
                }
                return;
            }
            prevLayoutHeight = getHeight();
            padWebView();
        });

        readMoreList.setAdapter(readMoreAdapter);

        // hide ourselves by default
        hide();
    }

    public void dispose() {
        disposables.clear();
    }

    public void setPage(@NonNull Page page) {
        this.page = page;
        funnel = new SuggestedPagesFunnel(WikipediaApp.getInstance());
        firstTimeShown = false;
        webViewPadded = false;

        setConditionalLayoutDirection(readMoreList, page.getTitle().getWikiSite().languageCode());

        setupAttribution();
        setupPageButtons();

        if (page.couldHaveReadMoreSection()) {
            preRequestReadMoreItems();
        } else {
            hideReadMore();
        }
        setVisibility(View.INVISIBLE);
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
        GeoUtil.sendGeoIntent(parentFragment.requireActivity(), page.getPageProperties().getGeo(), page.getDisplayTitle());
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
                readMoreAdapter.notifyDataSetChanged();
                funnel.logSuggestionsShown(page.getTitle(), readMoreItems);
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
        webViewPadded = true;
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
        // And while we wait, let's make ourselves invisible, until we're made explicitly visible
        // by the scroll handler.
        setVisibility(View.INVISIBLE);
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

    private void preRequestReadMoreItems() {
        if (page.isMainPage()) {
            disposables.add(Observable.fromCallable(new MainPageReadMoreTopicTask())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::requestReadMoreItems,
                            throwable -> {
                                L.w("Error while getting Read More topic for main page.", throwable);
                                requestReadMoreItems(null);
                            }));
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

        disposables.add(ServiceFactory.getRest(entry.getTitle().getWikiSite()).getRelatedPages(entry.getTitle().getConvertedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> response.getPages(Constants.MAX_SUGGESTION_RESULTS * 2))
                .subscribe(results -> {
                    funnel.setLatency(System.currentTimeMillis() - timeMillis);
                    readMoreItems = results;
                    if (readMoreItems != null && readMoreItems.size() > 0) {
                        readMoreAdapter.setResults(results);
                        showReadMore();
                    } else {
                        // If there's no results, just hide the section
                        hideReadMore();
                    }
                }, caught -> L.w("Error while fetching Read More titles.", caught)));
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
            return ((RbPageSummary) readMoreList.getAdapter().getItem(position)).getPageTitle(page.getTitle().getWikiSite());
        }

        @Override
        public void onOpenLink(PageTitle title, HistoryEntry entry) {
            super.onOpenLink(title, entry);
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems, lastPosition);
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            super.onOpenInNewTab(title, entry);
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems, lastPosition);
        }
    }

    private final class ReadMoreAdapter extends BaseAdapter implements PageItemView.Callback<RbPageSummary> {
        private List<RbPageSummary> results;

        public void setResults(List<RbPageSummary> results) {
            this.results = results;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results == null ? 0 : Math.min(results.size(), Constants.MAX_SUGGESTION_RESULTS);
        }

        @Override
        public RbPageSummary getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convView, ViewGroup parent) {
            PageItemView<RbPageSummary> itemView = (PageItemView<RbPageSummary>) convView;
            if (itemView == null) {
                itemView = new PageItemView<>(getContext());
                itemView.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            RbPageSummary result = getItem(position);
            PageTitle pageTitle = result.getPageTitle(page.getTitle().getWikiSite());
            itemView.setItem(result);

            ImageView primaryActionBtn = itemView.findViewById(R.id.page_list_item_action_primary);
            primaryActionBtn.setVisibility(VISIBLE);
            if (firstTimeShown) {
                setPrimaryActionDrawable(primaryActionBtn, pageTitle);
            }
            final int paddingEnd = 8;
            itemView.setPaddingRelative(itemView.getPaddingStart(), itemView.getPaddingTop(),
                    DimenUtil.roundedDpToPx(paddingEnd), itemView.getPaddingBottom());

            itemView.setCallback(this);
            itemView.setTitle(pageTitle.getDisplayText());
            itemView.setDescription(StringUtils.capitalize(pageTitle.getDescription()));
            itemView.setImageUrl(pageTitle.getThumbUrl());
            return itemView;
        }

        private void setPrimaryActionDrawable(ImageView primaryActionBtn, PageTitle pageTitle) {
            disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().findPageInAnyList(pageTitle) != null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(exists -> primaryActionBtn.setImageResource(exists
                            ? R.drawable.ic_bookmark_white_24dp
                            : R.drawable.ic_bookmark_border_black_24dp), L::w));
        }

        @Override public void onClick(@Nullable RbPageSummary item) {
            PageTitle title = item.getPageTitle(page.getTitle().getWikiSite());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
            parentFragment.loadPage(title, historyEntry);
            funnel.logSuggestionClicked(page.getTitle(), readMoreItems, results.indexOf(item));
        }

        @Override public void onActionClick(@Nullable RbPageSummary item, @NonNull View view) {
            if (item == null) {
                return;
            }
            PageTitle pageTitle = item.getPageTitle(page.getTitle().getWikiSite());
            disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().findPageInAnyList(pageTitle) != null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(pageInList -> {
                        if (!pageInList) {
                            parentFragment.addToReadingList(pageTitle, AddToReadingListDialog.InvokeSource.READ_MORE_BOOKMARK_BUTTON);
                        } else {
                            new ReadingListBookmarkMenu(view, new ReadingListBookmarkMenu.Callback() {
                                @Override
                                public void onAddRequest(@Nullable ReadingListPage page) {
                                    parentFragment.addToReadingList(pageTitle, AddToReadingListDialog.InvokeSource.READ_MORE_BOOKMARK_BUTTON);
                                }

                                @Override
                                public void onDeleted(@Nullable ReadingListPage page) {
                                    FeedbackUtil.showMessage((Activity) getContext(),
                                            getContext().getString(R.string.reading_list_item_deleted, pageTitle.getDisplayText()));
                                    setPrimaryActionDrawable((ImageView) view, pageTitle);

                                }

                                @Override
                                public void onShare() {
                                    // ignore
                                }
                            }).show(pageTitle);
                        }
                    }, L::w));
        }

        @Override public boolean onLongClick(@Nullable RbPageSummary item) {
            return false;
        }

        @Override public void onThumbClick(@Nullable RbPageSummary item) {
        }

        @Override public void onSecondaryActionClick(@Nullable RbPageSummary item, @NonNull View view) {
        }
    }

    public void updateBookmark() {
        ((ReadMoreAdapter) readMoreList.getAdapter()).notifyDataSetChanged();
    }
}
