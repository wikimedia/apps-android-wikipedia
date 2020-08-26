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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration

class TalkTopicActivity : BaseActivity() {
    private val disposables = CompositeDisposable()
    private var topicId: Int = 0
    private var topic: TalkPage.Topic? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topic)
        title = ""

        topicId = intent.extras?.getInt(EXTRA_TOPIC, 0)!!

        talk_recycler_view.layoutManager = LinearLayoutManager(this)
        talk_recycler_view.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable))
        talk_recycler_view.adapter = TalkReplyItemAdapter()

        talk_reply_button.setOnClickListener {
            // TODO
        }

        talk_refresh_view.setOnRefreshListener {
            loadTopic()
        }

        talk_reply_button.visibility = View.GONE
        loadTopic()
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun loadTopic() {
        disposables.clear()
        talk_progress_bar.visibility = View.VISIBLE
        talk_error_view.visibility = View.GONE

        ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getTalkPage(AccountUtil.getUserName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    topic = response.topics?.find { t -> t.id == topicId }
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                })
    }

    private fun updateOnSuccess() {
        talk_progress_bar.visibility = View.GONE
        talk_error_view.visibility = View.GONE
        talk_reply_button.visibility = View.VISIBLE
        talk_refresh_view.isRefreshing = false

        title = StringUtil.fromHtml(topic?.html)
        talk_recycler_view.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(t: Throwable) {
        talk_progress_bar.visibility = View.GONE
        talk_refresh_view.isRefreshing = false
        talk_reply_button.visibility = View.GONE
        talk_error_view.visibility = View.VISIBLE
        talk_error_view.setError(t)
    }

    private class TalkReplyHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.reply_text)
        private val indentArrow: View = view.findViewById(R.id.reply_indent_arrow)
        fun bindItem(reply: TalkPage.TopicReply) {
            text.movementMethod = LinkMovementMethodExt.getInstance()
            text.text = StringUtil.fromHtml(reply.html)
            indentArrow.visibility = if (reply.depth > 0) View.VISIBLE else View.GONE
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
            (holder as TalkReplyHolder).bindItem(topic?.replies!![pos])
        }
    }

    companion object {
        const val EXTRA_TOPIC = "topicId"

        @JvmStatic
        fun newIntent(context: Context, topicId: Int): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(EXTRA_TOPIC, topicId)
        }
    }
}