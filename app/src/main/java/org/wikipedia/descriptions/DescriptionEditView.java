package org.wikipedia.descriptions;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.suggestededits.SuggestedEditsSummary;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.PlainPasteEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class DescriptionEditView extends LinearLayout {
    @BindView(R.id.view_description_edit_toolbar_container) FrameLayout toolbarContainer;
    @BindView(R.id.view_description_edit_header) TextView headerText;
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
    @BindView(R.id.view_description_edit_page_summary_label) TextView pageSummaryLabel;
    @BindView(R.id.view_description_edit_read_article_bar_container) DescriptionEditBottomBarView bottomBarContainer;

    @Nullable private String originalDescription;
    @Nullable private Callback callback;
    private Activity activity;
    private PageTitle pageTitle;
    private SuggestedEditsSummary suggestedEditsSummary;
    private InvokeSource invokeSource;
    private boolean isTranslationEdit;

    public interface Callback {
        void onSaveClick();
        void onHelpClick();
        void onCancelClick();
        void onBottomBarClick();
        void onVoiceInputClick();
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
        originalDescription = pageTitle.getDescription();
        setHintText();
        setHelperText();
        setDescription(originalDescription);
        setReviewHeaderText(false);
    }

    private void setHintText() {
        pageDescriptionLayout.setHintTextAppearance(R.style.DescriptionEditViewHintTextStyle);
        pageDescriptionLayout.setHint(getHintText(pageTitle.getWikiSite().languageCode()));
    }

    private void setHelperText() {
        if (invokeSource == PAGE_ACTIVITY
                || invokeSource == SUGGESTED_EDITS_ADD_DESC
                || invokeSource == SUGGESTED_EDITS_TRANSLATE_DESC
                || invokeSource == FEED_CARD_SUGGESTED_EDITS_ADD_DESC
                || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) {
            pageDescriptionLayout.setHelperText(getContext().getString(R.string.description_edit_helper_text_lowercase_warning));
        }
    }

    private int getHeaderTextRes(boolean inReview) {
        if (inReview) {
            if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION
                    || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION
                    || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION
                    || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
                return R.string.suggested_edits_review_image_caption;
            } else {
                return R.string.suggested_edits_review_description;
            }
        }

        if (TextUtils.isEmpty(originalDescription)) {
            if (invokeSource == SUGGESTED_EDITS_TRANSLATE_DESC || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) {
                return R.string.description_edit_translate_description;
            } else if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION) {
                return R.string.description_edit_add_image_caption;
            } else if (invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
                return R.string.description_edit_translate_image_caption;
            } else {
                return R.string.description_edit_add_description;
            }
        } else {
            if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION
                    || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION
                    || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION
                    || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
                return R.string.description_edit_edit_image_caption;
            } else {
                return R.string.description_edit_edit_description;
            }
        }
    }

    private CharSequence getLabelText(@NonNull String lang) {
        if (invokeSource == SUGGESTED_EDITS_TRANSLATE_DESC || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) {
            return getContext().getString(R.string.description_edit_translate_article_description_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
            return getContext().getString(R.string.description_edit_translate_caption_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION) {
            return getContext().getString(R.string.description_edit_add_caption_label_per_language, WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else {
            return getContext().getString(R.string.description_edit_article_description_label_per_language, WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        }
    }

    private CharSequence getHintText(@NonNull String lang) {
        if (invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
            return getContext().getString(R.string.description_edit_translate_caption_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION) {
            return getContext().getString(R.string.description_edit_translate_caption_hint_per_language, WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else {
            return getContext().getString(R.string.description_edit_translate_article_description_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        }
    }

    private void setReviewHeaderText(boolean inReview) {
        headerText.setText(getContext().getString(getHeaderTextRes(inReview)));
    }

    private void setDarkReviewScreen(boolean enabled) {
        if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION) {
            int whiteRes = getResources().getColor(android.R.color.white);
            toolbarContainer.setBackgroundResource(enabled ? android.R.color.black : ResourceUtil.getThemedAttributeId(getContext(), R.attr.main_toolbar_color));
            saveButton.setColorFilter(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.themed_icon_color), PorterDuff.Mode.SRC_IN);
            cancelButton.setColorFilter(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.main_toolbar_icon_color), PorterDuff.Mode.SRC_IN);
            headerText.setTextColor(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.main_toolbar_title_color));
            ((DescriptionEditActivity) activity).updateStatusBarColor(enabled ? android.R.color.black : ResourceUtil.getThemedAttributeId(getContext(), R.attr.main_status_bar_color));
            DeviceUtil.updateStatusBarTheme(activity, null, enabled);
        }
    }

    public void setSummaries(@NonNull Activity activity, @NonNull SuggestedEditsSummary sourceSummary, SuggestedEditsSummary targetSummary) {
        this.activity = activity;
        // the summary data that will bring to the review screen
        suggestedEditsSummary = isTranslationEdit ? targetSummary : sourceSummary;

        pageSummaryContainer.setVisibility(View.VISIBLE);
        pageSummaryLabel.setText(getLabelText(sourceSummary.getLang()));
        pageSummaryText.setText(StringUtil.strip(StringUtils.capitalize(StringUtil.removeHTMLTags(isTranslationEdit || invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION
                ? sourceSummary.getDescription() : sourceSummary.getExtractHtml()))));
        if (pageSummaryText.getText().toString().isEmpty()
                || ((invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION))
                && !TextUtils.isEmpty(sourceSummary.getPageTitle().getDescription())) {
            pageSummaryContainer.setVisibility(GONE);
        }
        setConditionalLayoutDirection(pageSummaryContainer, (isTranslationEdit) ? sourceSummary.getLang() : pageTitle.getWikiSite().languageCode());
        setUpBottomBar();
    }

    private void setUpBottomBar() {
        bottomBarContainer.setSummary(suggestedEditsSummary);
        bottomBarContainer.setOnClickListener(view -> performReadArticleClick());
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
            pageReviewContainer.setSummary(suggestedEditsSummary, getDescription(), invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION);
            pageReviewContainer.show();
            bottomBarContainer.hide();
            descriptionEditContainer.setVisibility(GONE);
            helpButton.setVisibility(GONE);
            hideSoftKeyboard(pageReviewContainer);
        } else {
            pageReviewContainer.hide();
            bottomBarContainer.show();
            descriptionEditContainer.setVisibility(VISIBLE);
            helpButton.setVisibility(VISIBLE);
        }
        setReviewHeaderText(enabled);
        setDarkReviewScreen(enabled);
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

    @OnClick(R.id.view_description_edit_voice_input) void onVoiceInputClick() {
        if (callback != null) {
            callback.onVoiceInputClick();
        }
    }

    private void performReadArticleClick() {
        if (callback != null && suggestedEditsSummary != null) {
            callback.onBottomBarClick();
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

    public void setDescription(@Nullable String text) {
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

    public void setInvokeSource(InvokeSource source) {
        invokeSource = source;
        isTranslationEdit = (source == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC || source == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION || source == SUGGESTED_EDITS_TRANSLATE_DESC || source == SUGGESTED_EDITS_TRANSLATE_CAPTION);
    }
}
