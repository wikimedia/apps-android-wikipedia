package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

public class DescriptionEditView extends LinearLayout {
    @BindView(R.id.view_description_edit_header) TextView headerText;
    @BindView(R.id.view_description_edit_page_title) TextView pageTitleText;
    @BindView(R.id.view_description_edit_license_text) TextView licenseText;
    @BindView(R.id.view_description_edit_save_button) View saveButton;
    @BindView(R.id.view_description_edit_cancel_button) View cancelButton;
    @BindView(R.id.view_description_edit_help_button) View helpButton;
    @BindView(R.id.view_description_edit_text) EditText pageDescriptionText;
    @BindView(R.id.view_description_edit_text_layout) TextInputLayout pageDescriptionLayout;
    @BindView(R.id.view_description_edit_progress_bar) ProgressBar progressBar;

    @Nullable private String originalDescription;
    @Nullable private Callback callback;

    public interface Callback {
        void onSaveClick();
        void onHelpClick();
        void onCancelClick();
    }

    public DescriptionEditView(Context context) {
        super(context);
        init();
    }

    public DescriptionEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DescriptionEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DescriptionEditView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setPageTitle(@NonNull PageTitle pageTitle) {
        setTitle(pageTitle.getDisplayText());
        originalDescription = pageTitle.getDescription();
        setDescription(originalDescription);

        headerText.setText(getContext().getString(TextUtils.isEmpty(originalDescription)
                ? R.string.description_edit_add_description
                : R.string.description_edit_edit_description));
    }

    public void setSaveState(boolean saving) {
        showProgressBar(saving);
        if (saving) {
            enableSaveButton(false);
        } else {
            updateSaveButtonEnabled();
        }
    }

    @NonNull public String getDescription() {
        return pageDescriptionText.getText().toString();
    }

    public void setError(@Nullable CharSequence text) {
        pageDescriptionLayout.setError(text);
    }

    @OnClick(R.id.view_description_edit_save_button) void onSaveClick() {
        if (callback != null) {
            callback.onSaveClick();
        }
    }

    @OnClick(R.id.view_description_edit_help_button) void onHelpClick() {
        if (callback != null) {
            callback.onHelpClick();
        }
    }

    @OnClick(R.id.view_description_edit_cancel_button) void onCancelClick() {
        if (callback != null) {
            callback.onCancelClick();
        }
    }

    @OnTextChanged(value = R.id.view_description_edit_text,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pageDescriptionTextChanged() {
        updateSaveButtonEnabled();
        setError(null);
    }

    @OnEditorAction(R.id.view_description_edit_text)
    protected boolean descriptionTextEditorAction(int id) {
        if (id == EditorInfo.IME_ACTION_DONE) {
            if (saveButton.isEnabled() && callback != null) {
                callback.onSaveClick();
            }
            return true;
        }
        return false;
    }

    @VisibleForTesting void setTitle(@Nullable CharSequence text) {
        pageTitleText.setText(text);
    }

    @VisibleForTesting void setDescription(@Nullable String text) {
        pageDescriptionText.setText(text);
    }

    private void init() {
        inflate(getContext(), R.layout.view_description_edit, this);
        ButterKnife.bind(this);

        licenseText.setText(StringUtil.fromHtml(String
                .format(getContext().getString(R.string.description_edit_license_notice),
                        getContext().getString(R.string.terms_of_use_url),
                        getContext().getString(R.string.cc_0_url))));
        licenseText.setMovementMethod(new LinkMovementMethod());
        FeedbackUtil.setToolbarButtonLongPressToast(saveButton, cancelButton, helpButton);
        setOrientation(VERTICAL);
    }

    private void updateSaveButtonEnabled() {
        if (!TextUtils.isEmpty(pageDescriptionText.getText())
                && !StringUtils.equals(originalDescription, pageDescriptionText.getText())) {
            enableSaveButton(true);
        } else {
            enableSaveButton(false);
        }
    }

    private void enableSaveButton(boolean enabled) {
        final float disabledAlpha = 0.5f;
        saveButton.setEnabled(enabled);
        saveButton.setAlpha(enabled ? 1f : disabledAlpha);
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
