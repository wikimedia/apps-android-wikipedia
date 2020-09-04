package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_talk_topics.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.FooterMarginItemDecoration

class TalkTopicsActivity : BaseActivity() {
    private var wikiSite: WikiSite = WikipediaApp.getInstance().wikiSite
    private var userName: String = ""
    private val disposables = CompositeDisposable()
    private val topics = ArrayList<TalkPage.Topic>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topics)

        if (intent.hasExtra(EXTRA_LANGUAGE)) {
            wikiSite = WikiSite.forLanguageCode(intent.getStringExtra(EXTRA_LANGUAGE).orEmpty())
        }
        userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        title = getString(R.string.talk_user_title, StringUtil.removeUnderscores(userName))

        talkRecyclerView.layoutManager = LinearLayoutManager(this)
        talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 80))
        talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        talkRecyclerView.adapter = TalkTopicItemAdapter()

        talkErrorView.setBackClickListener {
            finish()
        }

        talkNewTopicButton.setOnClickListener {
            // TODO
        }

        talkRefreshView.setOnRefreshListener {
            loadTopics()
        }

        talkNewTopicButton.visibility = View.GONE
        loadTopics()
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        loadTopics()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                val pos = data.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                if (pos < WikipediaApp.getInstance().language().appLanguageCodes.size) {
                    wikiSite = WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCodes[pos])
                    loadTopics()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_talk, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_change_language) {
            startActivityForResult(WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.TALK_ACTIVITY.getName()),
                    Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadTopics() {
        L10nUtil.setConditionalLayoutDirection(talkRefreshView, wikiSite.languageCode())

        disposables.clear()
        talkProgressBar.visibility = View.VISIBLE
        talkErrorView.visibility = View.GONE
        talkEmptyContainer.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(wikiSite).getTalkPage(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    talkProgressBar.visibility = View.GONE
                    talkRefreshView.isRefreshing = false
                }
                .subscribe({ response ->
                    topics.clear()
                    topics.addAll(response.topics!!)
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                }))
    }

    private fun updateOnSuccess() {
        talkErrorView.visibility = View.GONE
        talkNewTopicButton.show()
        talkRecyclerView.visibility - View.VISIBLE
        talkRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(t: Throwable) {
        topics.clear()
        talkRecyclerView.adapter?.notifyDataSetChanged()
        talkRecyclerView.visibility - View.GONE

        // In the case of 404, it just means that the talk page hasn't been created yet.
        if (t is HttpStatusException && t.code() == 404) {
            talkEmptyContainer.visibility = View.VISIBLE
            // Allow them to create a new topic anyway
            talkNewTopicButton.show()
        } else {
            talkNewTopicButton.hide()
            talkErrorView.visibility = View.VISIBLE
            talkErrorView.setError(t)
        }
    }

    internal inner class TalkTopicHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val title: TextView = view.findViewById(R.id.topicTitleText)
        private val subtitle: TextView = view.findViewById(R.id.topicSubtitleText)
        private val readDot: View = view.findViewById(R.id.topicReadDot)
        private var id: Int = 0

        fun bindItem(topic: TalkPage.Topic) {
            id = topic.id
            val titleStr = StringUtil.fromHtml(topic.html).toString().trim()
            if (id == 0 && titleStr.isEmpty() && topic.replies!!.isNotEmpty()) {
                subtitle.text = StringUtil.fromHtml(topic.replies!![0].html)
                title.visibility = View.GONE
                subtitle.visibility = View.VISIBLE
                readDot.visibility = View.GONE
            } else {
                title.text = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
                title.visibility = View.VISIBLE
                subtitle.visibility = View.GONE

                // TODO: implement read/unread topics
                readDot.visibility = View.VISIBLE
            }
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, wikiSite.languageCode(), userName, id))
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
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_USER_NAME = "userName"

        @JvmStatic
        fun newIntent(context: Context, language: String?, userName: String?): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                    .putExtra(EXTRA_LANGUAGE, language.orEmpty())
                    .putExtra(EXTRA_USER_NAME, userName.orEmpty())
        }
    }
}