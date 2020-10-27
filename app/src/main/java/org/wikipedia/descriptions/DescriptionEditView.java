package org.wikipedia.descriptions;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ABTestDescriptionEditChecksFunnel;
import org.wikipedia.descriptions.DescriptionEditActivity.Action;
import org.wikipedia.language.LanguageUtil;
import org.wikipedia.mlkit.MlKitLanguageDetector;
import org.wikipedia.page.PageTitle;
import org.wikipedia.suggestededits.PageSummaryForEdit;
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

import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class DescriptionEditView extends LinearLayout implements MlKitLanguageDetector.Callback {
    private static final int TEXT_VALIDATE_DELAY_MILLIS = 1000;

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
    @BindView(R.id.view_description_edit_scrollview) ScrollView scrollView;

    @Nullable private String originalDescription;
    @Nullable private Callback callback;
    private Activity activity;
    private PageTitle pageTitle;
    private PageSummaryForEdit pageSummaryForEdit;
    private Action action;
    private boolean isTranslationEdit;
    private boolean isTextValid;

    private Runnable textValidateRunnable = this::validateText;
    private ABTestDescriptionEditChecksFunnel funnel = new ABTestDescriptionEditChecksFunnel();

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
        setVoiceInput();
        setHintText();
        setDescription(originalDescription);
        setReviewHeaderText(false);
    }

    private void setVoiceInput() {
        pageDescriptionLayout.setEndIconOnClickListener(view -> {
            if (callback != null) {
                callback.onVoiceInputClick();
            }
        });
    }

    private void setHintText() {
        pageDescriptionLayout.setHintTextAppearance(R.style.DescriptionEditViewHintTextStyle);
        pageDescriptionLayout.setHint(getHintText(pageTitle.getWikiSite().languageCode()));
    }

    private int getHeaderTextRes(boolean inReview) {
        if (inReview) {
            if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
                return R.string.suggested_edits_review_image_caption;
            } else {
                return R.string.suggested_edits_review_description;
            }
        }

        if (TextUtils.isEmpty(originalDescription)) {
            if (action == TRANSLATE_DESCRIPTION) {
                return R.string.description_edit_translate_description;
            } else if (action == ADD_CAPTION) {
                return R.string.description_edit_add_image_caption;
            } else if (action == TRANSLATE_CAPTION) {
                return R.string.description_edit_translate_image_caption;
            } else {
                return R.string.description_edit_add_description;
            }
        } else {
            if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
                return R.string.description_edit_edit_image_caption;
            } else {
                return R.string.description_edit_edit_description;
            }
        }
    }

    private CharSequence getLabelText(@NonNull String lang) {
        if (action == TRANSLATE_DESCRIPTION) {
            return getContext().getString(R.string.description_edit_translate_article_description_label_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (action == TRANSLATE_CAPTION) {
            return getContext().getString(R.string.description_edit_translate_caption_label_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (action == ADD_CAPTION) {
            return getContext().getString(R.string.description_edit_add_caption_label);
        } else {
            return getContext().getString(R.string.description_edit_article_description_label);
        }
    }

    private CharSequence getHintText(@NonNull String lang) {
        if (action == TRANSLATE_DESCRIPTION) {
            return getContext().getString(R.string.description_edit_translate_article_description_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (action == TRANSLATE_CAPTION) {
            return getContext().getString(R.string.description_edit_translate_caption_hint_per_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(lang));
        } else if (action == ADD_CAPTION) {
            return getContext().getString(R.string.description_edit_add_caption_hint);
        } else {
            return getContext().getString(R.string.description_edit_text_hint);
        }
    }

    private void setReviewHeaderText(boolean inReview) {
        headerText.setText(getContext().getString(getHeaderTextRes(inReview)));
    }

    private void setDarkReviewScreen(boolean enabled) {
        if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
            int whiteRes = getResources().getColor(android.R.color.white);
            toolbarContainer.setBackgroundResource(enabled ? android.R.color.black : ResourceUtil.getThemedAttributeId(getContext(), R.attr.paper_color));
            saveButton.setColorFilter(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.themed_icon_color), PorterDuff.Mode.SRC_IN);
            cancelButton.setColorFilter(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.toolbar_icon_color), PorterDuff.Mode.SRC_IN);
            headerText.setTextColor(enabled ? whiteRes : ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_primary_color));
            ((DescriptionEditActivity) activity).updateStatusBarColor(enabled ? Color.BLACK : ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
            DeviceUtil.updateStatusBarTheme(activity, null, enabled);
            ((DescriptionEditActivity) activity).updateNavigationBarColor(enabled ? Color.BLACK : ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        }
    }

    public void setSummaries(@NonNull Activity activity, @NonNull PageSummaryForEdit sourceSummary, PageSummaryForEdit targetSummary) {
        this.activity = activity;
        // the summary data that will bring to the review screen
        pageSummaryForEdit = isTranslationEdit ? targetSummary : sourceSummary;

        pageSummaryContainer.setVisibility(View.VISIBLE);
        pageSummaryLabel.setText(getLabelText(sourceSummary.getLang()));
        pageSummaryText.setText(StringUtil.strip(StringUtil.removeHTMLTags(isTranslationEdit || action == ADD_CAPTION
                ? sourceSummary.getDescription() : sourceSummary.getExtractHtml())));
        if (pageSummaryText.getText().toString().isEmpty() || (action == ADD_CAPTION)
                && !TextUtils.isEmpty(sourceSummary.getPageTitle().getDescription())) {
            pageSummaryContainer.setVisibility(GONE);
        }
        setConditionalLayoutDirection(this, isTranslationEdit ? sourceSummary.getLang() : pageTitle.getWikiSite().languageCode());
        setUpBottomBar();
    }

    private void setUpBottomBar() {
        bottomBarContainer.setSummary(pageSummaryForEdit);
        bottomBarContainer.setOnClickListener(view -> performReadArticleClick());
    }

    public void setSaveState(boolean saving) {
        showProgressBar(saving);
        if (saving) {
            enableSaveButton(false, true);
        } else {
            updateSaveButtonEnabled();
        }
    }

    public void loadReviewContent(boolean enabled) {
        if (enabled) {
            pageReviewContainer.setSummary(pageSummaryForEdit, getDescription(), action == ADD_CAPTION || action == TRANSLATE_CAPTION);
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
        return pageDescriptionText.getText().toString().trim();
    }

    public void setError(@Nullable CharSequence text) {
        pageDescriptionLayout.setErrorIconDrawable(R.drawable.ic_error_black_24dp);
        ColorStateList colorStateList = ColorStateList.valueOf(ResourceUtil.getThemedColor(getContext(), R.attr.colorError));
        pageDescriptionLayout.setErrorIconTintList(colorStateList);
        pageDescriptionLayout.setErrorTextColor(colorStateList);
        pageDescriptionLayout.setBoxStrokeErrorColor(colorStateList);
        layoutErrorState(text);
    }

    private void setWarning(@Nullable CharSequence text) {
        pageDescriptionLayout.setErrorIconDrawable(R.drawable.ic_warning_24);
        ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.yellow30));
        pageDescriptionLayout.setErrorIconTintList(colorStateList);
        pageDescriptionLayout.setErrorTextColor(colorStateList);
        pageDescriptionLayout.setBoxStrokeErrorColor(colorStateList);
        layoutErrorState(text);
    }

    private void clearError() {
        pageDescriptionLayout.setError(null);
    }

    private void layoutErrorState(@Nullable CharSequence text) {
        // explicitly clear the error, to prevent a glitch in the Material library.
        clearError();
        pageDescriptionLayout.setError(text);
        if (!TextUtils.isEmpty(text)) {
            post(() -> {
                if (isAttachedToWindow()) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    @OnClick(R.id.view_description_edit_save_button) void onSaveClick() {
        validateText();
        if (!saveButton.isEnabled()) {
            return;
        }
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
        if (callback != null && pageSummaryForEdit != null) {
            callback.onBottomBarClick();
        }
    }

    @OnTextChanged(value = R.id.view_description_edit_text,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pageDescriptionTextChanged() {
        if (funnel.shouldSeeChecks()) {
            removeCallbacks(textValidateRunnable);
            postDelayed(textValidateRunnable, TEXT_VALIDATE_DELAY_MILLIS);
        } else {
            isTextValid = true;
            updateSaveButtonEnabled();
            setError(null);
        }
    }

    void validateText() {
        if (!funnel.shouldSeeChecks()) {
            return;
        }
        isTextValid = true;
        String text = pageDescriptionText.getText().toString().toLowerCase().trim();

        MlKitLanguageDetector mlKitLanguageDetector = new MlKitLanguageDetector();
        mlKitLanguageDetector.setCallback(this);
        mlKitLanguageDetector.detectLanguageFromText(text);

        if (text.length() == 0) {
            isTextValid = false;
            clearError();
        } else if (text.length() < 2) {
            isTextValid = false;
            setError(getContext().getString(R.string.description_too_short));
        } else if ((action == ADD_DESCRIPTION || action == TRANSLATE_DESCRIPTION)
                && StringUtils.endsWithAny(text, ".", ",", "!", "?")) {
            isTextValid = false;
            setError(getContext().getString(R.string.description_ends_with_punctuation));
        } else if ((action == ADD_DESCRIPTION || action == TRANSLATE_DESCRIPTION)
                && LanguageUtil.startsWithArticle(text, pageTitle.getWikiSite().languageCode())) {
            setWarning(getContext().getString(R.string.description_starts_with_article));
        } else if ((action == ADD_DESCRIPTION || action == TRANSLATE_DESCRIPTION)
                && pageTitle.getWikiSite().languageCode().equals("en") && Character.isUpperCase(pageDescriptionText.getText().toString().charAt(0))) {
            setWarning(getContext().getString(R.string.description_starts_with_uppercase));
        } else {
            clearError();
        }

        updateSaveButtonEnabled();
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
        FeedbackUtil.setButtonLongPressToast(saveButton, cancelButton, helpButton);
        setOrientation(VERTICAL);
    }

    private void updateSaveButtonEnabled() {
        if (!TextUtils.isEmpty(pageDescriptionText.getText())
                && !StringUtils.equals(originalDescription, pageDescriptionText.getText())
                && isTextValid) {
            enableSaveButton(true, false);
        } else {
            enableSaveButton(false, false);
        }
    }

    private void enableSaveButton(boolean enabled, boolean saveInProgress) {
        if (saveInProgress) {
            saveButton.setImageResource(R.drawable.ic_check_circle_black_24dp);
            ImageViewCompat.setImageTintList(saveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(getContext(), R.attr.themed_icon_color)));
            saveButton.setEnabled(false);
            saveButton.setAlpha(1 / 2f);
        } else {
            saveButton.setAlpha(1f);
            if (enabled) {
                saveButton.setImageResource(R.drawable.ic_check_circle_black_24dp);
                ImageViewCompat.setImageTintList(saveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(getContext(), R.attr.themed_icon_color)));
                saveButton.setEnabled(true);
            } else {
                saveButton.setImageResource(R.drawable.ic_check_black_24dp);
                ImageViewCompat.setImageTintList(saveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color)));
                saveButton.setEnabled(false);
            }
        }
    }

    public void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setAction(Action action) {
        this.action = action;
        isTranslationEdit = (action == TRANSLATE_CAPTION || action == TRANSLATE_DESCRIPTION);
    }

    @Override
    public void onLanguageDetectionSuccess(@NonNull String languageCode) {
        if (!languageCode.equals(pageSummaryForEdit.getLang())) {
            setWarning(getContext().getString(R.string.description_is_in_different_language,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(pageSummaryForEdit.getLang())));
        }
    }
}
