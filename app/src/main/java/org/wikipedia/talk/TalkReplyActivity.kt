package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.talk.db.TalkTemplate
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
    private lateinit var messagePreviewFragment: EditPreviewFragment

    val viewModel: TalkReplyViewModel by viewModels { TalkReplyViewModel.Factory(intent.extras!!) }
    private var userMentionScrolled = false
    private var shouldWatchText = true
    private var subjectOrBodyModified = false
    private var savedSuccess = false

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val licenseTextMovementMethod = LinkMovementMethodExt { url: String ->
        if (url == "https://#login") {
            val loginIntent = LoginActivity.newIntent(this, LoginActivity.SOURCE_EDIT)
            requestLogin.launch(loginIntent)
        } else {
            UriUtil.handleExternalLink(this, Uri.parse(url))
        }
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            updateEditLicenseText()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
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

        textWatcher = binding.replySubjectText.doOnTextChanged { text, _, _, _ ->
            if (!shouldWatchText) {
                return@doOnTextChanged
            }
            subjectOrBodyModified = true
            binding.replySubjectLayout.error = null
            binding.replyInputView.textInputLayout.error = null
            setSaveButtonEnabled(binding.replyInputView.editText.text.isNotBlank())
            viewModel.talkTemplatesList.filter { it.subject == text.toString() }.let {
                if (viewModel.selectedTemplate == null && it.isNotEmpty()) {
                    binding.replySubjectLayout.error = getString(R.string.talk_subject_duplicate)
                    setSaveButtonEnabled(false)
                }
            }
        }

        binding.replyInputView.editText.addTextChangedListener(textWatcher)

        binding.replyNextButton.setOnClickListener {
            onGoNext()
        }

        binding.learnMoreButton.setOnClickListener {
            sendPatrollerExperienceEvent("learn_click", "pt_warning_messages")
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.talk_warn_learn_more_url)))
        }

        if (viewModel.isFromDiff) {
            binding.replyNextButton.text = getString(if (viewModel.templateManagementMode) R.string.talk_templates_new_message_save else R.string.edit_next)
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
                if (!viewModel.templateManagementMode) {
                    showEditPreview()
                } else {
                    setResult(RESULT_OK)
                    finish()
                }
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        viewModel.selectedTemplate?.let {
            binding.root.post {
                shouldWatchText = false
                binding.replySubjectText.setText(it.subject)
                binding.replyInputView.editText.setText(it.message)
                shouldWatchText = true
                setSaveButtonEnabled(true)
            }
        }

        SyntaxHighlightViewAdapter(this, viewModel.pageTitle, binding.root, binding.replyInputView.editText,
            binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
            Constants.InvokeSource.TALK_REPLY_ACTIVITY, requestInsertMedia, showUserMention = true, isFromDiff = viewModel.isFromDiff)

        messagePreviewFragment = supportFragmentManager.findFragmentById(R.id.message_preview_fragment) as EditPreviewFragment

        onInitialLoad()
    }

    override fun onResume() {
        super.onResume()
        if (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount) {
            binding.footerContainer.tempAccountInfoContainer.isVisible = true
            binding.footerContainer.tempAccountInfoIcon.setImageResource(if (AccountUtil.isTemporaryAccount) R.drawable.ic_temp_account else R.drawable.ic_anon_account)
            binding.footerContainer.tempAccountInfoText.movementMethod = LinkMovementMethod.getInstance()
            binding.footerContainer.tempAccountInfoText.text = StringUtil.fromHtml(if (AccountUtil.isTemporaryAccount) getString(R.string.temp_account_edit_status, AccountUtil.getTempAccountName(), getString(R.string.temp_accounts_help_url))
            else getString(R.string.temp_account_anon_edit_status, getString(R.string.temp_accounts_help_url)))
        } else {
            binding.footerContainer.tempAccountInfoContainer.isVisible = false
        }
        maybeShowTempAccountDialog()
        setToolbarTitle(viewModel.pageTitle)
        updateEditLicenseText()
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
        L10nUtil.setConditionalLayoutDirection(binding.talkScrollContainer, viewModel.pageTitle.wikiSite.languageCode)
        binding.learnMoreButton.isVisible = viewModel.isFromDiff
        if (viewModel.topic != null) {
            binding.replyInputView.userNameHints = setOf(viewModel.topic!!.author)
        }

        val savedReplyText = if (viewModel.topic == null) null else draftReplies.get(viewModel.topic?.id)
        if (!savedReplyText.isNullOrEmpty()) {
            binding.replyInputView.editText.setText(savedReplyText)
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }

        binding.progressBar.isVisible = false
        shouldWatchText = false
        binding.replySubjectText.setText(intent.getCharSequenceExtra(EXTRA_SUBJECT))
        if (intent.hasExtra(EXTRA_BODY) && binding.replyInputView.editText.text.isEmpty()) {
            binding.replyInputView.editText.setText(intent.getCharSequenceExtra(EXTRA_BODY))
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }
        shouldWatchText = true
        EditAttemptStepEvent.logInit(viewModel.pageTitle)

        setSaveButtonEnabled(binding.replyInputView.editText.text.isNotEmpty())

        if (viewModel.isNewTopic || viewModel.isFromDiff) {
            if (viewModel.isNewTopic) {
                title = getString(R.string.talk_new_topic)
            }
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
            binding.replySubjectLayout.isVisible = true
            binding.replySubjectLayout.requestFocus()
        } else {
            binding.replySubjectLayout.isVisible = false
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_reply_hint)
            binding.talkScrollContainer.fullScroll(View.FOCUS_DOWN)
            binding.replyInputView.maybePrepopulateUserName(AccountUtil.userName, viewModel.pageTitle)
            binding.talkScrollContainer.post {
                if (!isDestroyed) {
                    binding.replyInputView.editText.requestFocus()
                    DeviceUtil.showSoftKeyboard(binding.replyInputView.editText)
                    binding.talkScrollContainer.postDelayed({
                        if (!isDestroyed) {
                            binding.talkScrollContainer.smoothScrollTo(0, binding.talkScrollContainer.height * 4)
                        }
                    }, 500)
                }
            }
        }
        if (viewModel.templateManagementMode) {
            supportActionBar?.title = if (viewModel.selectedTemplate == null) getString(R.string.talk_templates_new_message_title) else getString(R.string.talk_templates_edit_message_dialog_title)
        }
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        val title = if (viewModel.templateManagementMode) getString(R.string.talk_warn_saved_messages) else
        StringUtil.fromHtml(if (viewModel.isNewTopic || viewModel.isFromDiff) pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } + ": " + "<a href='#'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>"
            else intent.getStringExtra(EXTRA_PARENT_SUBJECT).orEmpty()
        ).trim().ifEmpty { getString(R.string.talk_no_subject) }
        ViewUtil.getTitleViewFromToolbar(binding.replyToolbar)?.let {
            it.movementMethod = LinkMovementMethodExt { _ ->
                val entry = HistoryEntry(TalkTopicsActivity.getNonTalkPageTitle(pageTitle), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
            }
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
        binding.replyNextButton.isEnabled = enabled
        binding.replyNextButton.setTextColor(ResourceUtil
            .getThemedColor(this, if (enabled) R.attr.progressive_color else R.attr.inactive_color))
    }

    private fun showSaveDialog(subject: String, body: String) {
        TalkTemplatesTextInputDialog(this@TalkReplyActivity, R.string.talk_templates_new_message_dialog_save,
            R.string.talk_warn_save_dialog_dont_save,
            !viewModel.isExampleTemplate && viewModel.selectedTemplate != null).let { textInputDialog ->
            textInputDialog.callback = object : TalkTemplatesTextInputDialog.Callback {

                override fun onSuccess(subjectText: String) {
                    if (textInputDialog.isSaveAsNewChecked) {
                        viewModel.saveTemplate("", subjectText, body)
                    } else if (textInputDialog.isSaveExistingChecked) {
                        viewModel.selectedTemplate?.let {
                            viewModel.updateTemplate(it.title, subject, body, it)
                        }
                    } else {
                        showEditPreview()
                    }
                    val messageType = if (textInputDialog.isSaveAsNewChecked) "new" else "updated"
                    sendPatrollerExperienceEvent("save_message_success", "pt_warning_messages", PatrollerExperienceEvent.getActionDataString(messageType = messageType))
                }

                override fun onCancel() {
                    sendPatrollerExperienceEvent("publish_cancel", "pt_warning_messages")
                    showEditPreview()
                }

                override fun onTextChanged(text: String, dialog: TalkTemplatesTextInputDialog) {
                    if (textInputDialog.isSaveExistingChecked) {
                        dialog.setError(null)
                        dialog.setPositiveButtonEnabled(true)
                        return
                    }
                    text.trim().let {
                        when {
                            it.isEmpty() -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(false)
                            }

                            viewModel.talkTemplatesList.any { item -> item.subject == it } -> {
                                if (textInputDialog.isSaveExistingChecked) {
                                    return
                                }
                                dialog.setError(dialog.context.getString(R.string.talk_subject_duplicate))
                                dialog.setPositiveButtonEnabled(false)
                            }

                            else -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(true)
                            }
                        }
                    }
                }

                override fun onDismiss() {
                    setSaveButtonEnabled(true)
                }

                override fun getSubjectText(): String {
                    return subject
                }
            }
            textInputDialog.setTitle(R.string.talk_warn_save_dialog_title)
        }.show()
    }

    private fun onGoNext() {
        val subject = binding.replySubjectText.text.toString().trim()
        val body = binding.replyInputView.editText.text.toString().trim()
        Intent().let {
            it.putExtra(EXTRA_SUBJECT, subject)
            it.putExtra(EXTRA_BODY, body)
        }

        if (messagePreviewFragment.isActive) {
            EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle)
            sendPatrollerExperienceEvent("publish_message_click", "pt_warning_messages")
            binding.progressBar.isVisible = true
            setSaveButtonEnabled(false)
            viewModel.postReply(subject, getWikitextBody())
            return
        }

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

        if (viewModel.isFromDiff && subjectOrBodyModified) {
            setSaveButtonEnabled(false)
            DeviceUtil.hideSoftKeyboard(this)
            if (viewModel.templateManagementMode) {
                if (viewModel.selectedTemplate != null && !viewModel.isExampleTemplate) {
                    viewModel.selectedTemplate?.let {
                        viewModel.updateTemplate(it.title, subject, body, it)
                    }
                } else {
                    viewModel.saveTemplate("", subject, body)
                }
            } else {
                if (viewModel.selectedTemplate != null && viewModel.selectedTemplate?.subject == subject &&
                    viewModel.selectedTemplate?.message == body) {
                    sendPatrollerExperienceEvent("message_review_next_click", "pt_warning_messages")
                    showEditPreview()
                } else {
                    showSaveDialog(subject, body)
                }
            }
        } else {
            sendPatrollerExperienceEvent("message_review_next_click", "pt_warning_messages")
            showEditPreview()
            setSaveButtonEnabled(true)
        }
    }

    private fun showEditPreview() {
        DeviceUtil.hideSoftKeyboard(this)
        binding.talkScrollContainer.isVisible = false
        updateEditLicenseText()
        setSaveButtonEnabled(true)
        supportActionBar?.title = getString(R.string.edit_preview)
        binding.replyNextButton.text = getString(R.string.description_edit_save)
        messagePreviewFragment.showPreview(viewModel.pageTitle, getWikitextForPreview())
        EditAttemptStepEvent.logSaveIntent(viewModel.pageTitle)
    }

    private fun getWikitextForPreview(): String {
        val subject = binding.replySubjectText.text.toString().trim()
        val body = getWikitextBody()
        return if (subject.isNotEmpty()) "==$subject==\n$body" else body
    }

    private fun getWikitextBody(): String {
        return binding.replyInputView.editText.text.toString().trim()
            .replace(getString(R.string.username_wikitext), getString(R.string.wikiText_replace_url, viewModel.pageTitle.prefixedText, "@" + StringUtil.removeNamespace(viewModel.pageTitle.prefixedText)))
            .replace(getString(R.string.sender_username_wikitext), AccountUtil.userName.orEmpty())
            .replace(getString(R.string.diff_link_wikitext), viewModel.pageTitle.getWebApiUrl("diff=${viewModel.toRevisionId}&oldid=${viewModel.fromRevisionId}&variant=${viewModel.pageTitle.wikiSite.languageCode}"))
    }

    private fun onSaveSuccess(newRevision: Long) {
        AnonymousNotificationHelper.onEditSubmitted()

        sendPatrollerExperienceEvent("publish_message_success", "pt_warning_messages",
            PatrollerExperienceEvent.getPublishMessageActionString(isModified = viewModel.selectedTemplate != null && subjectOrBodyModified,
                isSaved = viewModel.talkTemplateSaved, isExample = viewModel.isExampleTemplate, exampleMessage = if (viewModel.isExampleTemplate) viewModel.selectedTemplate?.title else null))

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

    private fun updateEditLicenseText() {
        val text = StringUtil.fromHtml(getString(if (AccountUtil.isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_4_url)))
        messagePreviewFragment.view?.findViewById<TextView>(R.id.licenseText)?.apply {
            this.text = text
            this.movementMethod = licenseTextMovementMethod
        }
    }

    private fun maybeShowTempAccountDialog(force: Boolean = false): Boolean {
        if (force || (!Prefs.tempAccountDialogShown && (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount))) {
            MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon_NegativeInactive)
                .setIcon(if (AccountUtil.isTemporaryAccount) R.drawable.ic_temp_account else R.drawable.ic_anon_account)
                .setTitle(if (AccountUtil.isTemporaryAccount) R.string.temp_account_using_title else R.string.temp_account_not_logged_in)
                .setMessage(StringUtil.fromHtml(if (AccountUtil.isTemporaryAccount) getString(R.string.temp_account_temp_dialog_body, AccountUtil.userName) else getString(R.string.temp_account_anon_dialog_body)))
                .setPositiveButton(getString(R.string.temp_account_dialog_ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.create_account_login)) { dialog, _ ->
                    dialog.dismiss()
                    val loginIntent = LoginActivity.newIntent(this, LoginActivity.SOURCE_EDIT)
                    requestLogin.launch(loginIntent)
                }
                .show()
            Prefs.tempAccountDialogShown = true
            return true
        }
        return false
    }

    override fun onBackPressed() {
        setResult(RESULT_BACK_FROM_TOPIC)
        sendPatrollerExperienceEvent("publish_back", "pt_warning_messages")
        if (messagePreviewFragment.isActive) {
            showProgressBar(false)
            binding.talkScrollContainer.isVisible = true
            messagePreviewFragment.hide()
            setSaveButtonEnabled(true)
            binding.replyNextButton.text = getString(R.string.edit_next)
            setToolbarTitle(viewModel.pageTitle)
        } else if (subjectOrBodyModified) {
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

    override fun isNewPage(): Boolean {
        return !viewModel.doesPageExist
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
        const val TO_REVISION_ID = "toRevisionId"
        const val FROM_REVISION_ID = "fromRevisionId"
        const val EXTRA_SELECTED_TEMPLATE = "selectedTemplate"
        const val EXTRA_TEMPLATE_MANAGEMENT = "templateManagement"
        const val EXTRA_EXAMPLE_TEMPLATE = "exampleTemplate"

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
                      selectedTemplate: TalkTemplate? = null,
                      toRevisionId: Long = -1,
                      fromRevisionId: Long = -1,
                      templateManagementMode: Boolean = false,
                      isExampleTemplate: Boolean = false
        ): Intent {
            return Intent(context, TalkReplyActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(EXTRA_PARENT_SUBJECT, parentSubject)
                    .putExtra(EXTRA_TOPIC, topic)
                    .putExtra(EXTRA_SUBJECT, undoSubject)
                    .putExtra(EXTRA_BODY, undoBody)
                    .putExtra(EXTRA_FROM_DIFF, fromDiff)
                    .putExtra(EXTRA_SELECTED_TEMPLATE, selectedTemplate)
                    .putExtra(EXTRA_TEMPLATE_MANAGEMENT, templateManagementMode)
                    .putExtra(EXTRA_EXAMPLE_TEMPLATE, isExampleTemplate)
                    .putExtra(FROM_REVISION_ID, fromRevisionId)
                    .putExtra(TO_REVISION_ID, toRevisionId)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
