package org.wikipedia.page;

import android.graphics.*;
import android.support.v4.widget.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.nineoldandroids.view.ViewHelper;
import org.json.*;
import org.wikipedia.*;

import java.util.*;

public class ToCHandler {
    private final ListView tocList;
    private final ProgressBar tocProgress;
    private Page page;
    private final View quickReturnBar;
    private final CommunicationBridge bridge;
    private final SlidingPaneLayout slidingPane;

    public ToCHandler(final SlidingPaneLayout slidingPane, final View quickReturnBar, final CommunicationBridge bridge) {
        this.quickReturnBar = quickReturnBar;
        this.bridge = bridge;
        this.slidingPane = slidingPane;

        this.tocList = (ListView) slidingPane.findViewById(R.id.page_toc_list);
        this.tocProgress = (ProgressBar) slidingPane.findViewById(R.id.page_toc_in_progress);

        slidingPane.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            private float prevTranslateY;
            @Override
            public void onPanelOpened(View view) {
                prevTranslateY = ViewHelper.getTranslationY(quickReturnBar);
                bridge.sendMessage("requestCurrentSection", new JSONObject());
                Utils.ensureTranslationY(quickReturnBar, -quickReturnBar.getHeight());
            }

            @Override
            public void onPanelClosed(View view) {
                Utils.ensureTranslationY(quickReturnBar, (int)prevTranslateY);

            }
        });
    }

    private void scrollToSection(Section section) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("anchor", section.getAnchor());
        } catch (JSONException e) {
            // This won't happen
            throw new RuntimeException(e);
        }

        bridge.sendMessage("scrollToSection", payload);
    }

    public void setupToC(final Page page) {
        this.page = page;
        tocProgress.setVisibility(View.GONE);
        tocList.setVisibility(View.VISIBLE);

        bridge.addListener("currentSectionResponse", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                int sectionID = messagePayload.optInt("sectionID");

                tocList.setItemChecked(sectionID, true);
                tocList.smoothScrollToPosition(Math.max(sectionID - 1, 0));

                Log.d("Wikipedia", "current section is is " + sectionID);
            }
        });

        if (tocList.getAdapter() == null) {
            TextView headerView = (TextView) LayoutInflater.from(tocList.getContext()).inflate(R.layout.header_toc_list, null, false);
            headerView.setText(page.getTitle().getDisplayText());
            tocList.addHeaderView(headerView);
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scrollToSection(page.getSections().get(0));
                    hide();
                }
            });

            tocList.setAdapter(new ToCAdapter(page));
            tocList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Section section = (Section) parent.getAdapter().getItem(position);
                    scrollToSection(section);
                    hide();
                }
            });
        }
    }

    public void show() {
        slidingPane.openPane();
    }

    public void hide() {
        slidingPane.closePane();
    }

    public boolean isVisible() {
        return slidingPane.isOpen();
    }

    private static final class ToCAdapter extends BaseAdapter {
        private final ArrayList<Section> sections;
        private final PageTitle title;


        private ToCAdapter(Page page) {
            sections = new ArrayList<Section>();
            for (Section s : page.getSections()) {
                if (s.getLevel() < 3 && !s.isLead()) {
                    sections.add(s);
                }
            }
            this.title = page.getTitle();
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
