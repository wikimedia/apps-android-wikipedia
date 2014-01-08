package org.wikipedia.editing;

import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;
import org.wikipedia.CommunicationBridge;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.Section;

import java.util.List;

public class EditHandler implements CommunicationBridge.JSEventListener {
    private final Context context;
    private final CommunicationBridge bridge;
    private final Page currentPage;

    public EditHandler(Context context, CommunicationBridge bridge, Page currentPage) {
        this.context = context;
        this.bridge = bridge;
        this.currentPage = currentPage;

        this.bridge.addListener("editSectionClicked", this);
    }

    @Override
    public JSONObject onMessage(String messageType, JSONObject messagePayload) {
        if (messageType.equals("editSectionClicked")) {
            int id = Integer.valueOf(messagePayload.optInt("sectionID"));
            Section section = Section.findSectionForID(currentPage.getSections(), id);
            Intent intent = new Intent(context, EditSectionActivity.class);
            intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
            intent.putExtra(EditSectionActivity.EXTRA_SECTION, section);
            intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
            context.startActivity(intent);
        }
        return null;
    }
}
