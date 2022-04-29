package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTalkReplyBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.util.*
import org.wikipedia.views.UserMentionInputView

class TalkReplyActivity : BaseActivity(), LinkPreviewDialog.Callback, UserMentionInputView.Listener {
    private lateinit var binding: ActivityTalkReplyBinding
    private lateinit var editFunnel: EditFunnel
    private lateinit var linkHandler: TalkLinkHandler
    private lateinit var textWatcher: TextWatcher

    private val viewModel: TalkReplyViewModel by viewModels { TalkReplyViewModel.Factory(intent.extras!!) }
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var userMentionScrolled = false

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            updateEditLicenseText()
            editFunnel.logLoginSuccess()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        } else {
            editFunnel.logLoginFailure()
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
        }

        binding.replySaveButton.setOnClickListener {
            onSaveClicked()
        }

        binding.replyInputView.wikiSite = viewModel.pageTitle.wikiSite
        binding.replyInputView.listener = this

        editFunnel = EditFunnel(WikipediaApp.getInstance(), viewModel.pageTitle)
        editFunnel.logStart()
        EditAttemptStepEvent.logInit(viewModel.pageTitle)

        if (viewModel.topic != null) {
            binding.threadItemView.bindItem(viewModel.topic!!, linkMovementMethod, true)
            binding.threadItemView.isVisible = true
        } else {
            binding.threadItemView.isVisible = false
        }

        viewModel.postReplyData.observe(this) {
            if (it is Resource.Success) {
                onSaveSuccess(it.data)
            } else if (it is Resource.Error) {
                onSaveError(it.throwable)
            }
        }

        onInitialLoad()
    }

    public override fun onDestroy() {

        // TODO
        // binding.replySubjectText.removeTextChangedListener(textWatcher)
        // binding.replyInputView.editText.removeTextChangedListener(textWatcher)

        super.onDestroy()
    }

    private fun onInitialLoad() {
        updateEditLicenseText()
        binding.progressBar.isVisible = false
        binding.replySubjectText.setText(intent.getStringExtra(EXTRA_SUBJECT).orEmpty())
        binding.replyInputView.editText.setText(intent.getStringExtra(EXTRA_BODY).orEmpty())
        if (intent.hasExtra(EXTRA_BODY)) {
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }
        editFunnel.logStart()
        EditAttemptStepEvent.logInit(viewModel.pageTitle)

        if (viewModel.isNewTopic) {
            title = getString(R.string.talk_new_topic)
            binding.talkToolbarSubjectView.visibility = View.INVISIBLE
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
            binding.replySubjectText.isVisible = true
            binding.replySubjectLayout.requestFocus()

        } else {
            binding.replySubjectText.isVisible = false
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_reply_hint)
            binding.talkScrollContainer.fullScroll(View.FOCUS_DOWN)
            binding.replyInputView.maybePrepopulateUserName()
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

    private fun updateOnSuccess(threadItems: List<ThreadItem>) {
        binding.progressBar.visibility = View.GONE

        // TODO: Discuss this
        // currentRevision = talkTopic.revision

        // TODO:
        // binding.replyInputView.userNameHints = parseUserNamesFromTopic()
        // maybeShowUndoSnackbar()
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.visibility = View.GONE
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
            // TODO
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@TalkReplyActivity, bottomSheetPresenter, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun onSaveClicked() {
        val subject = binding.replySubjectText.text.toString().trim()
        val body = binding.replyInputView.editText.getParsedText(viewModel.pageTitle.wikiSite).trim()
        Intent().let {
            it.putExtra(EXTRA_SUBJECT, subject)
            it.putExtra(EXTRA_BODY, body)
        }

        editFunnel.logSaveAttempt()
        EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle)

        if (viewModel.isNewTopic && subject.isEmpty()) {
            binding.replySubjectLayout.error = getString(R.string.talk_subject_empty)
            binding.replySubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            binding.replyInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            binding.replyInputView.textInputLayout.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.replySaveButton.isEnabled = false

        viewModel.postReply(subject, body)
    }

    private fun onSaveSuccess(newRevision: Long) {
        AnonymousNotificationHelper.onEditSubmitted()

        binding.progressBar.visibility = View.GONE
        binding.replySaveButton.isEnabled = true
        editFunnel.logSaved(newRevision)
        EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle)

        Intent().let {
            it.putExtra(RESULT_NEW_REVISION_ID, newRevision)
            setResult(RESULT_EDIT_SUCCESS, it)
            finish()
        }
    }

    private fun onSaveError(t: Throwable) {
        editFunnel.logError(t.message)
        EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle)
        binding.progressBar.visibility = View.GONE
        binding.replySaveButton.isEnabled = true
        FeedbackUtil.showError(this, t)
    }

    /*
    private fun maybeShowUndoSnackbar() {
        if (undone) {
            replyClicked()
            undone = false
            return
        }
        if (showUndoSnackbar) {
            FeedbackUtil.makeSnackbar(this, getString(R.string.talk_response_submitted), FeedbackUtil.LENGTH_DEFAULT)
                .setAnchorView(binding.talkReplyButton)
                .setAction(R.string.talk_snackbar_undo) {
                    undone = true
                    binding.talkReplyButton.isEnabled = false
                    binding.talkReplyButton.alpha = 0.5f
                    binding.talkProgressBar.visibility = View.VISIBLE
                    // TODO
                    // viewModel.undoSave(revisionForUndo, "", "", "")
                }
                .show()
            showUndoSnackbar = false
        }
    }
    */

    private fun updateEditLicenseText() {
        binding.licenseText.text = StringUtil.fromHtml(getString(if (AccountUtil.isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_3_url)))
        binding.licenseText.movementMethod = LinkMovementMethodExt { url: String ->
            if (url == "https://#login") {
                val loginIntent = LoginActivity.newIntent(this,
                        LoginFunnel.SOURCE_EDIT, editFunnel.sessionToken)
                requestLogin.launch(loginIntent)
            } else {
                UriUtil.handleExternalLink(this, Uri.parse(url))
            }
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
            PageActivity.newIntentForCurrentTab(this, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, null, title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    override fun onBackPressed() {
        setResult(RESULT_BACK_FROM_TOPIC)
        super.onBackPressed()
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

    /*
    private fun parseUserNamesFromTopic(): Set<String> {
        val userNames = mutableSetOf<String>()
        // Go through our list of replies under the current topic, and collect any links to user
        // names, making sure to store them in reverse order, so that the last user name mentioned
        // in a response will appear first in the list of hints when searching for mentions.
        // TODO: search only up to the replied-to item
        viewModel.flattenedThreadItems.forEach {
            var start = 0
            val userList = mutableListOf<String>()
            while (true) {
                val searchStr = "title=\""
                start = it.html.indexOf(searchStr, startIndex = start)
                if (start < 0) {
                    break
                }
                start += searchStr.length
                val end = it.html.indexOf("\"", startIndex = start)
                if (end <= start) {
                    break
                }
                val name = it.html.substring(start, end)
                val title = PageTitle(name, viewModel.pageTitle.wikiSite)
                if (title.namespace() == Namespace.USER || title.namespace() == Namespace.USER_TALK) {
                    userList.add(0, StringUtil.removeUnderscores(title.text))
                }
                start = end
            }
            userNames.addAll(userList)
        }
        return userNames
    }
    */

    companion object {
        const val EXTRA_PAGE_TITLE = "pageTitle"
        const val EXTRA_TOPIC = "topic"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_BODY = "body"
        const val RESULT_EDIT_SUCCESS = 1
        const val RESULT_BACK_FROM_TOPIC = 2
        const val RESULT_NEW_REVISION_ID = "newRevisionId"

        fun newIntent(context: Context,
                      pageTitle: PageTitle,
                      topic: ThreadItem?,
                      invokeSource: Constants.InvokeSource,
                      undoneSubject: String? = null,
                      undoneBody: String? = null): Intent {
            return Intent(context, TalkReplyActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(EXTRA_TOPIC, topic)
                    .putExtra(EXTRA_SUBJECT, undoneSubject ?: "")
                    .putExtra(EXTRA_BODY, undoneBody ?: "")
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
