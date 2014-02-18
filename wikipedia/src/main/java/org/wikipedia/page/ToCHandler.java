package org.wikipedia.page;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.wikipedia.*;

import java.util.*;

public class ToCHandler {
    private final ListView tocList;
    private final Page page;
    private final View quickReturnBar;
    private final CommunicationBridge bridge;

    public ToCHandler(ListView tocList, Page page, View quickReturnBar, CommunicationBridge bridge) {
        this.tocList = tocList;
        this.page = page;
        this.quickReturnBar = quickReturnBar;
        this.bridge = bridge;
    }

    private void scrollToSection(Section section) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("sectionID", section.getId());
        } catch (JSONException e) {
            // This won't happen
            throw new RuntimeException(e);
        }

        bridge.sendMessage("scrollToSection", payload);
    }

    public void show() {
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
        Utils.ensureTranslationY(quickReturnBar, -quickReturnBar.getHeight());
        Utils.fadeIn(tocList);
    }

    public void hide() {
        Utils.fadeOut(tocList);
        Utils.ensureTranslationY(quickReturnBar, 0);
    }

    public boolean isVisible() {
        return tocList.getVisibility() == View.VISIBLE;
    }

    private static class ToCAdapter extends BaseAdapter {
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

            sectionHeading.setText(section.getHeading());

            if (section.getLevel() > 1) {
                sectionHeading.setTextColor(Color.parseColor("#898989"));
            } else {
                sectionHeading.setTextColor(Color.parseColor("#333333"));
            }
            return convertView;
        }
    }
}
