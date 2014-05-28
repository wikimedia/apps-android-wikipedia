package org.wikipedia.editing;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.support.v4.app.*;
import org.json.*;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ProtectedEditAttemptFunnel;
import org.wikipedia.bridge.*;
import org.wikipedia.page.*;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int REQUEST_EDIT_SECTION = 1;
    public static final int RESULT_REFRESH_PAGE = 1;

    private final Fragment fragment;
    private final CommunicationBridge bridge;
    private ProtectedEditAttemptFunnel funnel;
    private Page currentPage;

    public EditHandler(Fragment fragment, CommunicationBridge bridge) {
        this.fragment = fragment;
        this.bridge = bridge;

        this.bridge.addListener("editSectionClicked", this);
    }

    public void setPage(Page page) {
        this.currentPage = page;
        this.funnel = new ProtectedEditAttemptFunnel(WikipediaApp.getInstance(), page.getTitle().getSite());
    }

    private void showUneditableDialog() {
        new AlertDialog.Builder(fragment.getActivity())
                .setCancelable(false)
                .setTitle(R.string.page_protected_can_not_edit_title)
                .setMessage(R.string.page_protected_can_not_edit)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
        funnel.log(currentPage.getPageProperties().getEditProtectionStatus());
    }
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        if (messageType.equals("editSectionClicked")) {
            if (!currentPage.getPageProperties().canEdit()) {
                showUneditableDialog();
                return;
            }
            int id = messagePayload.optInt("sectionID");
            Section section = Section.findSectionForID(currentPage.getSections(), id);
            Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
            intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
            intent.putExtra(EditSectionActivity.EXTRA_SECTION, section);
            intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
            intent.putExtra(EditSectionActivity.EXTRA_PAGE_PROPS, currentPage.getPageProperties());
            fragment.startActivityForResult(intent, REQUEST_EDIT_SECTION);
        }
    }
}
