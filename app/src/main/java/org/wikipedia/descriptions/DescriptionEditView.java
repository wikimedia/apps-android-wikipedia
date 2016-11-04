package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class DescriptionEditView extends FrameLayout {
    @BindView(R.id.description_edit_page_title) TextView pageTitleText;
    @BindView(R.id.description_edit_save_button) FloatingActionButton saveButton;
    @BindView(R.id.description_edit_text) EditText editText;
    @BindView(R.id.description_edit_char_count) TextView charCountText;
    @BindView(R.id.description_edit_progress_bar) ProgressBar progressBar;

    private PageTitle pageTitle;
    private String originalDescription;
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
        this.pageTitle = pageTitle;
        setTitle(pageTitle.getDisplayText());
        setDescription(StringUtil.emptyIfNull(pageTitle.getDescription()));
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
        return editText.getText().toString();
    }

    @OnClick(R.id.description_edit_save_button) void onSaveClick() {
        if (callback != null) {
            callback.onSaveClick();
        }
    }

    @OnTextChanged(value = R.id.description_edit_text,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void descriptionTextChanged(Editable editable) {
        updateSaveButtonVisible();
        updateCharCount();
    }

    @VisibleForTesting void setTitle(@Nullable CharSequence text) {
        pageTitleText.setText(text);
    }

    @VisibleForTesting void setDescription(@Nullable String text) {
        originalDescription = text;
        editText.setText(text);
    }

    private void init() {
        inflate(getContext(), R.layout.view_description_edit, this);
        ButterKnife.bind(this);
    }

    private void updateSaveButtonVisible() {
        if (!TextUtils.isEmpty(editText.getText())
                && !originalDescription.equals(editText.getText().toString())) {
            saveButton.show();
        } else {
            saveButton.hide();
        }
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateCharCount() {
        int charCount = editText.getText().length();
        int maxChars = getResources().getInteger(R.integer.description_max_chars);
        charCountText.setText(String.format(getResources()
                .getString(R.string.description_edit_char_count), charCount, maxChars));
        @ColorInt int color = charCount > maxChars
                ? getColor(R.color.foundation_red) : getColor(R.color.foundation_gray);
        charCountText.setTextColor(color);
    }

    @ColorInt private int getColor(@ColorRes int id) {
        return ContextCompat.getColor(getContext(), id);
    }
}