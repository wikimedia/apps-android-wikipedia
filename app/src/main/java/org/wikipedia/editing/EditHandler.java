package org.wikipedia.editing;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ProtectedEditAttemptFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.Page;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.Section;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int RESULT_REFRESH_PAGE = 1;

    private final PageFragment fragment;
    private ProtectedEditAttemptFunnel funnel;
    private Page currentPage;

    public EditHandler(PageFragment fragment, CommunicationBridge bridge) {
        this.fragment = fragment;
        bridge.addListener("editSectionClicked", this);
    }

    public void setPage(Page page) {
        this.currentPage = page;
        this.funnel = new ProtectedEditAttemptFunnel(WikipediaApp.getInstance(), page.getTitle().getSite());
    }

    public void startEditingSection(int sectionID, @Nullable String highlightText) {
        Section section = currentPage.getSections().get(sectionID);
        Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
        intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
        intent.putExtra(EditSectionActivity.EXTRA_SECTION_ID, section.getId());
        intent.putExtra(EditSectionActivity.EXTRA_SECTION_HEADING, section.getHeading());
        intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
        intent.putExtra(EditSectionActivity.EXTRA_PAGE_PROPS, currentPage.getPageProperties());
        intent.putExtra(EditSectionActivity.EXTRA_HIGHLIGHT_TEXT, highlightText);
        fragment.startActivityForResult(intent, MainActivity.ACTIVITY_REQUEST_EDIT_SECTION);
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
        if (!fragment.isAdded()) {
            return;
        }
        if (messageType.equals("editSectionClicked")) {
            if (fragment.getHistoryEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                new AlertDialog.Builder(fragment.getActivity())
                        .setCancelable(false)
                        .setMessage(R.string.edit_saved_page_refresh)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fragment.refreshPage();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                return;
            }
            if (!currentPage.getPageProperties().canEdit()) {
                showUneditableDialog();
                return;
            }
            startEditingSection(messagePayload.optInt("sectionID"), null);
        }
    }
}
