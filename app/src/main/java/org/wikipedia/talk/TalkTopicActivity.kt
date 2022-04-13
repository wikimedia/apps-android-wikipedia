package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.ActivityTalkTopicBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.UserMentionInputView
import java.util.concurrent.TimeUnit

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback, UserMentionInputView.Listener {
    private lateinit var binding: ActivityTalkTopicBinding
    private lateinit var pageTitle: PageTitle
    private lateinit var talkFunnel: TalkFunnel
    private lateinit var editFunnel: EditFunnel
    private lateinit var linkHandler: TalkLinkHandler
    private lateinit var textWatcher: TextWatcher

    private val disposables = CompositeDisposable()
    private var topicId: Int = -1
    private var topicIndicatorSha: String = ""
    private var topic: TalkPage.Topic? = null
    private var replyActive = false
    private var undone = false
    private var undoneBody = ""
    private var undoneSubject = ""
    private var showUndoSnackbar = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var currentRevision: Long = 0
    private var revisionForUndo: Long = 0
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
    private val requestEditSource = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            // TODO: maybe add funnel?
            loadTopic()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.replyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        if (intent.hasExtra(EXTRA_SUBJECT)) undoneSubject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""
        if (intent.hasExtra(EXTRA_BODY)) undoneBody = intent.getStringExtra(EXTRA_BODY) ?: ""
        linkHandler = TalkLinkHandler(this)
        linkHandler.wikiSite = pageTitle.wikiSite
        topicId = intent.extras?.getInt(EXTRA_TOPIC, -1)!!
        topicIndicatorSha = intent.extras?.getString(EXTRA_TOPIC_INDICATOR_SHA, "")!!

        L10nUtil.setConditionalLayoutDirection(binding.talkRefreshView, pageTitle.wikiSite.languageCode)
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent))

        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        binding.talkRecyclerView.adapter = TalkReplyItemAdapter()

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            loadTopic()
        }

        binding.talkReplyButton.setOnClickListener {
            talkFunnel.logReplyClick()
            editFunnel.logStart()
            EditAttemptStepEvent.logInit(pageTitle)
            replyClicked()
        }

        textWatcher = binding.replySubjectText.doOnTextChanged { _, _, _, _ ->
            binding.replySubjectLayout.error = null
            binding.replyInputView.textInputLayout.error = null
        }
        binding.replySaveButton.setOnClickListener {
            onSaveClicked()
        }

        binding.talkRefreshView.isEnabled = !isNewTopic()
        binding.talkRefreshView.setOnRefreshListener {
            talkFunnel.logRefresh()
            loadTopic()
        }

        binding.talkScrollContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            if (binding.talkSubjectView.isVisible) {
                binding.talkToolbarSubjectView.visibility = if (binding.talkScrollContainer.scrollY >
                        binding.talkSubjectView.height) View.VISIBLE else View.INVISIBLE
            }
        })

        binding.talkReplyButton.visibility = View.GONE

        binding.replyInputView.wikiSite = pageTitle.wikiSite
        binding.replyInputView.listener = this

        talkFunnel = TalkFunnel(pageTitle, intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource)
        talkFunnel.logOpenTopic()

        editFunnel = EditFunnel(WikipediaApp.getInstance(), pageTitle)
        updateEditLicenseText()

        onInitialLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_talk_topic, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_talk_topic_group)?.isVisible = !replyActive
        menu?.findItem(R.id.menu_edit_source)?.isVisible = AccountUtil.isLoggedIn
        binding.talkRefreshView.isEnabled = !replyActive
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_discussion_subject, topic?.html?.ifEmpty { getString(R.string.talk_no_subject) }), pageTitle.uri + "#" + StringUtil.addUnderscores(topic?.html))
                true
            }
            R.id.menu_edit_source -> {
                requestEditSource.launch(EditSectionActivity.newIntent(this, topicId, undoneSubject, pageTitle))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun replyClicked() {
        replyActive = true
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        binding.talkScrollContainer.fullScroll(View.FOCUS_DOWN)
        binding.replySaveButton.visibility = View.VISIBLE
        binding.replyInputView.visibility = View.VISIBLE
        binding.replyInputView.maybePrepopulateUserName()

        binding.licenseText.visibility = View.VISIBLE
        binding.talkScrollContainer.post {
            if (!isDestroyed) {
                binding.replyInputView.editText.requestFocus()
                DeviceUtil.showSoftKeyboard(binding.replyInputView.editText)
                binding.talkScrollContainer.postDelayed({
                    binding.talkScrollContainer.smoothScrollTo(0, binding.talkScrollContainer.height * 4)
                }, 500)
            }
        }
        binding.talkReplyButton.hide()
        if (undone) {
            binding.replyInputView.editText.setText(undoneBody)
            binding.replyInputView.editText.setSelection(binding.replyInputView.editText.text.toString().length)
        }
        invalidateOptionsMenu()
    }

    public override fun onDestroy() {
        disposables.clear()
        binding.replySubjectText.removeTextChangedListener(textWatcher)
        binding.replyInputView.editText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        if (isNewTopic()) {
            replyActive = true
            title = getString(R.string.talk_new_topic)
            binding.talkSubjectView.visibility = View.GONE
            binding.talkToolbarSubjectView.visibility = View.INVISIBLE
            binding.talkProgressBar.visibility = View.GONE
            binding.talkErrorView.visibility = View.GONE
            binding.replySaveButton.visibility = View.VISIBLE
            binding.replySubjectLayout.visibility = View.VISIBLE
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
            binding.replySubjectText.setText(undoneSubject)
            binding.replyInputView.editText.setText(undoneBody)
            binding.replyInputView.visibility = View.VISIBLE
            binding.licenseText.visibility = View.VISIBLE
            binding.replySubjectLayout.requestFocus()
            editFunnel.logStart()
            EditAttemptStepEvent.logInit(pageTitle)
        } else {
            replyActive = false
            binding.replyInputView.editText.setText("")
            binding.replySaveButton.visibility = View.GONE
            binding.replySubjectLayout.visibility = View.GONE
            binding.replyInputView.visibility = View.GONE
            binding.replyInputView.textInputLayout.hint = getString(R.string.talk_reply_hint)
            binding.licenseText.visibility = View.GONE
            DeviceUtil.hideSoftKeyboard(this)
            loadTopic()
        }
        invalidateOptionsMenu()
    }

    private fun loadTopic() {
        if (isNewTopic()) {
            return
        }
        disposables.clear()
        binding.talkProgressBar.visibility = View.VISIBLE
        binding.talkErrorView.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response ->
                    val talkTopic = response.topics?.find { t -> if (topicId == -1) t.getIndicatorSha() == topicIndicatorSha else t.id == topicId }!!
                    AppDatabase.instance.talkPageSeenDao().insertTalkPageSeen(TalkPageSeen(sha = talkTopic.getIndicatorSha()))
                    currentRevision = response.revision
                    talkTopic
                }
                .subscribe({
                    topic = it
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                }))
    }

    private fun updateOnSuccess() {
        binding.talkProgressBar.visibility = View.GONE
        binding.talkErrorView.visibility = View.GONE
        if (replyActive || shouldHideReplyButton()) {
            binding.talkReplyButton.hide()
        } else {
            binding.talkReplyButton.show()
            binding.talkReplyButton.isEnabled = true
            binding.talkReplyButton.alpha = 1.0f
        }
        binding.talkRefreshView.isRefreshing = false

        val titleStr = StringUtil.fromHtml(topic?.html).toString().trim()
        binding.talkSubjectView.text = titleStr.ifEmpty { getString(R.string.talk_no_subject) }
        binding.talkSubjectView.visibility = View.VISIBLE
        binding.talkToolbarSubjectView.text = binding.talkSubjectView.text
        binding.talkToolbarSubjectView.visibility = View.INVISIBLE
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        binding.replyInputView.userNameHints = parseUserNamesFromTopic()

        maybeShowUndoSnackbar()
    }

    private fun updateOnError(t: Throwable) {
        binding.talkProgressBar.visibility = View.GONE
        binding.talkRefreshView.isRefreshing = false
        binding.talkReplyButton.hide()
        binding.talkErrorView.visibility = View.VISIBLE
        binding.talkErrorView.setError(t)
    }

    private fun isNewTopic(): Boolean {
        return topicId == TalkTopicsActivity.NEW_TOPIC_ID
    }

    private fun shouldHideReplyButton(): Boolean {
        // Hide the reply button when:
        // a) The topic ID is -1, which means the API couldn't parse it properly (TODO: wait until fixed)
        // b) The name of the topic is empty, implying that this is the topmost "header" section.
        return topicId == -1 || topic?.html.orEmpty().trim().isEmpty()
    }

    internal inner class TalkReplyHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.replyText)
        private val indentArrow: View = view.findViewById(R.id.replyIndentArrow)
        private val bottomSpace: View = view.findViewById(R.id.replyBottomSpace)
        fun bindItem(reply: TalkPage.TopicReply, isLast: Boolean) {
            text.movementMethod = linkMovementMethod
            text.text = StringUtil.fromHtml(reply.html)
            indentArrow.visibility = if (reply.depth > 0) View.VISIBLE else View.GONE
            bottomSpace.visibility = if (!isLast || replyActive || shouldHideReplyButton()) View.GONE else View.VISIBLE
        }
    }

    internal inner class TalkReplyItemAdapter : RecyclerView.Adapter<TalkReplyHolder>() {
        override fun getItemCount(): Int {
            return topic?.replies?.size ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): TalkReplyHolder {
            return TalkReplyHolder(layoutInflater.inflate(R.layout.item_talk_reply, parent, false))
        }

        override fun onBindViewHolder(holder: TalkReplyHolder, pos: Int) {
            holder.bindItem(topic?.replies!![pos], pos == itemCount - 1)
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
            UserTalkPopupHelper.show(this@TalkTopicActivity, bottomSheetPresenter, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun onSaveClicked() {
        val subject = binding.replySubjectText.text.toString().trim()
        var body = binding.replyInputView.editText.getParsedText(pageTitle.wikiSite).trim()
        undoneBody = body
        undoneSubject = subject

        editFunnel.logSaveAttempt()
        EditAttemptStepEvent.logSaveAttempt(pageTitle)

        if (isNewTopic() && subject.isEmpty()) {
            binding.replySubjectLayout.error = getString(R.string.talk_subject_empty)
            binding.replySubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            binding.replyInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            binding.replyInputView.textInputLayout.requestFocus()
            return
        }

        val topicDepth = topic?.replies?.lastOrNull()?.depth ?: 0

        body = addDefaultFormatting(body, topicDepth, isNewTopic())

        binding.talkProgressBar.visibility = View.VISIBLE
        binding.replySaveButton.isEnabled = false

        talkFunnel.logEditSubmit()

        disposables.add(CsrfTokenClient(pageTitle.wikiSite).token
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    doSave(it, subject, body)
                }, {
                    onSaveError(it)
                }))
    }

    private fun undoSave() {
        disposables.add(CsrfTokenClient(pageTitle.wikiSite).token
            .subscribeOn(Schedulers.io())
            .flatMap { token -> ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(pageTitle.prefixedText, revisionForUndo, token) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                waitForUpdatedRevision(it.edit!!.newRevId)
            }, {
                onSaveError(it)
            }))
    }

    private fun doSave(token: String, subject: String, body: String) {
        disposables.add(ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(pageTitle.prefixedText,
                if (isNewTopic()) "new" else topicId.toString(),
                if (isNewTopic()) subject else null,
                "", if (AccountUtil.isLoggedIn) "user" else null,
                if (isNewTopic()) body else null, if (isNewTopic()) null else body,
                currentRevision, token, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showUndoSnackbar = true
                    AnonymousNotificationHelper.onEditSubmitted()
                    waitForUpdatedRevision(it.edit!!.newRevId)
                }, {
                    onSaveError(it)
                }))
    }

    @Suppress("SameParameterValue")
    private fun waitForUpdatedRevision(newRevision: Long) {
        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map { response ->
                    if (response.revision < newRevision) {
                        throw IllegalStateException()
                    }
                    response.revision
                }
                .retry(20) { t ->
                    (t is IllegalStateException) ||
                            (isNewTopic() && t is HttpStatusException && t.code == 404)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    revisionForUndo = it
                    onSaveSuccess(it)
                }, { t ->
                    L.e(t)
                    onSaveError(t)
                }))
    }

    private fun onSaveSuccess(newRevision: Long) {
        binding.talkProgressBar.visibility = View.GONE
        binding.replySaveButton.isEnabled = true
        editFunnel.logSaved(newRevision)
        EditAttemptStepEvent.logSaveSuccess(pageTitle)

        if (isNewTopic()) {
            Intent().let {
                it.putExtra(RESULT_NEW_REVISION_ID, newRevision)
                it.putExtra(EXTRA_TOPIC, topicId)
                it.putExtra(EXTRA_SUBJECT, undoneSubject)
                it.putExtra(EXTRA_BODY, undoneBody)
                setResult(RESULT_EDIT_SUCCESS, it)
                finish()
            }
        } else {
            onInitialLoad()
        }
    }

    private fun onSaveError(t: Throwable) {
        editFunnel.logError(t.message)
        EditAttemptStepEvent.logSaveFailure(pageTitle)
        binding.talkProgressBar.visibility = View.GONE
        binding.replySaveButton.isEnabled = true
        FeedbackUtil.showError(this, t)
    }

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
                    undoSave()
                }
                .show()
            showUndoSnackbar = false
        }
    }

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
        if (replyActive && !isNewTopic()) {
            onInitialLoad()
        } else {
            setResult(RESULT_BACK_FROM_TOPIC)
            super.onBackPressed()
        }
    }

    override fun onUserMentionListUpdate() {
        if (!replyActive) {
            return
        }
        binding.licenseText.isVisible = false
        binding.talkScrollContainer.post {
            if (!isDestroyed && !userMentionScrolled) {
                binding.talkScrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        if (!replyActive) {
            return
        }
        userMentionScrolled = false
        binding.licenseText.isVisible = true
    }

    private fun parseUserNamesFromTopic(): Set<String> {
        val userNames = mutableSetOf<String>()
        // Go through our list of replies under the current topic, and collect any links to user
        // names, making sure to store them in reverse order, so that the last user name mentioned
        // in a response will appear first in the list of hints when searching for mentions.
        topic?.replies?.forEach {
            var start = 0
            val userList = mutableListOf<String>()
            while (true) {
                val searchStr = "title=\""
                start = it.html!!.indexOf(searchStr, startIndex = start)
                if (start < 0) {
                    break
                }
                start += searchStr.length
                val end = it.html!!.indexOf("\"", startIndex = start)
                if (end <= start) {
                    break
                }
                val name = it.html!!.substring(start, end)
                val title = PageTitle(name, pageTitle.wikiSite)
                if (title.namespace() == Namespace.USER || title.namespace() == Namespace.USER_TALK) {
                    userList.add(0, StringUtil.removeUnderscores(title.text))
                }
                start = end
            }
            userNames.addAll(userList)
        }
        return userNames
    }

    companion object {
        private const val EXTRA_PAGE_TITLE = "pageTitle"
        const val EXTRA_TOPIC = "topicId"
        const val EXTRA_TOPIC_INDICATOR_SHA = "topicIndicatorSha"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_BODY = "body"
        const val RESULT_EDIT_SUCCESS = 1
        const val RESULT_BACK_FROM_TOPIC = 2
        const val RESULT_NEW_REVISION_ID = "newRevisionId"

        fun newIntent(context: Context,
                      pageTitle: PageTitle,
                      topicId: Int,
                      topicIndicatorSha: String,
                      invokeSource: Constants.InvokeSource,
                      undoneSubject: String? = null,
                      undoneBody: String? = null): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(EXTRA_TOPIC, topicId)
                    .putExtra(EXTRA_TOPIC_INDICATOR_SHA, topicIndicatorSha)
                    .putExtra(EXTRA_SUBJECT, undoneSubject ?: "")
                    .putExtra(EXTRA_BODY, undoneBody ?: "")
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }

        fun addDefaultFormatting(text: String, topicDepth: Int, newTopic: Boolean = false): String {
            var body = ":".repeat(if (newTopic) 0 else topicDepth + 1) + text
            // if the message is not signed, then sign it explicitly
            if (!body.endsWith("~~~~")) {
                body += " ~~~~"
            }
            if (!newTopic) {
                // add two explicit newlines at the beginning, to delineate this message as a new paragraph.
                body = "\n\n" + body
            }
            return body
        }
    }
}
