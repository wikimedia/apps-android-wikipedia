package org.wikipedia.descriptions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.PlainPasteEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class DescriptionEditView extends LinearLayout {
    @BindView(R.id.view_description_edit_header) TextView headerText;
    @BindView(R.id.view_description_edit_page_title) TextView pageTitleText;
    @BindView(R.id.view_description_edit_save_button) ImageView saveButton;
    @BindView(R.id.view_description_edit_cancel_button) ImageView cancelButton;
    @BindView(R.id.view_description_edit_help_button) View helpButton;
    @BindView(R.id.view_description_edit_text) PlainPasteEditText pageDescriptionText;
    @BindView(R.id.view_description_edit_text_layout) TextInputLayout pageDescriptionLayout;
    @BindView(R.id.view_description_edit_progress_bar) ProgressBar progressBar;
    @BindView(R.id.view_description_edit_page_summary_container) ViewGroup pageSummaryContainer;
    @BindView(R.id.view_description_edit_page_summary) TextView pageSummaryText;
    @BindView(R.id.view_description_edit_container) ViewGroup descriptionEditContainer;
    @BindView(R.id.view_description_edit_review_container) DescriptionEditReviewView pageReviewContainer;
    @BindView(R.id.view_description_edit_license_container) DescriptionEditLicenseView licenseContainer;
    @BindView(R.id.label_text) TextView labelText;
    @BindView(R.id.view_description_edit_read_article_bar_container) DescriptionEditReadArticleBarView readArticleBarContainer;

    @Nullable private String originalDescription;
    @Nullable private Callback callback;
    private PageTitle pageTitle;
    private PageSummary pageSummary;
    private boolean isTranslationEdit;
    @Nullable private CharSequence translationSourceDescription;
    @Nullable private String translationSourceLanguageCode;

    public interface Callback {
        void onSaveClick();
        void onHelpClick();
        void onCancelClick();
        void onReadArticleClick();
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

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setPageTitle(@NonNull PageTitle pageTitle) {
        this.pageTitle = pageTitle;
        setTitle(pageTitle.getDisplayText());
        originalDescription = pageTitle.getDescription();
        setDescription(originalDescription);
        setReviewHeaderText(false);
    }

    public void editTaskEnabled(boolean enabled) {
        if (enabled) {
            pageTitleText.setVisibility(View.GONE);
            licenseContainer.setVisibility(GONE);
            saveButton.setColorFilter(ResourceUtil.getThemedColor(getContext(), R.attr.themed_icon_color), android.graphics.PorterDuff.Mode.SRC_IN);
            cancelButton.setImageResource(R.drawable.ic_arrow_back_themed_24dp);
            setHintText();
        } else {
            cancelButton.setImageResource(R.drawable.ic_close_main_themed_24dp);
        }
    }

    private void setHintText() {
        pageDescriptionLayout.setHintTextAppearance(R.style.DescriptionEditViewHintTextStyle);
        pageDescriptionLayout.setHint(String.format(getContext().getString(R.string.description_edit_text_hint_per_language),
                WikipediaApp.getInstance().language().getAppLanguageCanonicalName(pageTitle.getWikiSite().languageCode())));
    }

    private void setReviewHeaderText(boolean inReview) {
        int headerTextRes = inReview ? R.string.editactionfeed_review_title_description
                : TextUtils.isEmpty(originalDescription)
                ? (isTranslationEdit ? R.string.translation_task_title : R.string.description_edit_add_description_v2)
                : R.string.description_edit_edit_description;
        headerText.setText(getContext().getString(headerTextRes));
    }

    public void setPageSummary(@NonNull PageSummary pageSummary) {
        pageSummaryContainer.setVisibility(View.VISIBLE);
        labelText.setText(isTranslationEdit
                ? String.format(getContext().getString(R.string.description_edit_text_hint_per_language),
                WikipediaApp.getInstance().language().getAppLanguageCanonicalName(translationSourceLanguageCode))
                : getContext().getString(R.string.description_edit_article));
        pageSummaryText.setText(isTranslationEdit
                ? translationSourceDescription
                : StringUtil.fromHtml(pageSummary.getExtract()));
        setConditionalLayoutDirection(pageSummaryText, (isTranslationEdit) ? translationSourceLanguageCode : pageTitle.getWikiSite().languageCode());
        readArticleBarContainer.setPageSummary(pageSummary, pageTitle.getWikiSite().languageCode());
        readArticleBarContainer.setOnClickListener(view -> performReadArticleClick());
        this.pageSummary = pageSummary;
    }

    public void setSaveState(boolean saving) {
        showProgressBar(saving);
        if (saving) {
            enableSaveButton(false);
        } else {
            updateSaveButtonEnabled();
        }
    }

    public void loadReviewContent(boolean enabled) {
        if (enabled) {
            setReviewHeaderText(true);
            pageReviewContainer.setPageSummary(pageSummary, getDescription(), pageTitle.getWikiSite().languageCode());
            pageReviewContainer.show();
            readArticleBarContainer.hide();
            descriptionEditContainer.setVisibility(GONE);
            hideSoftKeyboard(pageReviewContainer);
        } else {
            setReviewHeaderText(false);
            pageReviewContainer.hide();
            readArticleBarContainer.show();
            descriptionEditContainer.setVisibility(VISIBLE);
        }
    }

    public boolean showingReviewContent() {
        return pageReviewContainer.isShowing();
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

    @OnClick(R.id.view_description_edit_page_summary_container) void onReadArticleClick() {
        performReadArticleClick();
    }

    private void performReadArticleClick() {
        if (callback != null && pageSummary != null) {
            callback.onReadArticleClick();
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

    public void setHighlightText(@Nullable String text) {
        if (text != null && originalDescription != null) {
            final int scrollDelayMs = 500;
            postDelayed(() -> StringUtil.highlightEditText(pageDescriptionText, originalDescription, text), scrollDelayMs);
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_description_edit, this);
        ButterKnife.bind(this);
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

    public void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setTranslationEdit(boolean translationEdit) {
        isTranslationEdit = translationEdit;
    }

    public void setTranslationSources(@Nullable CharSequence description, @Nullable String languageCode) {
        if (description != null && languageCode != null) {
            this.translationSourceDescription = description;
            this.translationSourceLanguageCode = languageCode;
        }
    }
}
