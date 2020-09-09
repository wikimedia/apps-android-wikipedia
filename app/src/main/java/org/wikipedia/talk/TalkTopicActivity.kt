package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_talk_topic.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.talk.TalkTopicsActivity.Companion.newIntent
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import java.util.concurrent.TimeUnit

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private val disposables = CompositeDisposable()
    private var topicId: Int = -1
    private var wikiSite: WikiSite = WikipediaApp.getInstance().wikiSite
    private var userName: String = ""
    private var topic: TalkPage.Topic? = null
    private var replyActive = false
    private val textWatcher = ReplyTextWatcher()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var csrfClient: CsrfTokenClient? = null
    private var currentRevision: Long = 0

    private var linkHandler: TalkLinkHandler? = null
    private val linkMovementMethod = LinkMovementMethodExt { url: String ->
        linkHandler?.onUrlClick(url, null, "")
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topic)
        setSupportActionBar(replyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        linkHandler = TalkLinkHandler(this)

        if (intent.hasExtra(EXTRA_LANGUAGE)) {
            wikiSite = WikiSite.forLanguageCode(intent.getStringExtra(EXTRA_LANGUAGE).orEmpty())
        }
        userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        topicId = intent.extras?.getInt(EXTRA_TOPIC, -1)!!

        L10nUtil.setConditionalLayoutDirection(talkRefreshView, wikiSite.languageCode())

        talkRecyclerView.layoutManager = LinearLayoutManager(this)
        talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        talkRecyclerView.adapter = TalkReplyItemAdapter()
        
        L10nUtil.setConditionalLayoutDirection(talkRefreshView, wikiSite.languageCode())

        talkErrorView.setBackClickListener {
            finish()
        }
        talkErrorView.setRetryClickListener {
            loadTopic()
        }

        talkReplyButton.setOnClickListener {
            replyActive = true
            talkRecyclerView.adapter?.notifyDataSetChanged()
            talkScrollContainer.fullScroll(View.FOCUS_DOWN)
            replySaveButton.visibility = View.VISIBLE
            replyTextLayout.visibility = View.VISIBLE
            replyTextLayout.requestFocus()
            DeviceUtil.showSoftKeyboard(replyEditText)
            talkReplyButton.hide()
        }

        replySubjectText.addTextChangedListener(textWatcher)
        replyEditText.addTextChangedListener(textWatcher)
        replySaveButton.setOnClickListener {
            onSaveClicked()
        }

        talkRefreshView.isEnabled = !isNewTopic()
        talkRefreshView.setOnRefreshListener {
            loadTopic()
        }

        talkReplyButton.visibility = View.GONE

        onInitialLoad()
    }

    public override fun onDestroy() {
        disposables.clear()
        replySubjectText.removeTextChangedListener(textWatcher)
        replyEditText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        if (isNewTopic()) {
            replyActive = true
            title = getString(R.string.talk_new_topic)
            talkProgressBar.visibility = View.GONE
            talkErrorView.visibility = View.GONE
            replySaveButton.visibility = View.VISIBLE
            replySubjectLayout.visibility = View.VISIBLE
            replyTextLayout.hint = getString(R.string.talk_message_hint)
            replyTextLayout.visibility = View.VISIBLE
            replySubjectLayout.requestFocus()
            DeviceUtil.showSoftKeyboard(replySubjectLayout)
        } else {
            replyActive = false
            replyEditText.setText("")
            replySaveButton.visibility = View.GONE
            replySubjectLayout.visibility = View.GONE
            replyTextLayout.visibility = View.GONE
            replyTextLayout.hint = getString(R.string.talk_reply_hint)
            loadTopic()
        }
    }

    private fun loadTopic() {
        if (isNewTopic()) {
            return
        }
        disposables.clear()
        talkProgressBar.visibility = View.VISIBLE
        talkErrorView.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(wikiSite).getTalkPage(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response ->
                    val talkTopic = response.topics?.find { t -> t.id == topicId }!!
                    TalkPageSeenDatabaseTable.setTalkTopicSeen(talkTopic)
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
        talkProgressBar.visibility = View.GONE
        talkErrorView.visibility = View.GONE
        talkReplyButton.show()
        talkRefreshView.isRefreshing = false

        val titleStr = StringUtil.fromHtml(topic?.html).toString().trim()
        title = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
        talkRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(t: Throwable) {
        talkProgressBar.visibility = View.GONE
        talkRefreshView.isRefreshing = false
        talkReplyButton.hide()
        talkErrorView.visibility = View.VISIBLE
        talkErrorView.setError(t)
    }

    private fun showLinkPreviewOrNavigate(title: PageTitle) {
        if (title.namespace() == Namespace.USER_TALK) {
            startActivity(newIntent(this, title.wikiSite.languageCode(), title.text))
        } else {
            bottomSheetPresenter.show(supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(title, HistoryEntry.SOURCE_TALK_TOPIC), null))
        }
    }

    private fun isNewTopic(): Boolean {
        return topicId == -1
    }

    internal inner class TalkReplyHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.replyText)
        private val indentArrow: View = view.findViewById(R.id.replyIndentArrow)
        private val bottomSpace: View = view.findViewById(R.id.replyBottomSpace)
        fun bindItem(reply: TalkPage.TopicReply, isLast: Boolean) {
            text.movementMethod = linkMovementMethod
            text.text = StringUtil.fromHtml(reply.html)
            indentArrow.visibility = if (reply.depth > 0) View.VISIBLE else View.GONE
            bottomSpace.visibility = if (!isLast || replyActive) View.GONE else View.VISIBLE
        }
    }

    internal inner class TalkReplyItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return topic?.replies?.size ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return TalkReplyHolder(layoutInflater.inflate(R.layout.item_talk_reply, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            (holder as TalkReplyHolder).bindItem(topic?.replies!![pos], pos == itemCount - 1)
        }
    }

    internal inner class TalkLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        override fun getWikiSite(): WikiSite {
            return this@TalkTopicActivity.wikiSite
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // TODO
        }

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
           showLinkPreviewOrNavigate(title)
        }
    }

    @Suppress("RedundantInnerClassModifier")
    internal inner class ReplyTextWatcher: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            replySubjectLayout.error = null
            replyTextLayout.error = null
        }

        override fun afterTextChanged(p0: Editable?) {
        }
    }

    private fun onSaveClicked() {
        val subject = replySubjectText.text.toString().trim()
        var body = replyEditText.text.toString().trim()

        if (isNewTopic() && subject.isEmpty()) {
            replySubjectLayout.error = getString(R.string.talk_subject_empty)
            replySubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            replyTextLayout.error = getString(R.string.talk_message_empty)
            replyTextLayout.requestFocus()
            return
        }

        // if the message is not signed, then sign it explicitly
        if (!body.endsWith("~~~~")) {
            body += " ~~~~"
        }
        if (!isNewTopic()) {
            // add two explicit newlines at the beginning, to delineate this message as a new paragraph.
            body = "\n\n" + body
        }

        talkProgressBar.visibility = View.VISIBLE
        replySaveButton.isEnabled = false

        csrfClient = CsrfTokenClient(wikiSite, wikiSite)
        csrfClient?.request(false, object : CsrfTokenClient.Callback {
            override fun success(token: String) {
                doSave(token, subject, body)
            }

            override fun failure(caught: Throwable) {
                onSaveError(caught)
            }

            override fun twoFactorPrompt() {
                onSaveError(LoginFailedException(resources.getString(R.string.login_2fa_other_workflow_error_msg)))
            }
        })
    }

    private fun doSave(token: String, subject: String, body: String) {
        disposables.add(ServiceFactory.get(wikiSite).postEditSubmit("User_talk:$userName",
                if (isNewTopic()) "new" else topicId.toString(),
                if (isNewTopic()) subject else null,
                "", if (AccountUtil.isLoggedIn()) "user" else null,
                if (isNewTopic()) body else null, if (isNewTopic()) null else body,
                currentRevision, token, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    waitForUpdatedRevision(it.edit()!!.newRevId())
                }, {
                    onSaveError(it)
                }))
    }

    private fun waitForUpdatedRevision(newRevision: Long) {
        disposables.add(ServiceFactory.getRest(wikiSite).getTalkPage(userName)
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map { response ->
                    if (response.revision < newRevision) {
                        throw IllegalStateException()
                    }
                    response
                }
                .retry(20) { t ->
                    (t is IllegalStateException)
                            || (isNewTopic() && t is HttpStatusException && t.code() == 404)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onSaveSuccess()
                }, { t ->
                    L.e(t)
                    onSaveError(t)
                }))
    }

    private fun onSaveSuccess() {
        talkProgressBar.visibility = View.GONE

        if (isNewTopic()) {
            setResult(RESULT_EDIT_SUCCESS)
            finish()
        } else {
            onInitialLoad()
        }
    }

    private fun onSaveError(t: Throwable) {
        talkProgressBar.visibility = View.GONE
        FeedbackUtil.showError(this, t)
    }

    companion object {
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_USER_NAME = "userName"
        private const val EXTRA_TOPIC = "topicId"
        const val RESULT_EDIT_SUCCESS = 1

        @JvmStatic
        fun newIntent(context: Context, language: String?, userName: String?, topicId: Int): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(EXTRA_LANGUAGE, language.orEmpty())
                    .putExtra(EXTRA_USER_NAME, userName.orEmpty())
                    .putExtra(EXTRA_TOPIC, topicId)
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
            PageActivity.newIntentForCurrentTab(this, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, null, title.uri.toString())
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }
}