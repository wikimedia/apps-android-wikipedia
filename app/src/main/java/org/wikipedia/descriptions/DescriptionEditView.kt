package org.wikipedia.descriptions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.addTextChangedListener
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewDescriptionEditBinding
import org.wikipedia.language.LanguageUtil
import org.wikipedia.mlkit.MlKitLanguageDetector
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.*
import java.util.*

class DescriptionEditView : LinearLayout, MlKitLanguageDetector.Callback {
    interface Callback {
        fun onSaveClick()
        fun onHelpClick()
        fun onCancelClick()
        fun onBottomBarClick()
        fun onVoiceInputClick()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var pageTitle: PageTitle
    private lateinit var pageSummaryForEdit: PageSummaryForEdit
    private lateinit var action: DescriptionEditActivity.Action
    private val binding = ViewDescriptionEditBinding.inflate(LayoutInflater.from(context), this)
    private val mlKitLanguageDetector = MlKitLanguageDetector()
    private val textValidateRunnable = Runnable { validateText() }
    private var originalDescription: String? = null
    private var isTranslationEdit = false
    private var isLanguageWrong = false
    private var isTextValid = false
    var callback: Callback? = null

    var description: String?
        get() = binding.viewDescriptionEditText.text.toString().trim()
        set(text) {
            binding.viewDescriptionEditText.setText(text)
        }

    init {
        FeedbackUtil.setButtonLongPressToast(binding.viewDescriptionEditSaveButton, binding.viewDescriptionEditCancelButton, binding.viewDescriptionEditHelpButton)
        orientation = VERTICAL
        mlKitLanguageDetector.callback = this

        binding.viewDescriptionEditSaveButton.setOnClickListener {
            validateText()
            if (it.isEnabled) {
                callback?.onSaveClick()
            }
        }

        binding.viewDescriptionEditHelpButton.setOnClickListener {
            callback?.onHelpClick()
        }

        binding.viewDescriptionEditCancelButton.setOnClickListener {
            callback?.onCancelClick()
        }

        binding.viewDescriptionEditPageSummaryContainer.setOnClickListener {
            performReadArticleClick()
        }

        binding.viewDescriptionEditText.addTextChangedListener {
            enqueueValidateText()
            isLanguageWrong = false
            mlKitLanguageDetector.detectLanguageFromText(binding.viewDescriptionEditText.text.toString())
        }

        binding.viewDescriptionEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (binding.viewDescriptionEditSaveButton.isEnabled) {
                    callback?.onSaveClick()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    fun setPageTitle(pageTitle: PageTitle) {
        this.pageTitle = pageTitle
        originalDescription = pageTitle.description
        setVoiceInput()
        setHintText()
        description = originalDescription
        setReviewHeaderText(false)
    }

    private fun setVoiceInput() {
        binding.viewDescriptionEditTextLayout.setEndIconOnClickListener {
            callback?.onVoiceInputClick()
        }
    }

    private fun setHintText() {
        binding.viewDescriptionEditTextLayout.setHintTextAppearance(R.style.DescriptionEditViewHintTextStyle)
        binding.viewDescriptionEditTextLayout.hint = getHintText(pageTitle.wikiSite.languageCode)
    }

    private fun getHeaderTextRes(inReview: Boolean): Int {
        if (inReview) {
            return if (action == DescriptionEditActivity.Action.ADD_CAPTION || action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                R.string.suggested_edits_review_image_caption
            } else {
                R.string.suggested_edits_review_description
            }
        }
        return if (originalDescription.isNullOrEmpty()) {
            when (action) {
                DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> R.string.description_edit_translate_description
                DescriptionEditActivity.Action.ADD_CAPTION -> R.string.description_edit_add_image_caption
                DescriptionEditActivity.Action.TRANSLATE_CAPTION -> R.string.description_edit_translate_image_caption
                else -> R.string.description_edit_add_description
            }
        } else {
            if (action == DescriptionEditActivity.Action.ADD_CAPTION || action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                R.string.description_edit_edit_image_caption
            } else {
                R.string.description_edit_edit_description
            }
        }
    }

    private fun getLabelText(lang: String): CharSequence {
        return when (action) {
            DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> {
                context.getString(R.string.description_edit_translate_article_description_label_per_language,
                        WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(lang))
            }
            DescriptionEditActivity.Action.TRANSLATE_CAPTION -> {
                context.getString(R.string.description_edit_translate_caption_label_per_language,
                        WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(lang))
            }
            DescriptionEditActivity.Action.ADD_CAPTION -> context.getString(R.string.description_edit_add_caption_label)
            else -> context.getString(R.string.description_edit_article_description_label)
        }
    }

    private fun getHintText(lang: String): CharSequence {
        return if (action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) {
            context.getString(R.string.description_edit_translate_article_description_hint_per_language,
                    WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(lang))
        } else if (action == DescriptionEditActivity.Action.ADD_CAPTION || action == DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
            context.getString(R.string.description_edit_translate_caption_hint_per_language,
                    WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(lang))
        } else {
            context.getString(R.string.description_edit_text_hint)
        }
    }

    private fun setReviewHeaderText(inReview: Boolean) {
        binding.viewDescriptionEditHeader.text = context.getString(getHeaderTextRes(inReview))
    }

    private fun setDarkReviewScreen(enabled: Boolean) {
        if (context is DescriptionEditActivity &&
                (action == DescriptionEditActivity.Action.ADD_CAPTION ||
                action == DescriptionEditActivity.Action.TRANSLATE_CAPTION)) {
            binding.viewDescriptionEditToolbarContainer.setBackgroundResource(if (enabled) android.R.color.black else ResourceUtil.getThemedAttributeId(context, R.attr.paper_color))
            binding.viewDescriptionEditSaveButton.setColorFilter(if (enabled) Color.WHITE else ResourceUtil.getThemedColor(context, R.attr.themed_icon_color), PorterDuff.Mode.SRC_IN)
            binding.viewDescriptionEditCancelButton.setColorFilter(if (enabled) Color.WHITE else ResourceUtil.getThemedColor(context, R.attr.toolbar_icon_color), PorterDuff.Mode.SRC_IN)
            binding.viewDescriptionEditHeader.setTextColor(if (enabled) Color.WHITE else ResourceUtil.getThemedColor(context, R.attr.material_theme_primary_color))
            (context as DescriptionEditActivity).updateStatusBarColor(if (enabled) Color.BLACK else ResourceUtil.getThemedColor(context, R.attr.paper_color))
            DeviceUtil.updateStatusBarTheme(context as DescriptionEditActivity, null, enabled)
            (context as DescriptionEditActivity).updateNavigationBarColor(if (enabled) Color.BLACK else ResourceUtil.getThemedColor(context, R.attr.paper_color))
        }
    }

    fun setSummaries(sourceSummary: PageSummaryForEdit, targetSummary: PageSummaryForEdit?) {
        // the summary data that will bring to the review screen
        pageSummaryForEdit = if (isTranslationEdit) targetSummary!! else sourceSummary
        binding.viewDescriptionEditPageSummaryContainer.visibility = VISIBLE
        binding.viewDescriptionEditPageSummaryLabel.text = getLabelText(sourceSummary.lang)
        binding.viewDescriptionEditPageSummary.text = StringUtil.strip(StringUtil.removeHTMLTags(if (isTranslationEdit || action == DescriptionEditActivity.Action.ADD_CAPTION) sourceSummary.description else sourceSummary.extractHtml))
        if (binding.viewDescriptionEditPageSummary.text.toString().isEmpty() || action == DescriptionEditActivity.Action.ADD_CAPTION &&
                !sourceSummary.pageTitle.description.isNullOrEmpty()) {
            binding.viewDescriptionEditPageSummaryContainer.visibility = GONE
        }
        L10nUtil.setConditionalLayoutDirection(this, if (isTranslationEdit) sourceSummary.lang else pageTitle.wikiSite.languageCode)

        binding.viewDescriptionEditReadArticleBarContainer.setSummary(pageSummaryForEdit)
        binding.viewDescriptionEditReadArticleBarContainer.setOnClickListener { performReadArticleClick() }
    }

    fun setSaveState(saving: Boolean) {
        showProgressBar(saving)
        if (saving) {
            enableSaveButton(enabled = false, saveInProgress = true)
        } else {
            updateSaveButtonEnabled()
        }
    }

    fun loadReviewContent(enabled: Boolean) {
        if (enabled) {
            binding.viewDescriptionEditReviewContainer.setSummary(pageSummaryForEdit, description.orEmpty(), action == DescriptionEditActivity.Action.ADD_CAPTION || action == DescriptionEditActivity.Action.TRANSLATE_CAPTION)
            binding.viewDescriptionEditReviewContainer.show()
            binding.viewDescriptionEditReadArticleBarContainer.hide()
            binding.viewDescriptionEditContainer.visibility = GONE
            binding.viewDescriptionEditHelpButton.visibility = GONE
            DeviceUtil.hideSoftKeyboard(binding.viewDescriptionEditReviewContainer)
        } else {
            binding.viewDescriptionEditReviewContainer.hide()
            binding.viewDescriptionEditReadArticleBarContainer.show()
            binding.viewDescriptionEditContainer.visibility = VISIBLE
            binding.viewDescriptionEditHelpButton.visibility = VISIBLE
        }
        setReviewHeaderText(enabled)
        setDarkReviewScreen(enabled)
    }

    fun showingReviewContent(): Boolean {
        return binding.viewDescriptionEditReviewContainer.isShowing
    }

    fun setError(text: CharSequence?) {
        binding.viewDescriptionEditTextLayout.setErrorIconDrawable(R.drawable.ic_error_black_24dp)
        val colorStateList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.colorError))
        binding.viewDescriptionEditTextLayout.setErrorIconTintList(colorStateList)
        binding.viewDescriptionEditTextLayout.setErrorTextColor(colorStateList)
        binding.viewDescriptionEditTextLayout.boxStrokeErrorColor = colorStateList
        layoutErrorState(text)
    }

    private fun setWarning(text: CharSequence?) {
        binding.viewDescriptionEditTextLayout.setErrorIconDrawable(R.drawable.ic_warning_24)
        val colorStateList = AppCompatResources.getColorStateList(context, R.color.yellow30)
        binding.viewDescriptionEditTextLayout.setErrorIconTintList(colorStateList)
        binding.viewDescriptionEditTextLayout.setErrorTextColor(colorStateList)
        binding.viewDescriptionEditTextLayout.boxStrokeErrorColor = colorStateList
        layoutErrorState(text)
    }

    private fun clearError() {
        binding.viewDescriptionEditTextLayout.error = null
    }

    private fun layoutErrorState(text: CharSequence?) {
        // explicitly clear the error, to prevent a glitch in the Material library.
        clearError()
        binding.viewDescriptionEditTextLayout.error = text
        if (!text.isNullOrEmpty()) {
            post {
                if (isAttachedToWindow) {
                    binding.viewDescriptionEditScrollview.fullScroll(FOCUS_DOWN)
                }
            }
        }
    }

    private fun enqueueValidateText() {
        removeCallbacks(textValidateRunnable)
        postDelayed(textValidateRunnable, TEXT_VALIDATE_DELAY_MILLIS)
    }

    private fun validateText() {
        isTextValid = true
        val text = binding.viewDescriptionEditText.text.toString().lowercase(Locale.getDefault()).trim()
        if (text.isEmpty()) {
            isTextValid = false
            clearError()
        } else if (text.length < 2) {
            isTextValid = false
            setError(context.getString(R.string.description_too_short))
        } else if ((action == DescriptionEditActivity.Action.ADD_DESCRIPTION || action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                (listOf(".", ",", "!", "?").filter { text.endsWith(it) }).isNotEmpty()) {
            isTextValid = false
            setError(context.getString(R.string.description_ends_with_punctuation))
        } else if ((action == DescriptionEditActivity.Action.ADD_DESCRIPTION || action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                LanguageUtil.startsWithArticle(text, pageTitle.wikiSite.languageCode)) {
            setWarning(context.getString(R.string.description_starts_with_article))
        } else if ((action == DescriptionEditActivity.Action.ADD_DESCRIPTION || action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION) &&
                pageTitle.wikiSite.languageCode == "en" && Character.isLowerCase(binding.viewDescriptionEditText.text.toString()[0])) {
            setWarning(context.getString(R.string.description_starts_with_lowercase))
        } else if (isLanguageWrong) {
            setWarning(context.getString(R.string.description_is_in_different_language,
                    WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(pageSummaryForEdit.lang)))
        } else {
            clearError()
        }
        updateSaveButtonEnabled()
    }

    fun setHighlightText(text: String?) {
        if (text != null && originalDescription != null) {
            postDelayed({ StringUtil.highlightEditText(binding.viewDescriptionEditText, originalDescription!!, text) }, 500)
        }
    }

    private fun updateSaveButtonEnabled() {
        if (!binding.viewDescriptionEditText.text.isNullOrEmpty() &&
                originalDescription.orEmpty() != binding.viewDescriptionEditText.text.toString() &&
                isTextValid) {
            enableSaveButton(enabled = true, saveInProgress = false)
        } else {
            enableSaveButton(enabled = false, saveInProgress = false)
        }
    }

    private fun enableSaveButton(enabled: Boolean, saveInProgress: Boolean) {
        if (saveInProgress) {
            binding.viewDescriptionEditSaveButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
            ImageViewCompat.setImageTintList(binding.viewDescriptionEditSaveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.themed_icon_color)))
            binding.viewDescriptionEditSaveButton.isEnabled = false
            binding.viewDescriptionEditSaveButton.alpha = 1 / 2f
        } else {
            binding.viewDescriptionEditSaveButton.alpha = 1f
            if (enabled) {
                binding.viewDescriptionEditSaveButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
                ImageViewCompat.setImageTintList(binding.viewDescriptionEditSaveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.themed_icon_color)))
                binding.viewDescriptionEditSaveButton.isEnabled = true
            } else {
                binding.viewDescriptionEditSaveButton.setImageResource(R.drawable.ic_check_black_24dp)
                ImageViewCompat.setImageTintList(binding.viewDescriptionEditSaveButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color)))
                binding.viewDescriptionEditSaveButton.isEnabled = false
            }
        }
    }

    fun showProgressBar(show: Boolean) {
        binding.viewDescriptionEditProgressBar.visibility = if (show) VISIBLE else GONE
    }

    fun setAction(action: DescriptionEditActivity.Action) {
        this.action = action
        isTranslationEdit = action == DescriptionEditActivity.Action.TRANSLATE_CAPTION || action == DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
    }

    private fun performReadArticleClick() {
        callback?.onBottomBarClick()
    }

    override fun onLanguageDetectionSuccess(languageCode: String) {
        if (languageCode != pageSummaryForEdit.lang &&
                languageCode != WikipediaApp.instance.appLanguageState.getDefaultLanguageCode(pageSummaryForEdit.lang)) {
            isLanguageWrong = true
            enqueueValidateText()
        }
    }

    companion object {
        private const val TEXT_VALIDATE_DELAY_MILLIS = 1000L
    }
}
