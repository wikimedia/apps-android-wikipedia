package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
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
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.talk.template.TalkTemplatesActivity
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

    private val viewModel: TalkReplyViewModel by viewModels { TalkReplyViewModel.Factory(intent.extras!!) }
    private var userMentionScrolled = false
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

    private val requestManageTalkTemplate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadTemplates()
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

        if (viewModel.isFromDiff) {
            binding.talkTemplateContainer.isVisible = true
            binding.talkTemplateButton.setOnClickListener {
                sendPatrollerExperienceEvent("saved_message_edit_click", "pt_warning_messages")
                requestManageTalkTemplate.launch(TalkTemplatesActivity.newIntent(this))
            }
            FeedbackUtil.setButtonTooltip(binding.talkTemplateButton)
        } else {
            binding.talkTemplateContainer.isVisible = false
        }

        linkHandler = TalkLinkHandler(this)
        linkHandler.wikiSite = viewModel.pageTitle.wikiSite

        textWatcher = binding.replySubjectText.doOnTextChanged { _, _, _, _ ->
            subjectOrBodyModified = true
            binding.replySubjectLayout.error = null
            binding.replyInputView.textInputLayout.error = null
            setSaveButtonEnabled(binding.replyInputView.editText.text.isNotBlank())
        }
        binding.replyInputView.editText.addTextChangedListener(textWatcher)

        binding.replyNextButton.setOnClickListener {
            onGoNext()
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

        viewModel.loadTemplateData.observe(this) {
            if (it is Resource.Success) {
                setTalkTemplateSpinnerAdapter()
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        viewModel.saveTemplateData.observe(this) {
            if (it is Resource.Success) {
                viewModel.talkTemplateSaved = true
                binding.progressBar.isVisible = true
                showPreview()
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        SyntaxHighlightViewAdapter(this, viewModel.pageTitle, binding.root, binding.replyInputView.editText,
            binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
            Constants.InvokeSource.TALK_REPLY_ACTIVITY, requestInsertMedia, true)

        messagePreviewFragment = supportFragmentManager.findFragmentById(R.id.message_preview_fragment) as EditPreviewFragment

        onInitialLoad()
    }

    override fun onResume() {
        super.onResume()
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
        setSaveButtonEnabled(false)
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
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        if (messagePreviewFragment.isActive) {
            supportActionBar?.title = getString(R.string.edit_preview)
            binding.replyNextButton.text = getString(R.string.description_edit_save)
            binding.replyNextButton.contentDescription = binding.replyNextButton.text
            return
        }
        binding.replyNextButton.text = getString(R.string.edit_next)
        binding.replyNextButton.contentDescription = binding.replyNextButton.text
        if (viewModel.isFromDiff) {
            supportActionBar?.title = getString(R.string.talk_warn)
            return
        }
        val title = StringUtil.fromHtml(
            if (viewModel.isNewTopic) pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } + ": " + "<a href='#'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>"
            else intent.getStringExtra(EXTRA_PARENT_SUBJECT).orEmpty()
        ).trim().ifEmpty { getString(R.string.talk_no_subject) }
        ViewUtil.getTitleViewFromToolbar(binding.replyToolbar)?.let {
            it.movementMethod = LinkMovementMethodExt { _ ->
                val entry = HistoryEntry(TalkTopicsActivity.getNonTalkPageTitle(pageTitle), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
            }
            FeedbackUtil.setButtonTooltip(it)
        }
        supportActionBar?.title = title
    }

    private fun setTalkTemplateSpinnerAdapter() {
        val talkTemplateMsgStringId: Int
        if (viewModel.talkTemplatesList.isEmpty()) {
            talkTemplateMsgStringId = R.string.talk_templates_new_message_description
            sendPatrollerExperienceEvent("first_message_init", "pt_warning_messages")
        } else {
            talkTemplateMsgStringId = R.string.talk_warn_saved_message
            sendPatrollerExperienceEvent("message_init", "pt_warning_messages")
        }
        binding.talkTemplateMessage.text = getString(talkTemplateMsgStringId)
        binding.talkTemplateSpinnerLayout.isVisible = viewModel.talkTemplatesList.isNotEmpty()
        L10nUtil.setConditionalTextDirection(binding.talkTemplateSpinner, viewModel.pageTitle.wikiSite.languageCode)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, viewModel.talkTemplatesList)
        binding.talkTemplateSpinner.setAdapter(adapter)
        binding.talkTemplateSpinner.setOnClickListener {
            DeviceUtil.hideSoftKeyboard(this)
        }
        binding.talkTemplateSpinner.setOnItemClickListener { _, _, position, _ ->
            sendPatrollerExperienceEvent("saved_message_select_click", "pt_warning_messages")
            viewModel.selectedTemplate = viewModel.talkTemplatesList[position]
            viewModel.selectedTemplate?.let { talkTemplate ->
                sendPatrollerExperienceEvent("saved_message_impression", "pt_warning_messages")
                binding.replySubjectText.setText(talkTemplate.subject)
                binding.replyInputView.editText.setText(talkTemplate.message)
                subjectOrBodyModified = false
            }
        }
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
        TalkTemplatesTextInputDialog(this@TalkReplyActivity, R.string.edit_preview,
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
                    } else {
                        showPreview()
                    }
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

    private fun onGoNext() {
        val subject = binding.replySubjectText.text.toString().trim()
        val body = binding.replyInputView.editText.text.toString().trim()
        Intent().let {
            it.putExtra(EXTRA_SUBJECT, subject)
            it.putExtra(EXTRA_BODY, body)
        }

        if (messagePreviewFragment.isActive) {
            EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle)

            binding.progressBar.isVisible = true
            setSaveButtonEnabled(false)
            viewModel.postReply(subject, body)
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
            sendPatrollerExperienceEvent("publish_saved_message_click", "pt_warning_messages")
            DeviceUtil.hideSoftKeyboard(this)
            showSaveDialog(subject, body)
        } else {
            setSaveButtonEnabled(true)
            showPreview()
        }
    }

    private fun showPreview() {
        DeviceUtil.hideSoftKeyboard(this)
        val subject = binding.replySubjectText.text.toString().trim()
        val body = binding.replyInputView.editText.text.toString().trim()

        var wikitext = body
        if (!binding.replySubjectText.text.isNullOrEmpty()) {
            wikitext = "==$subject==\n$wikitext"
        }

        EditAttemptStepEvent.logSaveIntent(viewModel.pageTitle)
        messagePreviewFragment.showPreview(viewModel.pageTitle, wikitext)
        setToolbarTitle(viewModel.pageTitle)
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

    private fun updateEditLicenseText() {
        val text = StringUtil.fromHtml(getString(if (AccountUtil.isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_4_url)))
        binding.licenseText.text = text
        binding.licenseText.movementMethod = licenseTextMovementMethod
        messagePreviewFragment.view?.findViewById<TextView>(R.id.licenseText)?.apply {
            this.text = text
            this.movementMethod = licenseTextMovementMethod
        }
    }

    override fun onBackPressed() {
        if (messagePreviewFragment.isActive) {
            messagePreviewFragment.hide()
            setToolbarTitle(viewModel.pageTitle)
            return
        }
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
        binding.licenseText.isVisible = false
        binding.talkScrollContainer.post {
            if (!isDestroyed && !userMentionScrolled) {
                binding.talkScrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        userMentionScrolled = false
        binding.licenseText.isVisible = true
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
                      fromDiff: Boolean = false): Intent {
            return Intent(context, TalkReplyActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(EXTRA_PARENT_SUBJECT, parentSubject)
                    .putExtra(EXTRA_TOPIC, topic)
                    .putExtra(EXTRA_SUBJECT, undoSubject)
                    .putExtra(EXTRA_BODY, undoBody)
                    .putExtra(EXTRA_FROM_DIFF, fromDiff)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
