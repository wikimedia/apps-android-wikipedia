package org.wikipedia.page;

import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getkeepsafe.taptargetview.TapTargetView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ToCInteractionFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.onboarding.PrefsOnboardingStateMachine;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.WikiDrawerLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

class ToCHandler {
    private static final int MAX_LEVELS = 3;
    private static final int INDENTATION_WIDTH_DP = 16;
    private static final int ABOUT_SECTION_ID = -1;
    private final ListView tocList;
    private final ProgressBar tocProgress;
    private final CommunicationBridge bridge;
    private final WikiDrawerLayout slidingPane;
    private final TextView headerView;
    private final PageFragment fragment;
    private ToCInteractionFunnel funnel;

    /**
     * Flag to track if the drawer is closing because a link was clicked.
     * Used to make sure that we don't track closes that are caused by
     * the user clicking on a section.
     */
    private boolean wasClicked = false;

    ToCHandler(final PageFragment fragment, final WikiDrawerLayout slidingPane,
                      final CommunicationBridge bridge) {
        this.fragment = fragment;
        this.bridge = bridge;
        this.slidingPane = slidingPane;

        this.tocList = slidingPane.findViewById(R.id.page_toc_list);
        ((FrameLayout.LayoutParams) tocList.getLayoutParams()).setMargins(0, getContentTopOffsetPx(fragment.getContext()), 0, 0);
        this.tocProgress = slidingPane.findViewById(R.id.page_toc_in_progress);

        bridge.addListener("currentSectionResponse", (String messageType, JSONObject messagePayload) -> {
            int sectionID = messagePayload.optInt("sectionID");
            L.d("current section is " + sectionID);
            if (tocList.getAdapter() == null) {
                return;
            }
            int itemToSelect = 0;
            if (sectionID == ABOUT_SECTION_ID) {
                itemToSelect = tocList.getAdapter().getCount() - 1;
            } else {
                // Find the list item that corresponds to the returned sectionID.
                // Start with index 1 of the list adapter, since index 0 is the header view,
                // and won't have a Section object associated with it.
                // The lead section (id 0) will automatically fall through the loop.
                for (int i = 1; i < tocList.getAdapter().getCount() - 1; i++) {
                    if (((Section) tocList.getAdapter().getItem(i)).getId() <= sectionID) {
                        itemToSelect = i;
                    } else {
                        break;
                    }
                }
            }
            tocList.setItemChecked(itemToSelect, true);
            tocList.smoothScrollToPosition(itemToSelect);
        });

        headerView = (TextView) LayoutInflater.from(tocList.getContext()).inflate(R.layout.header_toc_list, null, false);
        tocList.addHeaderView(headerView);

        // create a dummy funnel, in case the drawer is pulled out before a page is loaded.
        funnel = new ToCInteractionFunnel(WikipediaApp.getInstance(),
                WikipediaApp.getInstance().getWikiSite(), 0, 0);

        slidingPane.addDrawerListener(new DrawerListener()); // todo: remove what was added
    }

    public void show() {
        if (slidingPane.getSlidingEnabled(Gravity.END)) {
            slidingPane.openDrawer(GravityCompat.END);
        }
    }

    public void hide() {
        slidingPane.closeDrawer(GravityCompat.END);
    }

    public boolean isVisible() {
        return slidingPane.isDrawerOpen(GravityCompat.END);
    }

    public void setEnabled(boolean enabled) {
        slidingPane.setSlidingEnabled(enabled);
    }

    void setupToC(final Page page, WikiSite wiki, boolean firstPage) {
        tocProgress.setVisibility(View.GONE);
        tocList.setVisibility(View.VISIBLE);

        headerView.setText(StringUtil.fromHtml(page.getDisplayTitle()));
        headerView.setOnClickListener((v) -> {
            scrollToSection(page.getSections().get(0));
            wasClicked = true;
            funnel.logClick(0, page.getTitle().getDisplayText());
            hide();
        });

        tocList.setAdapter(new ToCAdapter(page));
        setConditionalLayoutDirection(tocList, wiki.languageCode());
        tocList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Section section = (Section) parent.getAdapter().getItem(position);
            scrollToSection(section);
            wasClicked = true;
            funnel.logClick(position, section.getHeading());
            hide();
        });

        funnel = new ToCInteractionFunnel(WikipediaApp.getInstance(), wiki,
                page.getPageProperties().getPageId(), tocList.getAdapter().getCount());

        if (onboardingEnabled() && !page.isMainPage() && !firstPage) {
            showTocOnboarding();
        }
    }

    void scrollToSection(String sectionAnchor) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("anchor", sectionAnchor);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("scrollToSection", payload);
    }

    private void scrollToSection(Section section) {
        if (section != null) {
            // is it the bottom (about) section?
            if (section.getId() == ABOUT_SECTION_ID) {
                JSONObject payload = new JSONObject();
                try {
                    final int topPadding = 16;
                    payload.put("offset", topPadding
                            + (int)(fragment.getBottomContentView().getHeight() / DimenUtil.getDensityScalar()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                bridge.sendMessage("scrollToBottom", payload);
            } else {
                scrollToSection(section.isLead() ? "heading_" + section.getId() : section.getAnchor());
            }
        }
    }

    private boolean onboardingEnabled() {
        return PrefsOnboardingStateMachine.getInstance().isTocTutorialEnabled();
    }

    private final class ToCAdapter extends BaseAdapter {
        private final ArrayList<Section> sections;

        private ToCAdapter(Page page) {
            sections = new ArrayList<>();
            for (Section s : page.getSections()) {
                if (s.getLevel() < MAX_LEVELS && !s.isLead()) {
                    sections.add(s);
                }
            }
            // add a fake section at the end to represent the "about this article" contents at the bottom:
            sections.add(new Section(ABOUT_SECTION_ID, 0,
                    getStringForArticleLanguage(page.getTitle(), R.string.about_article_section), "", ""));
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
            View sectionFiller = convertView.findViewById(R.id.page_toc_filler);

            LinearLayout.LayoutParams indentLayoutParameters = new LinearLayout.LayoutParams(sectionFiller.getLayoutParams());
            indentLayoutParameters.width = (section.getLevel() - 1) * (int) (INDENTATION_WIDTH_DP * DimenUtil.getDensityScalar());
            sectionFiller.setLayoutParams(indentLayoutParameters);

            sectionHeading.setText(StringUtil.fromHtml(section.getHeading()));

            if (section.getLevel() > 1) {
                sectionHeading.setTextColor(
                        getThemedColor(fragment.getContext(), R.attr.secondary_text_color));
            } else {
                sectionHeading.setTextColor(
                        getThemedColor(fragment.getContext(), R.attr.primary_text_color));
            }
            return convertView;
        }
    }

    private void showTocOnboarding() {
        TabLayout pageActionTabLayout = fragment.getActivity().findViewById(R.id.page_actions_tab_layout);
        TabLayout.Tab tocTab = pageActionTabLayout.getTabAt(PageActionTab.VIEW_TOC.code());
        try {
            Field f = tocTab.getClass().getDeclaredField("mView");
            f.setAccessible(true);
            View tabView = (View) f.get(tocTab);
            FeedbackUtil.showTapTargetView(fragment.getActivity(), tabView, R.string.menu_show_toc,
                    R.string.tool_tip_toc_button, new TapTargetView.Listener() {
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            super.onTargetClick(view);
                            show();
                        }
                    });
            PrefsOnboardingStateMachine.getInstance().setTocTutorial();
        } catch (Exception e) {
            // If this fails once it will likely always fail for the same reason, so let's prevent
            // the onboarding from being attempted and failing on every page view forever.
            PrefsOnboardingStateMachine.getInstance().setTocTutorial();
            L.w("ToC onboarding failed", e);
        }
    }

    private class DrawerListener extends DrawerLayout.SimpleDrawerListener {
        private boolean sectionRequested = false;

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            fragment.getActivity().invalidateOptionsMenu();
            funnel.logOpen();
            wasClicked = false;
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            fragment.getActivity().invalidateOptionsMenu();
            if (!wasClicked) {
                funnel.logClose();
            }
            sectionRequested = false;
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            super.onDrawerSlide(drawerView, slideOffset);
            // make sure the ActionBar is showing
            fragment.showToolbar();
            fragment.setToolbarForceNoFace(slideOffset != 0);
            // request the current section to highlight, if we haven't yet
            if (!sectionRequested) {
                bridge.sendMessage("requestCurrentSection", new JSONObject());
                sectionRequested = true;
            }
        }
    }
}
