package org.wikipedia.edit;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ProtectedEditAttemptFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.Section;
import org.wikipedia.util.log.L;

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
        this.funnel = new ProtectedEditAttemptFunnel(WikipediaApp.getInstance(), page.getTitle().getWikiSite());
    }

    public void startEditingSection(int sectionID, @Nullable String highlightText) {
        if (!currentPage.getPageProperties().canEdit()) {
            showUneditableDialog();
            return;
        }
        if (sectionID < 0 || sectionID >= currentPage.getSections().size()) {
            L.w("Attempting to edit a mismatched section ID.");
            return;
        }
        Section section = currentPage.getSections().get(sectionID);
        Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
        intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
        intent.putExtra(EditSectionActivity.EXTRA_SECTION_ID, section.getId());
        intent.putExtra(EditSectionActivity.EXTRA_SECTION_HEADING, section.getHeading());
        intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
        intent.putExtra(EditSectionActivity.EXTRA_PAGE_PROPS, currentPage.getPageProperties());
        intent.putExtra(EditSectionActivity.EXTRA_HIGHLIGHT_TEXT, highlightText);
        fragment.startActivityForResult(intent, Constants.ACTIVITY_REQUEST_EDIT_SECTION);
    }

    public void showUneditableDialog() {
        new AlertDialog.Builder(fragment.getActivity())
                .setCancelable(false)
                .setTitle(R.string.page_protected_can_not_edit_title)
                .setMessage(AccountUtil.isLoggedIn()
                        ? R.string.page_protected_can_not_edit
                        : R.string.page_protected_can_not_edit_anon)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        funnel.log(currentPage.getPageProperties().getEditProtectionStatus());
    }

    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        if (!fragment.isAdded()) {
            return;
        }
        if (messageType.equals("editSectionClicked")) {
            startEditingSection(messagePayload.optInt("sectionID"), null);
        }
    }
}
