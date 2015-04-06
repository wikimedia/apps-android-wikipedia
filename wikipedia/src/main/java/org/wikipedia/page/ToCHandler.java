package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ToCInteractionFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.views.DisableableDrawerLayout;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;

public class ToCHandler {
    private static final int MAX_LEVELS = 3;
    private static final int INDENTATION_WIDTH_DP = 16;
    private static final int READ_MORE_SECTION_ID = -1;
    private final View knowToCContainer;
    private final ListView tocList;
    private final ProgressBar tocProgress;
    private final CommunicationBridge bridge;
    private final DisableableDrawerLayout slidingPane;
    private final TextView headerView;
    private final ActionBarActivity parentActivity;
    private ToCInteractionFunnel funnel;

    /**
     * Flag to track if the drawer is closing because a link was clicked.
     * Used to make sure that we don't track closes that are caused by
     * the user clicking on a section.
     */
    private boolean wasClicked = false;
    private boolean openedViaSwipe = true;

    public ToCHandler(final ActionBarActivity activity, final DisableableDrawerLayout slidingPane,
                      final CommunicationBridge bridge) {
        this.parentActivity = activity;
        this.bridge = bridge;
        this.slidingPane = slidingPane;

        this.tocList = (ListView) slidingPane.findViewById(R.id.page_toc_list);
        this.tocProgress = (ProgressBar) slidingPane.findViewById(R.id.page_toc_in_progress);
        knowToCContainer = slidingPane.findViewById(R.id.know_toc_intro_container);

        bridge.addListener("currentSectionResponse", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                int sectionID = messagePayload.optInt("sectionID");
                Log.d("Wikipedia", "current section is " + sectionID);
                if (tocList.getAdapter() == null) {
                    return;
                }
                int itemToSelect = 0;
                // Find the list item that corresponds to the returned sectionID.
                // Start with index 1 of the list adapter, since index 0 is the header view,
                // and won't have a Section object associated with it.
                // And end with the second-to-last section, since the last section is the
                // artificial Read More section, and unknown to the WebView.
                // The lead section (id 0) will automatically fall through the loop.
                for (int i = 1; i < tocList.getAdapter().getCount() - 1; i++) {
                    if (((Section) tocList.getAdapter().getItem(i)).getId() <= sectionID) {
                        itemToSelect = i;
                    } else {
                        break;
                    }
                }
                tocList.setItemChecked(itemToSelect, true);
                tocList.smoothScrollToPosition(itemToSelect);
            }
        });

        headerView = (TextView) LayoutInflater.from(tocList.getContext()).inflate(R.layout.header_toc_list, null, false);
        tocList.addHeaderView(headerView);

        slidingPane.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            private boolean sectionRequested = false;

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                parentActivity.supportInvalidateOptionsMenu();
                funnel.logOpen();
                wasClicked = false;
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
                final boolean knowsToC = prefs.getBoolean(PrefKeys.getKnowTocDrawer(), false);
                if (!knowsToC) {
                    showToCIntro(prefs, slidingPane);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                parentActivity.supportInvalidateOptionsMenu();
                ((PageActivity)parentActivity).getSearchBarHideHandler().setForceNoFade(false);
                if (!wasClicked) {
                    funnel.logClose();
                }
                openedViaSwipe = true;
                sectionRequested = false;
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // make sure the ActionBar is showing
                ((PageActivity)parentActivity).showToolbar();
                // request the current section to highlight, if we haven't yet
                if (!sectionRequested) {
                    ((PageActivity)parentActivity).getSearchBarHideHandler().setForceNoFade(true);
                    bridge.sendMessage("requestCurrentSection", new JSONObject());
                    sectionRequested = true;
                }
            }
        });
    }

    private void showToCIntro(final SharedPreferences prefs, DisableableDrawerLayout slidingPane) {
        if (openedViaSwipe) {
            knowSwipe(prefs);
        } else {
            final View gotItButton = slidingPane.findViewById(R.id.know_toc_drawer_button);
            if (!knowToCContainer.isShown()) {
                ViewAnimations.crossFade(tocList, knowToCContainer, new Runnable() {
                    @Override
                    public void run() {
                        ViewAnimations.fadeIn(gotItButton);
                    }
                });
            }
            gotItButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    knowSwipe(prefs);
                }
            });
        }
    }

    private void knowSwipe(SharedPreferences prefs) {
        prefs.edit().putBoolean(PrefKeys.getKnowTocDrawer(), true).apply();
        if (knowToCContainer.isShown()) {
            ViewAnimations.crossFade(knowToCContainer, tocList);
        }
    }

    public void scrollToSection(String sectionAnchor) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("anchor", sectionAnchor);
        } catch (JSONException e) {
            // This won't happen
            throw new RuntimeException(e);
        }
        bridge.sendMessage("scrollToSection", payload);
    }

    public void scrollToSection(Section section) {
        if (section != null) {
            // is it the bottom (read more) section?
            if (section.getId() == READ_MORE_SECTION_ID) {
                bridge.sendMessage("scrollToBottom", new JSONObject());
            } else {
                scrollToSection(
                        section.isLead() ? "heading_" + section.getId() : section.getAnchor());
            }
        }
    }

    public void setupToC(final Page page, Site site, boolean firstPage) {
        funnel = new ToCInteractionFunnel((WikipediaApp)slidingPane.getContext().getApplicationContext(), site);

        tocProgress.setVisibility(View.GONE);
        tocList.setVisibility(View.VISIBLE);

        headerView.setText(Html.fromHtml(page.getDisplayTitle()));
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToSection(page.getSections().get(0));
                wasClicked = true;
                funnel.logClick();
                hide();
            }
        });

        tocList.setAdapter(new ToCAdapter(page));
        tocList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Section section = (Section) parent.getAdapter().getItem(position);
                scrollToSection(section);
                wasClicked = true;
                funnel.logClick();
                hide();
            }
        });

        if (!page.getPageProperties().isMainPage() && !firstPage) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
            final boolean knowsToC = prefs.getBoolean(PrefKeys.getKnowTocDrawer(), false);
            if (!knowsToC) {
                openedViaSwipe = false;
                slidingPane.openDrawer(Gravity.END);
                showToCIntro(prefs, slidingPane);
            }
        }
    }

    public void show() {
        if (slidingPane.getSlidingEnabled(Gravity.END)) {
            openedViaSwipe = false;
            slidingPane.openDrawer(Gravity.END);
        }
    }

    public void hide() {
        slidingPane.closeDrawer(Gravity.END);
    }

    public boolean isVisible() {
        return slidingPane.isDrawerOpen(Gravity.END);
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
            if (page.couldHaveReadMoreSection()) {
                // add a fake section at the end to represent the "read more" contents at the bottom:
                sections.add(new Section(READ_MORE_SECTION_ID, 0,
                        parentActivity.getString(BottomContentHandler.useNewBottomContent(WikipediaApp.getInstance()) ? R.string.read_next_section : R.string.read_more_section), "", ""));
            }
        }

        @Override
        public int getCount() {
            return sections.size();
        }

        @Override
        public Object getItem(int position) {
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
            Section section = (Section) getItem(position);
            TextView sectionHeading = (TextView) convertView.findViewById(R.id.page_toc_item_text);
            View sectionFiller = convertView.findViewById(R.id.page_toc_filler);

            LinearLayout.LayoutParams indentLayoutParameters = new LinearLayout.LayoutParams(sectionFiller.getLayoutParams());
            indentLayoutParameters.width = (section.getLevel() - 1) * (int) (INDENTATION_WIDTH_DP * WikipediaApp.getInstance().getScreenDensity());
            sectionFiller.setLayoutParams(indentLayoutParameters);

            sectionHeading.setText(Html.fromHtml(section.getHeading()));

            if (section.getLevel() > 1) {
                sectionHeading.setTextColor(
                        WikipediaApp.getInstance().getResources().getColor(Utils.getThemedAttributeId(parentActivity, R.attr.toc_subsection_text_color)));
            } else {
                sectionHeading.setTextColor(
                        WikipediaApp.getInstance().getResources().getColor(Utils.getThemedAttributeId(parentActivity, R.attr.toc_section_text_color)));
            }
            return convertView;
        }
    }
}
