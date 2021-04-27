package org.wikipedia.edit;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.google.gson.JsonObject;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.descriptions.DescriptionEditUtil;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.Section;
import org.wikipedia.util.log.L;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int RESULT_REFRESH_PAGE = 1;

    private final PageFragment fragment;
    @Nullable private Page currentPage;

    public EditHandler(PageFragment fragment, CommunicationBridge bridge) {
        this.fragment = fragment;
        bridge.addListener("edit_section", this);
        bridge.addListener("add_title_description", this);
    }

    public void setPage(@Nullable Page page) {
        if (page == null) {
            return;
        }
        this.currentPage = page;
    }

    public void startEditingSection(int sectionID, @Nullable String highlightText) {
        if (currentPage == null) {
            return;
        }
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
        new AlertDialog.Builder(fragment.requireActivity())
                .setCancelable(false)
                .setTitle(R.string.page_protected_can_not_edit_title)
                .setMessage(AccountUtil.isLoggedIn()
                        ? R.string.page_protected_can_not_edit
                        : R.string.page_protected_can_not_edit_anon)
                .setPositiveButton(R.string.protected_page_warning_dialog_ok_button_text, null)
                .show();
    }

    @Override
    public void onMessage(String messageType, JsonObject messagePayload) {
        if (!fragment.isAdded() || currentPage == null) {
            return;
        }
        if (messageType.equals("edit_section")) {
            int sectionId = messagePayload.get("sectionId").getAsInt();
            if (sectionId == 0 && DescriptionEditUtil.isEditAllowed(currentPage)) {
                View tempView = new View(fragment.requireContext());
                tempView.setX(fragment.getWebView().getTouchStartX());
                tempView.setY(fragment.getWebView().getTouchStartY());
                ((ViewGroup) fragment.getView()).addView(tempView);
                PopupMenu menu = new PopupMenu(fragment.requireContext(), tempView, 0, 0, R.style.PagePopupMenu);
                menu.getMenuInflater().inflate(R.menu.menu_page_header_edit, menu.getMenu());
                menu.setOnMenuItemClickListener(new EditMenuClickListener());
                menu.setOnDismissListener(menu1 -> ((ViewGroup) fragment.getView()).removeView(tempView));
                menu.show();
            } else {
                startEditingSection(sectionId, null);
            }
        } else if (messageType.equals("add_title_description") && DescriptionEditUtil.isEditAllowed(currentPage)) {
            fragment.verifyBeforeEditingDescription(null);
        }
    }

    private class EditMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_page_header_edit_description:
                    fragment.verifyBeforeEditingDescription(null);
                    return true;
                case R.id.menu_page_header_edit_lead_section:
                    startEditingSection(0, null);
                    return true;
                default:
                    return false;
            }
        }
    }
}
