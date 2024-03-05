package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.util.lruCache
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityTalkReplyBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.edit.SyntaxHighlightViewAdapter
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.insertmedia.InsertMediaViewModel
import org.wikipedia.edit.preview.EditPreviewFragment
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.talk.template.TalkTemplatesActivity.Companion.EXTRA_TEMPLATE_ID
import org.wikipedia.talk.template.TalkTemplatesActivity.Companion.EXTRA_TEMPLATE_MANAGEMENT
import org.wikipedia.talk.template.TalkTemplatesTextInputDialog
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.UserMentionInputView
import org.wikipedia.views.ViewUtil

class TalkReplyActivity : BaseActivity(), UserMentionInputView.Listener, EditPreviewFragment.Callback {
    private lateinit var binding: ActivityTalkReplyBinding
    private lateinit var linkHandler: TalkLinkHandler
    private lateinit var textWatcher: TextWatcher
    private lateinit var editPreviewFragment: EditPreviewFragment

    val viewModel: TalkReplyViewModel by viewModels { TalkReplyViewModel.Factory(intent.extras!!) }
    private var userMentionScrolled = false
    private var savedSuccess = false

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val requestInsertMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == InsertMediaActivity.RESULT_INSERT_MEDIA_SUCCESS) {
            it.data?.let { data ->
                val imageTitle = data.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
                val imageCaption = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION)
                val imageAlt = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT)
                val imageSize = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_SIZE)
                val imageType = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_TYPE)
                val imagePos = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_POS)

                val newWikiText = InsertMediaViewModel.insertImageIntoWikiText(viewModel.pageTitle.wikiSite.languageCode,
                    binding.replyInputView.editText.text.toString(), imageTitle?.text.orEmpty(), imageCaption.orEmpty(),
                    imageAlt.orEmpty(), imageSize.orEmpty(), imageType.orEmpty(), imagePos.orEmpty(),
                    binding.replyInputView.editText.selectionStart, false, false)

                binding.replyInputView.editText.setText(newWikiText.first)

                val insertPos = newWikiText.third
                binding.replyInputView.editText.setSelection(insertPos.first, insertPos.first + insertPos.second)
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkReplyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.replyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        linkHandler = TalkLinkHandler(this)
        linkHandler.wikiSite = viewModel.pageTitle.wikiSite

        textWatcher = binding.replySubjectText.doOnTextChanged { _, _, _, _ ->
            binding.replySubjectLayout.error = null
            binding.replyInputView.textInputLayout.error = null
            setSaveButtonEnabled(binding.replyInputView.editText.text.isNotBlank())
        }
        binding.replyInputView.editText.addTextChangedListener(textWatcher)

        binding.replySaveButton.setOnClickListener {
            onSaveClicked()
        }

        binding.learnLinkContainer.setOnClickListener {
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.create_account_ip_block_help_url)))
        }

        if (viewModel.isFromDiff) {
            binding.replySaveButton.text = getString(if (viewModel.templateManagementMode) R.string.talk_templates_new_message_save else R.string.edit_next)
        }

        binding.replyInputView.wikiSite = viewModel.pageTitle.wikiSite
        binding.replyInputView.listener = this

        if (viewModel.topic != null) {
            binding.threadItemView.bindItem(viewModel.topic!!, linkMovementMethod, true)
            binding.threadItemView.isVisible = true
        } else {
            binding.threadItemView.isVisible = false
        }

        viewModel.postReplyData.observe(this) {
            if (it is Resource.Success) {
                savedSuccess = true
                onSaveSuccess(it.data)
            } else if (it is Resource.Error) {
                onSaveError(it.throwable)
            }
        }

        viewModel.saveTemplateData.observe(this) {
            if (it is Resource.Success) {
                viewModel.talkTemplateSaved = true
                binding.progressBar.isVisible = true
                viewModel.postReply(it.data.subject, it.data.message)
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        viewModel.savedTemplateData.observe(this) {
            if (it is Resource.Success && viewModel.isFromDiff && viewModel.templateId != -1) {
                binding.replySubjectText.setText(it.data?.subject)
                binding.replyInputView.editText.setText(it.data?.message)
            }
        }

        SyntaxHighlightViewAdapter(this, viewModel.pageTitle, binding.root, binding.replyInputView.editText,
            binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
            Constants.InvokeSource.TALK_REPLY_ACTIVITY, requestInsertMedia, true)

        editPreviewFragment = supportFragmentManager.findFragmentById(R.id.edit_section_preview_fragment) as EditPreviewFragment

        onInitialLoad()
    }

    public override fun onDestroy() {
        if (!savedSuccess && binding.replyInputView.editText.text.isNotBlank() && viewModel.topic != null) {
            draftReplies.put(viewModel.topic!!.id, binding.replyInputView.editText.text)
        }
        binding.replySubjectText.removeTextChangedListener(textWatcher)
        binding.replyInputView.editText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        setSaveButtonEnabled(false)
        setToolbarTitle(viewModel.pageTitle)
        L10nUtil.setConditionalLayoutDirection(binding.talkScrollContainer, viewModel.pageTitle.wikiSite.languageCode)

        if (viewModel.topic != null) {
            binding.replyInputView.userNameHints = setOf(viewModel.topic!!.author)
        }

        val savedReplyText = if (viewModel.topic == null) null else draftReplies.get(viewModel.topic?.id)
        if (!savedReplyText.isNullOrEmpty()) {
            binding.replyInputView.editText.setText(savedReplyText)
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }

        binding.progressBar.isVisible = false
        binding.replySubjectText.setText(intent.getCharSequenceExtra(EXTRA_SUBJECT))
        if (intent.hasExtra(EXTRA_BODY) && binding.replyInputView.editText.text.isEmpty()) {
            binding.replyInputView.editText.setText(intent.getCharSequenceExtra(EXTRA_BODY))
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }
        EditAttemptStepEvent.logInit(viewModel.pageTitle)

        if (viewModel.isNewTopic) {
            title = getString(R.string.talk_new_topic)
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
            binding.replySubjectLayout.isVisible = true
            binding.replySubjectLayout.requestFocus()
        } else {
            binding.replySubjectLayout.isVisible = false
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_reply_hint)
            binding.talkScrollContainer.fullScroll(View.FOCUS_DOWN)
            binding.replyInputView.maybePrepopulateUserName(AccountUtil.userName.orEmpty(), viewModel.pageTitle)
            binding.talkScrollContainer.post {
                if (!isDestroyed) {
                    binding.replyInputView.editText.requestFocus()
                    DeviceUtil.showSoftKeyboard(binding.replyInputView.editText)
                    binding.talkScrollContainer.postDelayed({
                        binding.talkScrollContainer.smoothScrollTo(0, binding.talkScrollContainer.height * 4)
                    }, 500)
                }
            }
        }
        if (viewModel.templateManagementMode) {
            supportActionBar?.title = if (viewModel.templateId == -1) getString(R.string.talk_templates_new_message_title) else getString(R.string.talk_templates_edit_message_dialog_title)
        }
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        val title = StringUtil.fromHtml(
            if (viewModel.isNewTopic) pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } + ": " + "<a href='#'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>"
            else intent.getStringExtra(EXTRA_PARENT_SUBJECT).orEmpty()
        ).trim().ifEmpty { getString(R.string.talk_no_subject) }
        ViewUtil.getTitleViewFromToolbar(binding.replyToolbar)?.let {
            it.contentDescription = title
            it.movementMethod = LinkMovementMethodExt { _ ->
                val entry = HistoryEntry(TalkTopicsActivity.getNonTalkPageTitle(pageTitle), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
            }
            FeedbackUtil.setButtonLongPressToast(it)
        }
        supportActionBar?.title = title
    }

    internal inner class TalkLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        private var lastX: Int = 0
        private var lastY: Int = 0

        fun onUrlClick(url: String, title: String?, linkText: String, x: Int, y: Int) {
            lastX = x
            lastY = y
            super.onUrlClick(url, title, linkText)
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            startActivity(FilePageActivity.newIntent(this@TalkReplyActivity, title))
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@TalkReplyActivity, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_REPLY_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.replySaveButton.isEnabled = enabled
        binding.replySaveButton.setTextColor(ResourceUtil
            .getThemedColor(this, if (enabled) R.attr.progressive_color else R.attr.inactive_color))
    }

    private fun showSaveDialog(subject: String, body: String) {
        TalkTemplatesTextInputDialog(this@TalkReplyActivity, R.string.talk_templates_new_message_dialog_save,
            R.string.talk_warn_save_dialog_cancel).let { textInputDialog ->
            textInputDialog.callback = object : TalkTemplatesTextInputDialog.Callback {
                override fun onShow(dialog: TalkTemplatesTextInputDialog) {
                    dialog.setTitleHint(R.string.talk_warn_save_dialog_hint)
                    dialog.setPositiveButtonEnabled(true)
                }

                override fun onTextChanged(text: CharSequence, dialog: TalkTemplatesTextInputDialog) {
                    text.toString().trim().let {
                        when {
                            it.isEmpty() -> {
                                if (textInputDialog.isSaveExistingChecked) {
                                    return
                                }
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(false)
                            }

                            viewModel.talkTemplatesList.any { item -> item.title == it } -> {
                                dialog.setError(
                                    dialog.context.getString(
                                        R.string.talk_templates_new_message_dialog_exists,
                                        it
                                    )
                                )
                                dialog.setPositiveButtonEnabled(false)
                            }

                            else -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(true)
                            }
                        }
                    }
                }

                override fun onSuccess(titleText: CharSequence, subjectText: CharSequence, bodyText: CharSequence) {
                    if (textInputDialog.isSaveAsNewChecked) {
                        viewModel.saveTemplate(titleText.toString(), subject, body)
                    } else if (textInputDialog.isSaveExistingChecked) {
                        viewModel.selectedTemplate?.let {
                            viewModel.updateTemplate(it.title, subject, body, it)
                        }
                    }
                    showEditPreview()
                    val messageType = if (textInputDialog.isSaveAsNewChecked) "new" else if (textInputDialog.isSaveExistingChecked) "updated" else ""
                    sendPatrollerExperienceEvent("publish_message_click", "pt_warning_messages", PatrollerExperienceEvent.getActionDataString(messageType = messageType))
                }

                override fun onCancel() {
                    sendPatrollerExperienceEvent("publish_cancel", "pt_warning_messages")
                    setSaveButtonEnabled(true)
                }

                override fun onDismiss() {
                    setSaveButtonEnabled(true)
                }
            }
            textInputDialog.showDialogMessage(false)
            textInputDialog.showTemplateCheckboxes(viewModel.selectedTemplate != null)
            textInputDialog.setTitle(R.string.talk_warn_save_dialog_title)
        }.show()
    }

    private fun showEditPreview() {
        binding.talkScrollContainer.isVisible = false
        binding.editSectionPreviewFragment.isVisible = true
        updateEditLicenseText()
        setSaveButtonEnabled(true)
        supportActionBar?.title = getString(R.string.edit_preview)
        binding.replySaveButton.text = getString(R.string.description_edit_save)
        // editPreviewFragment.showPreview(viewModel.pageTitle, "Hello @"+AccountUtil.userName+", I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia;I appreciate your collaboration on Wikipedia; However, I noticed you are in a [[Wikipedia:Conflict_of_interest|conflict of interest]]. A conflict of interest is the incompatibility between Wikipedia\\'s objectives of neutrality and reliability and the particular objectives of certain editors, individuals, entities, or companies of any type.\\nAll contributions in the main space are subject to the policies of content criteria ([[Wikipedia:What_Wikipedia_is_not|what Wikipedia is not]]), encyclopedic quality ([[Wikipedia:Verifiability|verifiability]] and [[Wikipedia:No_original_research|no original research]]), editorial method ([[Wikipedia:Neutral_point_of_view|neutral point of view]]), and legitimacy of content ([[Wikipedia:Copyrights|copyrights]]). All publishers are expected to abide by these policies when creating and evaluating content and to respect and assume good faith in the actions of other publishers to ensure that these policies are followed.\\n\\nIf you edit under a conflict of interest, you must apply the corresponding policy with special care; Otherwise, your user account could be considered private purpose and blocked. Feel free to reach out on my talk page if you have any questions.")
        editPreviewFragment.showPreview(viewModel.pageTitle, binding.replyInputView.editText.text.toString())
    }

    private fun updateEditLicenseText() {
        val editLicenseText = ActivityCompat.requireViewById<TextView>(this, R.id.licenseText)
        editLicenseText.text = StringUtil.fromHtml(getString(R.string.edit_save_action_license_logged_in,
            getString(R.string.terms_of_use_url), getString(R.string.cc_by_sa_4_url)))
        editLicenseText.movementMethod = LinkMovementMethodExt { url: String ->
            UriUtil.handleExternalLink(this@TalkReplyActivity, url.toUri())
        }
    }

    private fun onSaveClicked() {
        val subject = binding.replySubjectText.text.toString().trim()
        val body = binding.replyInputView.editText.text.toString().trim()
        Intent().let {
            it.putExtra(EXTRA_SUBJECT, subject)
            it.putExtra(EXTRA_BODY, body)
        }
        if (editPreviewFragment.isActive) {
            binding.progressBar.visibility = View.VISIBLE
            binding.editSectionPreviewFragment.isVisible = false
            // viewModel.postReply(subject, body)
            onSaveSuccess(-1)
            return
        }
        EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle)

        if (viewModel.isNewTopic && subject.isEmpty()) {
            sendPatrollerExperienceEvent("publish_error_subject", "pt_warning_messages")
            binding.replySubjectLayout.error = getString(R.string.talk_subject_empty)
            binding.replySubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            binding.replyInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            binding.replyInputView.textInputLayout.requestFocus()
            return
        }

        setSaveButtonEnabled(false)

        if (viewModel.isFromDiff) {
            sendPatrollerExperienceEvent("publish_saved_message_click", "pt_warning_messages")
            DeviceUtil.hideSoftKeyboard(this)
            if (viewModel.templateManagementMode) {
                if (viewModel.templateId != -1) {
                    viewModel.selectedTemplate?.let {
                        viewModel.updateTemplate(it.title, subject, body, it)
                    }
                } else {
                    viewModel.saveTemplate("", subject, body)
                }
                setResult(RESULT_OK)
                finish()
            } else {
                showSaveDialog(subject, body)
            }
        } else {
            showEditPreview()
        }
    }

    private fun onSaveSuccess(newRevision: Long) {
        AnonymousNotificationHelper.onEditSubmitted()

        binding.progressBar.visibility = View.GONE
        setSaveButtonEnabled(true)
        EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle)

        Intent().let {
            it.putExtra(RESULT_NEW_REVISION_ID, newRevision)
            it.putExtra(EXTRA_SUBJECT, binding.replySubjectText.text)
            it.putExtra(EXTRA_BODY, binding.replyInputView.editText.text)
            if (viewModel.topic != null) {
                it.putExtra(EXTRA_TOPIC_ID, viewModel.topic!!.id)
            }
            setResult(if (viewModel.talkTemplateSaved) RESULT_SAVE_TEMPLATE else RESULT_EDIT_SUCCESS, it)

            if (viewModel.topic != null) {
                draftReplies.remove(viewModel.topic?.id)
            }
            finish()
        }
    }

    private fun onSaveError(t: Throwable) {
        EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle)
        binding.progressBar.visibility = View.GONE
        setSaveButtonEnabled(true)
        FeedbackUtil.showError(this, t)
    }

    override fun onBackPressed() {
        setResult(RESULT_BACK_FROM_TOPIC)
        sendPatrollerExperienceEvent("publish_back", "pt_warning_messages")
        if (viewModel.isNewTopic && (!binding.replySubjectText.text.isNullOrEmpty() ||
                    binding.replyInputView.editText.text.isNotEmpty())) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.talk_new_topic_exit_dialog_title)
                .setMessage(R.string.talk_new_topic_exit_dialog_message)
                .setPositiveButton(R.string.edit_abandon_confirm_yes) { _, _ ->
                    sendPatrollerExperienceEvent("publish_exit", "pt_warning_messages")
                    super.onBackPressed()
                }
                .setNegativeButton(R.string.edit_abandon_confirm_no) { _, _ ->
                    sendPatrollerExperienceEvent("publish_exit_cancel", "pt_warning_messages")
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserMentionListUpdate() {
        binding.talkScrollContainer.post {
            if (!isDestroyed && !userMentionScrolled) {
                binding.talkScrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        userMentionScrolled = false
    }

    private fun sendPatrollerExperienceEvent(action: String, activeInterface: String, actionData: String = "") {
        if (viewModel.isFromDiff) {
            PatrollerExperienceEvent.logAction(action, activeInterface, actionData)
        }
    }

    override fun getParentPageTitle(): PageTitle {
        return viewModel.pageTitle
    }

    override fun showProgressBar(visible: Boolean) {
        binding.progressBar.isVisible = visible
        invalidateOptionsMenu()
    }

    companion object {
        const val EXTRA_PARENT_SUBJECT = "parentSubject"
        const val EXTRA_TOPIC = "topic"
        const val EXTRA_TOPIC_ID = "topicId"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_BODY = "body"
        const val EXTRA_FROM_DIFF = "fromDiff"
        const val RESULT_EDIT_SUCCESS = 1
        const val RESULT_BACK_FROM_TOPIC = 2
        const val RESULT_SAVE_TEMPLATE = 3
        const val RESULT_NEW_REVISION_ID = "newRevisionId"

        // TODO: persist in db. But for now, it's fine to store these for the lifetime of the app.
        val draftReplies = lruCache<String, CharSequence>(10)

        fun newIntent(context: Context,
                      pageTitle: PageTitle,
                      parentSubject: String?,
                      topic: ThreadItem?,
                      invokeSource: Constants.InvokeSource,
                      undoSubject: CharSequence? = null,
                      undoBody: CharSequence? = null,
                      fromDiff: Boolean = false,
                      templateId: Int = -1,
                      templateManagementMode: Boolean = false
        ): Intent {
            return Intent(context, TalkReplyActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(EXTRA_PARENT_SUBJECT, parentSubject)
                    .putExtra(EXTRA_TOPIC, topic)
                    .putExtra(EXTRA_SUBJECT, undoSubject)
                    .putExtra(EXTRA_BODY, undoBody)
                    .putExtra(EXTRA_FROM_DIFF, fromDiff)
                    .putExtra(EXTRA_TEMPLATE_ID, templateId)
                    .putExtra(EXTRA_TEMPLATE_MANAGEMENT, templateManagementMode)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
