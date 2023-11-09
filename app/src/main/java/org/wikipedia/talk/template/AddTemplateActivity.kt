package org.wikipedia.talk.template

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.ActivityAddTemplateBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.edit.SyntaxHighlightViewAdapter
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.insertmedia.InsertMediaViewModel
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.UserMentionInputView

class AddTemplateActivity : BaseActivity(), LinkPreviewDialog.Callback, UserMentionInputView.Listener {
    private lateinit var binding: ActivityAddTemplateBinding
    private lateinit var textWatcher: TextWatcher

    private val viewModel: AddTemplateViewModel by viewModels { AddTemplateViewModel.Factory(intent.extras!!) }
    private var userMentionScrolled = false

    private val wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)

    private val requestInsertMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == InsertMediaActivity.RESULT_INSERT_MEDIA_SUCCESS) {
            it.data?.let { data ->
                val imageTitle = data.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
                val imageCaption = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION)
                val imageAlt = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT)
                val imageSize = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_SIZE)
                val imageType = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_TYPE)
                val imagePos = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_POS)

                val newWikiText = InsertMediaViewModel.insertImageIntoWikiText(wikiSite.languageCode,
                    binding.addTemplateInputView.editText.text.toString(), imageTitle?.text.orEmpty(), imageCaption.orEmpty(),
                    imageAlt.orEmpty(), imageSize.orEmpty(), imageType.orEmpty(), imagePos.orEmpty(),
                    binding.addTemplateInputView.editText.selectionStart, autoInsert = false, attemptInfobox = false
                )

                binding.addTemplateInputView.editText.setText(newWikiText.first)

                val insertPos = newWikiText.third
                binding.addTemplateInputView.editText.setSelection(insertPos.first, insertPos.first + insertPos.second)
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.addTemplateToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (viewModel.talkTemplateId != -1) {
            title = getString(R.string.talk_templates_edit_message_dialog_title)
            binding.addTemplateDescription.isVisible = false
        } else {
            title = getString(R.string.talk_templates_new_message_title)
            binding.addTemplateDescription.isVisible = true
        }

        addTextWatcher()

        binding.addTemplateSaveButton.setOnClickListener {
            onSaveClicked()
        }

        binding.addTemplateInputView.wikiSite = wikiSite
        binding.addTemplateInputView.listener = this

        SyntaxHighlightViewAdapter(this, PageTitle("Main Page", wikiSite), binding.root, binding.addTemplateInputView.editText,
            binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
            Constants.InvokeSource.ADD_TEMPLATE_ACTIVITY, requestInsertMedia, true)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is AddTemplateViewModel.UiState.Success -> onInitialLoad()
                        is AddTemplateViewModel.UiState.Saved -> onSaveSuccess()
                        is AddTemplateViewModel.UiState.Error -> onError(it.throwable)
                    }
                }
            }
        }
    }

    public override fun onDestroy() {
        binding.addTemplateTitleText.removeTextChangedListener(textWatcher)
        binding.addTemplateSubjectText.removeTextChangedListener(textWatcher)
        binding.addTemplateInputView.editText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        setSaveButtonEnabled(false)
        L10nUtil.setConditionalLayoutDirection(binding.addTemplateScrollContainer, wikiSite.languageCode)
        binding.addTemplateInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
        binding.addTemplateTitleLayout.requestFocus()

        if (viewModel.talkTemplateId != -1) {
            viewModel.talkTemplate?.let {
                binding.addTemplateTitleText.setText(it.title)
                binding.addTemplateSubjectText.setText(it.subject)
                binding.addTemplateInputView.editText.setText(it.message)
            }
        }
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.addTemplateSaveButton.isEnabled = enabled
        binding.addTemplateSaveButton.setTextColor(ResourceUtil
            .getThemedColor(this, if (enabled) R.attr.progressive_color else R.attr.inactive_color))
    }

    private fun addTextWatcher() {
        textWatcher = binding.addTemplateTitleText.doOnTextChanged { _, _, _, _ ->
            binding.addTemplateTitleLayout.isErrorEnabled = false
            binding.addTemplateSubjectLayout.isErrorEnabled = false
            binding.addTemplateInputView.textInputLayout.isErrorEnabled = false
            val title = binding.addTemplateTitleText.text.toString().trim()
            val subject = binding.addTemplateSubjectText.text.toString().trim()
            val body = binding.addTemplateInputView.editText.text.toString().trim()
            if (title.isEmpty() && binding.addTemplateTitleText.isFocused) {
                PatrollerExperienceEvent.logAction("publish_error_title", "pt_templates")
                binding.addTemplateTitleLayout.isErrorEnabled = true
                binding.addTemplateTitleLayout.error = getString(R.string.talk_templates_message_title_empty)
            }
            if (subject.isEmpty() && binding.addTemplateSubjectText.isFocused) {
                PatrollerExperienceEvent.logAction("save_error_subject", "pt_templates")
                binding.addTemplateSubjectLayout.isErrorEnabled = true
                binding.addTemplateSubjectLayout.error = getString(R.string.talk_subject_empty)
            }
            if (body.isEmpty() && binding.addTemplateInputView.editText.isFocused) {
                PatrollerExperienceEvent.logAction("save_error_compose", "pt_templates")
                binding.addTemplateInputView.textInputLayout.isErrorEnabled = true
                binding.addTemplateInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            }
            var enableSaveButton = title.isNotBlank() && subject.isNotBlank() && body.isNotBlank()
            if (viewModel.talkTemplatesList.any { item -> item.title == title && item.id != viewModel.talkTemplateId }) {
                binding.addTemplateTitleLayout.isErrorEnabled = true
                binding.addTemplateTitleLayout.error = getString(R.string.talk_templates_new_message_dialog_exists, title)
                enableSaveButton = false
            }
            setSaveButtonEnabled(enableSaveButton)
        }
        binding.addTemplateSubjectText.addTextChangedListener(textWatcher)
        binding.addTemplateInputView.editText.addTextChangedListener(textWatcher)
    }

    private fun onSaveClicked() {
        val title = binding.addTemplateTitleText.text.toString().trim()
        val subject = binding.addTemplateSubjectText.text.toString().trim()
        val body = binding.addTemplateInputView.editText.text.toString().trim()

        if (title.isEmpty()) {
            // TODO: add eventlogging
            PatrollerExperienceEvent.logAction("publish_error_title", "pt_templates")
            binding.addTemplateTitleLayout.isErrorEnabled = true
            binding.addTemplateTitleLayout.error = getString(R.string.talk_templates_message_title_empty)
            binding.addTemplateTitleLayout.requestFocus()
            return
        } else if (subject.isEmpty()) {
            PatrollerExperienceEvent.logAction("save_error_subject", "pt_templates")
            binding.addTemplateSubjectLayout.isErrorEnabled = true
            binding.addTemplateSubjectLayout.error = getString(R.string.talk_subject_empty)
            binding.addTemplateSubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            PatrollerExperienceEvent.logAction("save_error_compose", "pt_templates")
            binding.addTemplateInputView.textInputLayout.isErrorEnabled = true
            binding.addTemplateInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            binding.addTemplateInputView.textInputLayout.requestFocus()
            return
        }

        setSaveButtonEnabled(false)

        // TODO: verify eventlogging
        if (viewModel.talkTemplateId != -1) {
            viewModel.talkTemplate?.let {
                PatrollerExperienceEvent.logAction("edit_message_save", "pt_templates")
                viewModel.updateTalkTemplate(title, subject, body, it)
            }
        } else {
            PatrollerExperienceEvent.logAction("save_message_click", "pt_templates")
            viewModel.saveTemplate(title, subject, body)
        }
    }

    private fun onSaveSuccess() {
        setSaveButtonEnabled(true)
        setResult(RESULT_OK)
        finish()
    }

    private fun onError(t: Throwable) {
        setSaveButtonEnabled(true)
        FeedbackUtil.showError(this, t)
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
            PageActivity.newIntentForCurrentTab(this, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, text = title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_REPLY_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    override fun onBackPressed() {
        PatrollerExperienceEvent.logAction("new_message_back", "pt_templates")
        if (!binding.addTemplateSubjectText.text.isNullOrEmpty() || binding.addTemplateInputView.editText.text.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.talk_new_topic_exit_dialog_title)
                .setMessage(R.string.talk_new_topic_exit_dialog_message)
                .setPositiveButton(R.string.edit_abandon_confirm_yes) { _, _ ->
                    PatrollerExperienceEvent.logAction("save_message_exit", "pt_templates")
                    super.onBackPressed() }
                .setNegativeButton(R.string.edit_abandon_confirm_no) { _, _ ->
                    PatrollerExperienceEvent.logAction("save_message_exit_cancel", "pt_templates")
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserMentionListUpdate() {
        binding.addTemplateScrollContainer.post {
            if (!isDestroyed && !userMentionScrolled) {
                binding.addTemplateScrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        userMentionScrolled = false
    }

    companion object {
        const val EXTRA_TEMPLATE_ID = "templateId"

        fun newIntent(context: Context,
                      templateId: Int = -1): Intent {
            return Intent(context, AddTemplateActivity::class.java)
                .putExtra(EXTRA_TEMPLATE_ID, templateId)
        }
    }
}
