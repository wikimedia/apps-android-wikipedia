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
import kotlinx.android.synthetic.main.activity_talk_topics.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import kotlin.collections.ArrayList

class TalkTopicsActivity : BaseActivity() {
    private val disposables = CompositeDisposable()
    private val topics = ArrayList<TalkPage.Topic>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topics)
        title = getString(R.string.talk_user_title, AccountUtil.getUserName().orEmpty())

        talk_recycler_view.layoutManager = LinearLayoutManager(this)
        talk_recycler_view.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable))
        talk_recycler_view.adapter = TalkTopicItemAdapter()

        talk_new_topic_button.setOnClickListener {
            // TODO
        }

        talk_refresh_view.setOnRefreshListener {
            loadTopics()
        }

        talk_new_topic_button.visibility = View.GONE
        loadTopics()
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun loadTopics() {
        disposables.clear()
        talk_progress_bar.visibility = View.VISIBLE
        talk_error_view.visibility = View.GONE
        talk_empty_container.visibility = View.GONE

        ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getTalkPage(AccountUtil.getUserName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    topics.clear()
                    topics.addAll(response.topics!!)
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                })
    }

    private fun updateOnSuccess() {
        talk_progress_bar.visibility = View.GONE
        talk_error_view.visibility = View.GONE
        talk_new_topic_button.visibility = View.VISIBLE
        talk_refresh_view.isRefreshing = false

        talk_recycler_view.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(t: Throwable) {
        talk_new_topic_button.visibility = View.GONE
        talk_progress_bar.visibility = View.GONE
        talk_refresh_view.isRefreshing = false
        talk_error_view.visibility = View.VISIBLE
        talk_error_view.setError(t)
    }

    internal inner class TalkTopicHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val title: TextView = view.findViewById(R.id.topic_title_text)
        private var id: Int = 0

        fun bindItem(topic: TalkPage.Topic) {
            title.text = StringUtil.fromHtml(topic.html)
            itemView.setOnClickListener(this)
            id = topic.id
        }

        override fun onClick(v: View?) {
            startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, id))
        }
    }

    internal inner class TalkTopicItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return topics.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return TalkTopicHolder(layoutInflater.inflate(R.layout.item_talk_topic, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            (holder as TalkTopicHolder).bindItem(topics[pos])
        }
    }


    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
        }
    }
}