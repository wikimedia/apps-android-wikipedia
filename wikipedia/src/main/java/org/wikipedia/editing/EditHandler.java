package org.wikipedia.editing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import org.json.JSONObject;
import org.wikipedia.CommunicationBridge;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.Section;

import java.util.List;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int REQUEST_EDIT_SECTION = 1;
    public static final int RESULT_REFRESH_PAGE = 1;

    private final Fragment fragment;
    private final CommunicationBridge bridge;
    private final Page currentPage;

    public EditHandler(Fragment fragment, CommunicationBridge bridge, Page currentPage) {
        this.fragment = fragment;
        this.bridge = bridge;
        this.currentPage = currentPage;

        this.bridge.addListener("editSectionClicked", this);
    }

    @Override
    public JSONObject onMessage(String messageType, JSONObject messagePayload) {
        if (messageType.equals("editSectionClicked")) {
            int id = Integer.valueOf(messagePayload.optInt("sectionID"));
            Section section = Section.findSectionForID(currentPage.getSections(), id);
            Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
            intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
            intent.putExtra(EditSectionActivity.EXTRA_SECTION, section);
            intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
            fragment.startActivityForResult(intent, REQUEST_EDIT_SECTION);
        }
        return null;
    }
}
