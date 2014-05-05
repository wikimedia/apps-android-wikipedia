package org.wikipedia.editing;

import android.content.*;
import android.support.v4.app.*;
import org.json.*;
import org.wikipedia.bridge.*;
import org.wikipedia.page.*;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int REQUEST_EDIT_SECTION = 1;
    public static final int RESULT_REFRESH_PAGE = 1;

    private final Fragment fragment;
    private final CommunicationBridge bridge;
    private Page currentPage;

    public EditHandler(Fragment fragment, CommunicationBridge bridge) {
        this.fragment = fragment;
        this.bridge = bridge;

        this.bridge.addListener("editSectionClicked", this);
    }

    public void setPage(Page page) {
        this.currentPage = page;
    }

    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        if (messageType.equals("editSectionClicked")) {
            int id = Integer.valueOf(messagePayload.optInt("sectionID"));
            Section section = Section.findSectionForID(currentPage.getSections(), id);
            Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
            intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
            intent.putExtra(EditSectionActivity.EXTRA_SECTION, section);
            intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
            fragment.startActivityForResult(intent, REQUEST_EDIT_SECTION);
        }
    }
}
