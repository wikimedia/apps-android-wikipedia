package org.wikipedia.page;

import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
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
import com.nineoldandroids.view.ViewHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ToCInteractionFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.views.DisableableDrawerLayout;

import java.util.ArrayList;

public class ToCHandler {
    private final ListView tocList;
    private final ProgressBar tocProgress;
    private final CommunicationBridge bridge;
    private final DisableableDrawerLayout slidingPane;
    private ToCInteractionFunnel funnel;

    /**
     * Flag to track if the drawer is closing because a link was clicked.
     * Used to make sure that we don't track closes that are caused by
     * the user clicking on a section.
     */
    private boolean wasClicked = false;

    public ToCHandler(final DisableableDrawerLayout slidingPane, final View quickReturnBar, final CommunicationBridge bridge) {
        this.bridge = bridge;
        this.slidingPane = slidingPane;

        this.tocList = (ListView) slidingPane.findViewById(R.id.page_toc_list);
        this.tocProgress = (ProgressBar) slidingPane.findViewById(R.id.page_toc_in_progress);

        slidingPane.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            private float prevTranslateY;

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                bridge.sendMessage("requestCurrentSection", new JSONObject());
                if (quickReturnBar != null) {
                    prevTranslateY = ViewHelper.getTranslationY(quickReturnBar);
                    ViewAnimations.ensureTranslationY(quickReturnBar, -quickReturnBar.getHeight());
                }
                funnel.logOpen();
                wasClicked = false;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (quickReturnBar != null) {
                    ViewAnimations.ensureTranslationY(quickReturnBar, (int) prevTranslateY);
                }
                if (!wasClicked) {
                    funnel.logClose();
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        });
    }

    private void scrollToSection(Section section) {
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
        slidingPane.setSlidingEnabled(page.getSections().size() > 1);
    }

    public void show() {
        if (slidingPane.getSlidingEnabled()) {
            slidingPane.openDrawer();
        }
    }

    public void hide() {
        slidingPane.closeDrawer();
    }

    public boolean isVisible() {
        return slidingPane.isDrawerOpen();
    }

    private static final class ToCAdapter extends BaseAdapter {
        private final ArrayList<Section> sections;

        private ToCAdapter(Page page) {
            sections = new ArrayList<Section>();
            for (Section s : page.getSections()) {
                if (s.getLevel() < 3 && !s.isLead()) {
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
            indentLayoutParameters.width = (section.getLevel() - 1) * (int)(16 * WikipediaApp.SCREEN_DENSITY);
            sectionFiller.setLayoutParams(indentLayoutParameters);

            sectionHeading.setText(Html.fromHtml(section.getHeading()));

            if (section.getLevel() > 1) {
                sectionHeading.setTextColor(Color.parseColor("#898989"));
            } else {
                sectionHeading.setTextColor(Color.parseColor("#333333"));
            }
            return convertView;
        }
    }
}
