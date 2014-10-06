package org.wikipedia.page;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ToCInteractionFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.views.DisableableDrawerLayout;

import java.util.ArrayList;

public class ToCHandler {
    private static final int MAX_LEVELS = 3;
    private static final int INDENTATION_WIDTH_DP = 16;
    private final ListView tocList;
    private final ProgressBar tocProgress;
    private final ImageView tocButton;
    private final CommunicationBridge bridge;
    private final DisableableDrawerLayout slidingPane;
    private ToCInteractionFunnel funnel;
    private Activity parentActivity;

    /**
     * Flag to track if the drawer is closing because a link was clicked.
     * Used to make sure that we don't track closes that are caused by
     * the user clicking on a section.
     */
    private boolean wasClicked = false;
    private boolean openedViaSwipe = true;

    public ToCHandler(final Activity activity, final DisableableDrawerLayout slidingPane, final View quickReturnBar, final CommunicationBridge bridge) {
        this.parentActivity = activity;
        this.bridge = bridge;
        this.slidingPane = slidingPane;

        this.tocList = (ListView) slidingPane.findViewById(R.id.page_toc_list);
        this.tocProgress = (ProgressBar) slidingPane.findViewById(R.id.page_toc_in_progress);
        this.tocButton = (ImageView) quickReturnBar.findViewById(R.id.search_bar_show_toc);
        final View knowToCContainer = slidingPane.findViewById(R.id.know_toc_intro_container);

        slidingPane.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                tocButton.setImageResource(R.drawable.toc_expanded);
                WikipediaApp.getInstance().adjustDrawableToTheme(tocButton.getDrawable());
                bridge.sendMessage("requestCurrentSection", new JSONObject());
                if (quickReturnBar != null) {
                    ViewAnimations.ensureTranslationY(quickReturnBar, 0);
                }
                funnel.logOpen();
                wasClicked = false;
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
                final boolean knowsToC = prefs.getBoolean(PrefKeys.getKnowTocDrawer(), false);
                if (!knowsToC) {
                    showToCIntro(prefs, slidingPane, knowToCContainer);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                tocButton.setImageResource(R.drawable.toc_collapsed);
                WikipediaApp.getInstance().adjustDrawableToTheme(tocButton.getDrawable());
                if (!wasClicked) {
                    funnel.logClose();
                }
                openedViaSwipe = true;
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        });
    }

    private void showToCIntro(final SharedPreferences prefs, DisableableDrawerLayout slidingPane, final View knowToCContainer) {
        if (openedViaSwipe) {
            knowSwipe(prefs, slidingPane.findViewById(R.id.know_toc_intro_container));
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
                    knowSwipe(prefs, knowToCContainer);
                }
            });
        }
    }

    private void knowSwipe(SharedPreferences prefs, View knowToCContainer) {
        prefs.edit().putBoolean(PrefKeys.getKnowTocDrawer(), true).commit();
        if (knowToCContainer.isShown()) {
            ViewAnimations.crossFade(knowToCContainer, tocList);
        }
    }

    public void scrollToSection(Section section) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("anchor", section.isLead() ? "heading_" + section.getId() : section.getAnchor());
        } catch (JSONException e) {
            // This won't happen
            throw new RuntimeException(e);
        }

        bridge.sendMessage("scrollToSection", payload);
    }

    public void setupToC(final Page page) {
        tocProgress.setVisibility(View.GONE);
        tocList.setVisibility(View.VISIBLE);

        funnel = new ToCInteractionFunnel((WikipediaApp)slidingPane.getContext().getApplicationContext(), page.getTitle().getSite());
        bridge.addListener("currentSectionResponse", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                int sectionID = messagePayload.optInt("sectionID");

                tocList.setItemChecked(sectionID, true);
                tocList.smoothScrollToPosition(Math.max(sectionID - 1, 0));

                Log.d("Wikipedia", "current section is is " + sectionID);
            }
        });

        if (tocList.getHeaderViewsCount() == 0) {
            TextView headerView = (TextView) LayoutInflater.from(tocList.getContext()).inflate(R.layout.header_toc_list, null, false);
            headerView.setText(page.getTitle().getDisplayText());
            tocList.addHeaderView(headerView);
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scrollToSection(page.getSections().get(0));
                    wasClicked = true;
                    funnel.logClick();
                    hide();
                }
            });
        }

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

        //enable ToC, but only if we have more than one section
        boolean enable = page.getSections().size() > 1;
        slidingPane.setSlidingEnabled(enable);

        if (enable) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
            final boolean knowsToC = prefs.getBoolean(PrefKeys.getKnowTocDrawer(), false);
            if (!knowsToC) {
                openedViaSwipe = false;
                slidingPane.openDrawer(Gravity.END);
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
            sections = new ArrayList<Section>();
            for (Section s : page.getSections()) {
                if (s.getLevel() < MAX_LEVELS && !s.isLead()) {
                    sections.add(s);
                }
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
