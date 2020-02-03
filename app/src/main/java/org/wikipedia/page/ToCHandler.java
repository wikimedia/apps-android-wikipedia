package org.wikipedia.page;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getkeepsafe.taptargetview.TapTargetView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ToCInteractionFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.PageScrollerView;
import org.wikipedia.views.SwipeableListView;

import java.util.ArrayList;

import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class ToCHandler implements ObservableWebView.OnClickListener,
        ObservableWebView.OnScrollChangeListener, ObservableWebView.OnContentHeightChangedListener,
        ObservableWebView.OnEdgeSwipeListener {
    private static final float SCROLLER_BUTTON_SIZE = 44f;
    private static final float SCROLLER_BUTTON_PEEK_MARGIN = -18f;
    private static final float SCROLLER_BUTTON_HIDE_MARGIN = 48f;
    private static final float SCROLLER_BUTTON_ONBOARDING_MARGIN = 22f;
    private static final float SCROLLER_BUTTON_REVEAL_MARGIN = -30f;

    private static final float TOC_LEAD_TEXT_SIZE = 24f;
    private static final float TOC_SECTION_TEXT_SIZE = 18f;
    private static final float TOC_SUBSECTION_TEXT_SIZE = 14f;
    private static final float TOC_SEMI_FADE_ALPHA = 0.9f;
    private static final float TOC_SECTION_TOP_OFFSET_ADJUST = 70f;

    private static final int MAX_LEVELS = 3;
    private static final int ABOUT_SECTION_ID = -1;

    private final SwipeableListView tocList;
    private final PageScrollerView scrollerView;
    private final FrameLayout.LayoutParams scrollerViewParams;
    private final ViewGroup tocContainer;
    private final ObservableWebView webView;
    private final CommunicationBridge bridge;
    private final PageFragment fragment;

    private ToCAdapter adapter = new ToCAdapter();
    private ToCInteractionFunnel funnel;

    private boolean rtl;
    private boolean tocShown;
    private boolean showOnboading;
    private int currentItemSelected;

    private ValueCallback<String> sectionOffsetsCallback = new ValueCallback<String>() {
        @Override
        public void onReceiveValue(String value) {
            try {
                JSONArray sections = new JSONObject(value).getJSONArray("sections");
                for (int i = 0; i < sections.length(); i++) {
                    adapter.setYOffset(sections.getJSONObject(i).getInt("id"),
                            sections.getJSONObject(i).getInt("yOffset"));
                }
                // artificially add height for bottom About section
                adapter.setYOffset(ABOUT_SECTION_ID, webView.getContentHeight());
            } catch (JSONException e) {
                // ignore
            }
        }
    };

    ToCHandler(final PageFragment fragment, ViewGroup tocContainer, PageScrollerView scrollerView,
                      final CommunicationBridge bridge) {
        this.fragment = fragment;
        this.bridge = bridge;
        this.tocContainer = tocContainer;
        this.scrollerView = scrollerView;
        scrollerViewParams = new FrameLayout.LayoutParams(DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE), DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE));

        tocList = tocContainer.findViewById(R.id.toc_list);
        tocList.setAdapter(adapter);
        tocList.setOnItemClickListener((parent, view, position, id) -> {
            Section section = adapter.getItem(position);
            scrollToSection(section);
            funnel.logClick();
            hide();
        });
        tocList.setListener(this::hide);

        webView = fragment.getWebView();
        webView.addOnClickListener(this);
        webView.addOnScrollChangeListener(this);
        webView.addOnContentHeightChangedListener(this);
        webView.setOnEdgeSwipeListener(this);

        scrollerView.setCallback(new ScrollerCallback());
        setScrollerPosition();

        // create a dummy funnel, in case the drawer is pulled out before a page is loaded.
        funnel = new ToCInteractionFunnel(WikipediaApp.getInstance(),
                WikipediaApp.getInstance().getWikiSite(), 0, 0);
    }

    @SuppressLint("RtlHardcoded")
    void setupToC(@NonNull Page page, @NonNull WikiSite wiki, boolean firstPage) {
        adapter.setPage(page);
        rtl = L10nUtil.isLangRTL(wiki.languageCode());
        showOnboading = Prefs.isTocTutorialEnabled() && !page.isMainPage() && !firstPage;
        tocList.setRtl(rtl);
        setConditionalLayoutDirection(tocContainer, wiki.languageCode());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)tocContainer.getLayoutParams();
        params.gravity = rtl ? Gravity.LEFT : Gravity.RIGHT;
        tocContainer.setLayoutParams(params);

        log();
        funnel = new ToCInteractionFunnel(WikipediaApp.getInstance(), wiki,
                page.getPageProperties().getPageId(), adapter.getCount());
    }

    void log() {
        if (funnel != null) {
            funnel.log();
        }
    }

    void scrollToSection(@NonNull String sectionAnchor) {
        for (Section section : adapter.sections) {
            if (section.getAnchor().equals(sectionAnchor)) {
                scrollToSection(section);
                break;
            }
        }
    }

    private void scrollToSection(@Nullable Section section) {
        if (section == null) {
            return;
        }
        if (section.getId() == ABOUT_SECTION_ID) {
            bridge.execute(JavaScriptActionHandler.scrollToFooter(webView.getContext()));
        } else {
            final int topScrollExtra = 64;
            int offset = DimenUtil.roundedDpToPx(adapter.getYOffset(section.getId()) - topScrollExtra);
            webView.setScrollY(section.getId() == 0 ? 0 : offset);
        }
    }

    public void show() {
        fadeInToc(false);
        bringOutScroller();
    }

    public void hide() {
        fadeOutToc();
        bringInScroller();
    }

    public boolean isVisible() {
        return tocShown;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            scrollerView.setTranslationX(DimenUtil.roundedDpToPx(rtl ? -SCROLLER_BUTTON_HIDE_MARGIN : SCROLLER_BUTTON_HIDE_MARGIN));
            scrollerView.setVisibility(View.VISIBLE);
            setScrollerPosition();
            if (showOnboading) {
                showTocOnboarding();
            }
        } else {
            tocContainer.setVisibility(View.GONE);
            scrollerView.setVisibility(View.GONE);
            bringInScroller();
        }
    }

    @Override
    public boolean onClick(float x, float y) {
        if (isVisible()) {
            hide();
        }
        return false;
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        setScrollerPosition();
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        if (fragment.isLoading()) {
            return;
        }
        bridge.evaluate(JavaScriptActionHandler.getOffsets(), sectionOffsetsCallback);
    }

    @Override
    public void onEdgeSwipe(boolean direction) {
        if (direction == rtl) {
            show();
        }
    }

    public final class ToCAdapter extends BaseAdapter {
        private final ArrayList<Section> sections = new ArrayList<>();
        private final SparseIntArray sectionYOffsets = new SparseIntArray();
        private String pageTitle;
        private int highlightedSection;

        void setPage(@NonNull Page page) {
            sections.clear();
            sectionYOffsets.clear();
            pageTitle = page.getDisplayTitle();

            for (Section s : page.getSections()) {
                if (s.getLevel() < MAX_LEVELS) {
                    sections.add(s);
                }
            }
            // add a fake section at the end to represent the "about this article" contents at the bottom:
            sections.add(new Section(ABOUT_SECTION_ID, 0,
                    getStringForArticleLanguage(page.getTitle(), R.string.about_article_section), getStringForArticleLanguage(page.getTitle(), R.string.about_article_section), ""));
            highlightedSection = 0;
            notifyDataSetChanged();
        }

        void setHighlightedSection(int id) {
            highlightedSection = id;
            notifyDataSetChanged();
        }

        int getYOffset(int id) {
            return sectionYOffsets.get(id, 0);
        }

        void setYOffset(int id, int yOffset) {
            sectionYOffsets.put(id, yOffset);
        }

        @Override
        public int getCount() {
            return sections.size();
        }

        @Override
        public Section getItem(int position) {
            return sections.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_toc_entry, parent, false);
            }
            Section section = getItem(position);
            TextView sectionHeading = convertView.findViewById(R.id.page_toc_item_text);
            View sectionBullet = convertView.findViewById(R.id.page_toc_item_bullet);

            sectionHeading.setText(StringUtil.fromHtml(section.getHeading()));
            float textSize = TOC_SUBSECTION_TEXT_SIZE;
            if (section.isLead()) {
                textSize = TOC_LEAD_TEXT_SIZE;
                sectionHeading.setTypeface(Typeface.SERIF);
            } else if (section.getLevel() == 1) {
                textSize = TOC_SECTION_TEXT_SIZE;
                sectionHeading.setTypeface(Typeface.SERIF);
            } else {
                sectionHeading.setTypeface(Typeface.SANS_SERIF);
            }
            sectionHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sectionBullet.getLayoutParams();
            params.topMargin = DimenUtil.roundedDpToPx(textSize / 2);
            sectionBullet.setLayoutParams(params);

            if (highlightedSection == position) {
                sectionHeading.setTextColor(getThemedColor(fragment.requireContext(), R.attr.colorAccent));
            } else {
                if (section.getLevel() > 1) {
                    sectionHeading.setTextColor(
                            getThemedColor(fragment.requireContext(), R.attr.primary_text_color));
                } else {
                    sectionHeading.setTextColor(
                            getThemedColor(fragment.requireContext(), R.attr.toc_h1_h2_color));
                }
            }
            return convertView;
        }
    }

    private void showTocOnboarding() {
        try {
            showCompleteScroller(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (!fragment.isAdded()) {
                        return;
                    }
                    FeedbackUtil.showTapTargetView(fragment.requireActivity(), scrollerView, R.string.tool_tip_toc_title,
                            R.string.tool_tip_toc_text, new TapTargetView.Listener() {

                                boolean targetClicked;

                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    super.onTargetClick(view);
                                    targetClicked = true;
                                    show();
                                }

                                @Override
                                public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                    if (!targetClicked) {
                                        hide();
                                    }
                                }
                            });
                }
            });
        } catch (Exception e) {
            L.w("ToC onboarding failed", e);
        }
        Prefs.setTocTutorialEnabled(false);
    }

    @SuppressLint("RtlHardcoded")
    private void setScrollerPosition() {
        scrollerViewParams.gravity = rtl ? Gravity.LEFT : Gravity.RIGHT;
        scrollerViewParams.leftMargin = rtl ? DimenUtil.roundedDpToPx(SCROLLER_BUTTON_PEEK_MARGIN) : 0;
        scrollerViewParams.rightMargin = rtl ? 0 : DimenUtil.roundedDpToPx(SCROLLER_BUTTON_PEEK_MARGIN);
        int toolbarHeight = DimenUtil.getToolbarHeightPx(fragment.requireContext());
        scrollerViewParams.topMargin = (int) (toolbarHeight
                + (webView.getHeight() - 2 * toolbarHeight) * ((float)webView.getScrollY() / (float)webView.getContentHeight() / DimenUtil.getDensityScalar()));
        if (scrollerViewParams.topMargin < toolbarHeight) {
            scrollerViewParams.topMargin = toolbarHeight;
        }
        scrollerView.setLayoutParams(scrollerViewParams);
    }

    private void fadeInToc(boolean semiFade) {
        tocContainer.setAlpha(tocShown ? 1f : 0f);
        tocContainer.setVisibility(View.VISIBLE);
        tocContainer.animate().alpha(semiFade ? TOC_SEMI_FADE_ALPHA : 1f)
                .setDuration(tocContainer.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
        tocList.setEnabled(true);
        currentItemSelected = -1;
        onScrollerMoved(0f, false);
        funnel.scrollStart();
        if (semiFade) {
            return;
        }
        tocShown = true;
    }

    private void fadeOutToc() {
        tocContainer.animate().alpha(0f)
                .setDuration(tocContainer.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!fragment.isAdded()) {
                            return;
                        }
                        tocContainer.setVisibility(View.GONE);
                    }
                });
        tocList.setEnabled(false);
        tocShown = false;
        funnel.scrollStop();
    }

    private void bringOutScroller() {
        if (scrollerView.getVisibility() != View.VISIBLE) {
            return;
        }
        scrollerView.animate().translationX(DimenUtil.roundedDpToPx(rtl ? -SCROLLER_BUTTON_REVEAL_MARGIN : SCROLLER_BUTTON_REVEAL_MARGIN))
                .setDuration(tocContainer.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
    }

    private void bringInScroller() {
        if (scrollerView.getVisibility() != View.VISIBLE) {
            return;
        }
        scrollerView.animate().translationX(DimenUtil.roundedDpToPx(rtl ? -SCROLLER_BUTTON_HIDE_MARGIN : SCROLLER_BUTTON_HIDE_MARGIN))
                .setDuration(tocContainer.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
    }

    private void showCompleteScroller(@Nullable AnimatorListenerAdapter listenerAdapter) {
        if (scrollerView.getVisibility() != View.VISIBLE) {
            return;
        }
        scrollerView.animate().translationX(DimenUtil.roundedDpToPx(rtl ? SCROLLER_BUTTON_ONBOARDING_MARGIN : -SCROLLER_BUTTON_ONBOARDING_MARGIN))
                .setDuration(tocContainer.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(listenerAdapter);
    }

    private void scrollToListSectionByOffset(int yOffset) {
        yOffset = DimenUtil.roundedPxToDp(yOffset);
        int itemToSelect = 0;
        for (int i = 1; i < adapter.getCount(); i++) {
            Section section = adapter.getItem(i);
            if (adapter.getYOffset(section.getId()) < yOffset) {
                itemToSelect = i;
            } else {
                break;
            }
        }
        if (itemToSelect != currentItemSelected) {
            adapter.setHighlightedSection(itemToSelect);
            currentItemSelected = itemToSelect;
        }
        tocList.smoothScrollToPositionFromTop(currentItemSelected,
                scrollerViewParams.topMargin - DimenUtil.roundedDpToPx(TOC_SECTION_TOP_OFFSET_ADJUST), 0);
    }

    private void onScrollerMoved(float dy, boolean scrollWebView) {
        int webViewScrollY = webView.getScrollY();
        int webViewHeight = webView.getHeight();
        float webViewContentHeight = webView.getContentHeight() * DimenUtil.getDensityScalar();
        float scrollY = webViewScrollY;
        scrollY += (dy * webViewContentHeight / (float)(webViewHeight - 2 * DimenUtil.getToolbarHeightPx(fragment.requireContext())));
        if (scrollY < 0) {
            scrollY = 0;
        } else if (scrollY > (webViewContentHeight - webViewHeight)) {
            scrollY = webViewContentHeight - webViewHeight;
        }

        if (scrollWebView) {
            webView.scrollTo(0, (int) scrollY);
        }
        scrollToListSectionByOffset((int) scrollY + webViewHeight / 2);
    }

    private class ScrollerCallback implements PageScrollerView.Callback {
        @Override
        public void onClick() {
            show();
        }

        @Override
        public void onScrollStart() {
            fadeInToc(true);
            bringOutScroller();
        }

        @Override
        public void onScrollStop() {
            fadeOutToc();
            bringInScroller();
        }

        @Override
        public void onVerticalScroll(float dy) {
            onScrollerMoved(dy, true);
        }

        @Override
        public void onSwipeOut() {
            fadeInToc(false);
        }
    }
}
