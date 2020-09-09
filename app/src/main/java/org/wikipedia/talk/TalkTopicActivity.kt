package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.talk.TalkTopicsActivity.Companion.newIntent
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private val disposables = CompositeDisposable()
    private var topicId: Int = 0
    private var wikiSite: WikiSite = WikipediaApp.getInstance().wikiSite
    private var userName: String = ""
    private var topic: TalkPage.Topic? = null
    private var replyActive = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    private var linkHandler: TalkLinkHandler? = null
    private val linkMovementMethod = LinkMovementMethodExt { url: String ->
        linkHandler?.onUrlClick(url, null, "")
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topic)
        title = ""
        linkHandler = TalkLinkHandler(this)

        if (intent.hasExtra(EXTRA_LANGUAGE)) {
            wikiSite = WikiSite.forLanguageCode(intent.getStringExtra(EXTRA_LANGUAGE).orEmpty())
        }
        userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        topicId = intent.extras?.getInt(EXTRA_TOPIC, 0)!!

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
            // TODO
        }

        talkRefreshView.setOnRefreshListener {
            loadTopic()
        }

        talkReplyButton.visibility = View.GONE
        loadTopic()
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun loadTopic() {
        disposables.clear()
        talkProgressBar.visibility = View.VISIBLE
        talkErrorView.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(wikiSite).getTalkPage(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    topic = response.topics?.find { t -> t.id == topicId }
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

    companion object {
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_USER_NAME = "userName"
        private const val EXTRA_TOPIC = "topicId"

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