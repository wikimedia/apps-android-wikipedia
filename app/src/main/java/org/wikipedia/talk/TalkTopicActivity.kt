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
import kotlinx.android.synthetic.main.item_suggested_edits_type.view.*
import kotlinx.android.synthetic.main.view_on_this_day_event.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.TalkFunnel
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
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PlainPasteEditText
import java.util.concurrent.TimeUnit

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var pageTitle: PageTitle
    private val disposables = CompositeDisposable()
    private var topicId: Int = -1
    private var topic: TalkPage.Topic? = null
    private var replyActive = false
    private var inLineReplyPosition = -1
    private val textWatcher = ReplyTextWatcher()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var csrfClient: CsrfTokenClient? = null
    private var currentRevision: Long = 0
    private lateinit var talkFunnel: TalkFunnel
    private lateinit var editFunnel: EditFunnel

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

        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        topicId = intent.extras?.getInt(EXTRA_TOPIC, -1)!!

        L10nUtil.setConditionalLayoutDirection(talkRefreshView, pageTitle.wikiSite.languageCode())

        talkRecyclerView.layoutManager = LinearLayoutManager(this)
        talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        talkRecyclerView.adapter = TalkReplyItemAdapter()

        L10nUtil.setConditionalLayoutDirection(talkRefreshView, pageTitle.wikiSite.languageCode())

        talkErrorView.setBackClickListener {
            finish()
        }
        talkErrorView.setRetryClickListener {
            loadTopic()
        }

        talkReplyButton.setOnClickListener {
            talkFunnel.logReplyClick()
            replyActive = true
            talkRecyclerView.adapter?.notifyDataSetChanged()
            talkScrollContainer.fullScroll(View.FOCUS_DOWN)
            replySaveButton.visibility = View.VISIBLE
            replyTextLayout.visibility = View.VISIBLE
            replyTextLayout.requestFocus()
            onStartComposition()
            talkReplyButton.hide()
        }

        replySubjectText.addTextChangedListener(textWatcher)
        replyEditText.addTextChangedListener(textWatcher)
        replySaveButton.setOnClickListener {
            onSaveClicked()
        }

        talkRefreshView.isEnabled = !isNewTopic()
        talkRefreshView.setOnRefreshListener {
            talkFunnel.logRefresh()
            loadTopic()
        }

        talkReplyButton.visibility = View.GONE

        talkFunnel = TalkFunnel(pageTitle, intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource)
        talkFunnel.logOpenTopic()

        editFunnel = EditFunnel(WikipediaApp.getInstance(), pageTitle)
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
            talkSubjectView.visibility = View.GONE
            talkProgressBar.visibility = View.GONE
            talkErrorView.visibility = View.GONE
            replySaveButton.visibility = View.VISIBLE
            replySubjectLayout.visibility = View.VISIBLE
            replyTextLayout.hint = getString(R.string.talk_message_hint)
            replyTextLayout.visibility = View.VISIBLE
            replySubjectLayout.requestFocus()
            onStartComposition()
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

    private fun onStartComposition() {
        editFunnel.logStart()
        DeviceUtil.showSoftKeyboard(replySubjectLayout)
    }

    private fun loadTopic() {
        if (isNewTopic()) {
            return
        }
        disposables.clear()
        talkProgressBar.visibility = View.VISIBLE
        talkErrorView.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
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
        if (replyActive) {
            talkReplyButton.hide()
        } else {
            talkReplyButton.show()
        }
        talkRefreshView.isRefreshing = false

        val titleStr = StringUtil.fromHtml(topic?.html).toString().trim()
        talkSubjectView.text = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
        talkSubjectView.visibility = View.VISIBLE
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
        if (title.namespace() == Namespace.USER_TALK || title.namespace() == Namespace.TALK) {
            startActivity(TalkTopicsActivity.newIntent(this, title.pageTitleForTalkPage(), Constants.InvokeSource.TALK_ACTIVITY))
        } else {
            bottomSheetPresenter.show(supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(title, HistoryEntry.SOURCE_TALK_TOPIC), null))
        }
    }

    private fun isNewTopic(): Boolean {
        return topicId == -1
    }

    private fun inLineReplyText(position: Int): String {
        return " [ <a href='$IN_LINE_REPLY_ANCHOR$position'>${L10nUtil.getStringForArticleLanguage(pageTitle, R.string.talk_add_reply)}</a> ]"
    }

    internal inner class TalkReplyHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.replyText)
        private val indentArrow: View = view.findViewById(R.id.replyIndentArrow)
        private val inlineReplyTextLayout: View = view.findViewById(R.id.replyTextLayout)
        private val bottomSpace: View = view.findViewById(R.id.replyBottomSpace)
        fun bindItem(reply: TalkPage.TopicReply, position: Int, isLast: Boolean) {
            text.movementMethod = linkMovementMethod
            text.text = StringUtil.fromHtml(reply.html + inLineReplyText(position))
            indentArrow.visibility = if (reply.depth > 0) View.VISIBLE else View.GONE
            bottomSpace.visibility = if (!isLast || replyActive) View.GONE else View.VISIBLE
            if (inLineReplyPosition == position) {
                inlineReplyTextLayout.visibility = View.VISIBLE
                inlineReplyTextLayout.requestFocus()
            } else {
                inlineReplyTextLayout.visibility = View.GONE
            }
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
            (holder as TalkReplyHolder).bindItem(topic?.replies!![pos], pos, pos == itemCount - 1)
        }
    }

    internal inner class TalkLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        override fun getWikiSite(): WikiSite {
            return this@TalkTopicActivity.pageTitle.wikiSite
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // TODO
        }

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            val position = anchor.split("-")[1].toInt()
            if (replyActive && position == inLineReplyPosition) {
                inLineReplyPosition = -1
                replyActive = false
                replySaveButton.visibility = View.GONE
                talkReplyButton.visibility = View.VISIBLE
            } else {
                inLineReplyPosition = position
                replyActive = true
                replySaveButton.visibility = View.VISIBLE
                replyTextLayout.visibility = View.GONE
                talkReplyButton.visibility = View.GONE
            }
            talkRecyclerView.adapter?.notifyDataSetChanged()
        }

        override fun onInternalLinkClicked(title: PageTitle) {
           showLinkPreviewOrNavigate(title)
        }
    }

    @Suppress("RedundantInnerClassModifier")
    internal inner class ReplyTextWatcher : TextWatcher {
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
        var subject = ""
        var body = ""
        if (inLineReplyPosition != -1) {
            body = talkRecyclerView.layoutManager
                    ?.findViewByPosition(inLineReplyPosition)
                    ?.findViewById<PlainPasteEditText>(R.id.replyEditText)
                    ?.text.toString().trim()

            // TODO: use getWikiTextForSection() to get the section and append text to certain line?
            // TODO: then use postEdit()?
            if (!body.endsWith("~~~~")) {
                body += " ~~~~"
            }
            body = "\n\n:$body"
        } else {
            subject = replySubjectText.text.toString().trim()
            body = replyEditText.text.toString().trim()

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
        }

        // TODO: remove this
        return

        talkProgressBar.visibility = View.VISIBLE
        replySaveButton.isEnabled = false

        talkFunnel.logEditSubmit()

        csrfClient = CsrfTokenClient(pageTitle.wikiSite, pageTitle.wikiSite)
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
        disposables.add(ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(pageTitle.prefixedText,
                if (isNewTopic()) "new" else topicId.toString(),
                if (isNewTopic()) subject else null,
                "", if (AccountUtil.isLoggedIn) "user" else null,
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
                            (isNewTopic() && t is HttpStatusException && t.code() == 404)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onSaveSuccess(it)
                }, { t ->
                    L.e(t)
                    onSaveError(t)
                }))
    }

    private fun onSaveSuccess(newRevision: Long) {
        talkProgressBar.visibility = View.GONE
        editFunnel.logSaved(newRevision)

        if (isNewTopic()) {
            setResult(RESULT_EDIT_SUCCESS)
            finish()
        } else {
            onInitialLoad()
        }
    }

    private fun onSaveError(t: Throwable) {
        editFunnel.logError(t.message)
        talkProgressBar.visibility = View.GONE
        FeedbackUtil.showError(this, t)
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

    companion object {
        private const val EXTRA_PAGE_TITLE = "pageTitle"
        private const val EXTRA_TOPIC = "topicId"
        const val IN_LINE_REPLY_ANCHOR = "#inlinereply-"
        const val RESULT_EDIT_SUCCESS = 1

        @JvmStatic
        fun newIntent(context: Context, pageTitle: PageTitle, topicId: Int, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(EXTRA_TOPIC, topicId)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
