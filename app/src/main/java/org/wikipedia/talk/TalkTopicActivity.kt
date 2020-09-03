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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.talk.TalkTopicsActivity.Companion.newIntent
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import java.util.concurrent.TimeUnit

class TalkTopicActivity : BaseActivity() {
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
        setSupportActionBar(reply_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        linkHandler = TalkLinkHandler(this)

        if (intent.hasExtra(EXTRA_LANGUAGE)) {
            wikiSite = WikiSite.forLanguageCode(intent.getStringExtra(EXTRA_LANGUAGE).orEmpty())
        }
        userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        topicId = intent.extras?.getInt(EXTRA_TOPIC, -1)!!

        talk_recycler_view.layoutManager = LinearLayoutManager(this)
        talk_recycler_view.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        talk_recycler_view.adapter = TalkReplyItemAdapter()

        talk_reply_button.setOnClickListener {
            replyActive = true
            talk_recycler_view.adapter?.notifyDataSetChanged()
            talk_scroll_container.fullScroll(View.FOCUS_DOWN)
            reply_save_button.visibility = View.VISIBLE
            reply_text_layout.visibility = View.VISIBLE
            reply_text_layout.requestFocus()
            DeviceUtil.showSoftKeyboard(reply_edit_text)
            talk_reply_button.hide()
        }

        reply_subject_text.addTextChangedListener(textWatcher)
        reply_edit_text.addTextChangedListener(textWatcher)
        reply_save_button.setOnClickListener {
            onSaveClicked()
        }

        talk_refresh_view.isEnabled = !isNewTopic()
        talk_refresh_view.setOnRefreshListener {
            loadTopic()
        }

        talk_reply_button.visibility = View.GONE

        onInitialLoad()
    }

    public override fun onDestroy() {
        disposables.clear()
        reply_subject_text.removeTextChangedListener(textWatcher)
        reply_edit_text.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        if (isNewTopic()) {
            replyActive = true
            title = getString(R.string.talk_new_topic)
            talk_progress_bar.visibility = View.GONE
            talk_error_view.visibility = View.GONE
            reply_save_button.visibility = View.VISIBLE
            reply_subject_layout.visibility = View.VISIBLE
            reply_text_layout.hint = getString(R.string.talk_message_hint)
            reply_text_layout.visibility = View.VISIBLE
            reply_subject_layout.requestFocus()
            DeviceUtil.showSoftKeyboard(reply_subject_layout)
        } else {
            replyActive = false
            reply_edit_text.setText("")
            reply_save_button.visibility = View.GONE
            reply_subject_layout.visibility = View.GONE
            reply_text_layout.visibility = View.GONE
            reply_text_layout.hint = getString(R.string.talk_reply_hint)
            loadTopic()
        }
    }

    private fun loadTopic() {
        if (isNewTopic()) {
            return
        }
        disposables.clear()
        talk_progress_bar.visibility = View.VISIBLE
        talk_error_view.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(wikiSite).getTalkPage(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    topic = response.topics?.find { t -> t.id == topicId }
                    currentRevision = response.revision
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                }))
    }

    private fun updateOnSuccess() {
        talk_progress_bar.visibility = View.GONE
        talk_error_view.visibility = View.GONE
        talk_reply_button.show()
        talk_refresh_view.isRefreshing = false

        val titleStr = StringUtil.fromHtml(topic?.html).toString().trim()
        title = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
        talk_recycler_view.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(t: Throwable) {
        talk_progress_bar.visibility = View.GONE
        talk_refresh_view.isRefreshing = false
        talk_reply_button.hide()
        talk_error_view.visibility = View.VISIBLE
        talk_error_view.setError(t)
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
        private val text: TextView = view.findViewById(R.id.reply_text)
        private val indentArrow: View = view.findViewById(R.id.reply_indent_arrow)
        private val bottomSpace: View = view.findViewById(R.id.reply_bottom_space)
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

    inner internal class ReplyTextWatcher: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            reply_subject_layout.error = null
            reply_text_layout.error = null
        }

        override fun afterTextChanged(p0: Editable?) {
        }
    }

    private fun onSaveClicked() {
        val subject = reply_subject_text.text.toString().trim()
        var body = reply_edit_text.text.toString().trim()

        if (isNewTopic() && subject.isEmpty()) {
            reply_subject_layout.error = getString(R.string.talk_subject_empty)
            reply_subject_layout.requestFocus()
            return
        } else if (body.isEmpty()) {
            reply_text_layout.error = getString(R.string.talk_message_empty)
            reply_text_layout.requestFocus()
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

        talk_progress_bar.visibility = View.VISIBLE
        reply_save_button.isEnabled = false

        csrfClient = CsrfTokenClient(WikipediaApp.getInstance().wikiSite, WikipediaApp.getInstance().wikiSite)
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
        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().wikiSite).postEditSubmit("User_talk:$userName",
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
        disposables.add(ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getTalkPage(userName)
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())

                .map { response ->
                    if (response.revision < newRevision) {
                        throw IllegalStateException()
                    }
                    response
                }
                .retry(20) { t -> t is IllegalStateException }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onSaveSuccess()
                }, { t ->
                    L.e(t)
                    onSaveError(t)
                }))
    }

    private fun onSaveSuccess() {
        talk_progress_bar.visibility = View.GONE

        if (isNewTopic()) {
            setResult(RESULT_EDIT_SUCCESS)
            finish()
        } else {
            onInitialLoad()
        }
    }

    private fun onSaveError(t: Throwable) {
        talk_progress_bar.visibility = View.GONE
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
}