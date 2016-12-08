package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.page.PageTitle;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class DescriptionEditView extends FrameLayout {
    @BindView(R.id.view_description_edit_page_title) TextView pageTitleText;
    @BindView(R.id.view_description_edit_save_button) FloatingActionButton saveButton;
    @BindView(R.id.view_description_edit_text) EditText pageDescriptionText;
    @BindView(R.id.view_description_edit_text_layout) TextInputLayout pageDescriptionLayout;
    @BindView(R.id.view_description_edit_progress_bar) ProgressBar progressBar;

    @Nullable private String originalDescription;
    @Nullable private Callback callback;

    public interface Callback {
        void onSaveClick();
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
    }

    public void setSaveState(boolean saving) {
        showProgressBar(saving);
        if (saving) {
            saveButton.hide();
        } else {
            updateSaveButtonVisible();
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

    @OnTextChanged(value = R.id.view_description_edit_text,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pageDescriptionTextChanged() {
        updateSaveButtonVisible();
        setError(null);
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
    }

    private void updateSaveButtonVisible() {
        if (!TextUtils.isEmpty(pageDescriptionText.getText())
                && !StringUtils.equals(originalDescription, pageDescriptionText.getText())) {
            saveButton.show();
        } else {
            saveButton.hide();
        }
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}