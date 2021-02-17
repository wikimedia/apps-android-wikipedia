package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.FooterMarginItemDecoration
import java.util.*
import kotlin.collections.ArrayList

class TalkTopicsActivity : BaseActivity() {
    private lateinit var pageTitle: PageTitle
    private val disposables = CompositeDisposable()
    private val topics = ArrayList<TalkPage.Topic>()
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var funnel: TalkFunnel

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_topics)

        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        talkRecyclerView.layoutManager = LinearLayoutManager(this)
        talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 80))
        talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        talkRecyclerView.adapter = TalkTopicItemAdapter()

        talkErrorView.setBackClickListener {
            finish()
        }
        talkErrorView.setRetryClickListener {
            loadTopics()
        }

        talkNewTopicButton.setOnClickListener {
            funnel.logNewTopicClick()
            startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, -1, invokeSource))
        }

        talkRefreshView.setOnRefreshListener {
            funnel.logRefresh()
            loadTopics()
        }

        invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
        funnel = TalkFunnel(pageTitle, invokeSource)
        funnel.logOpenTalk()

        talkNewTopicButton.visibility = View.GONE
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()
        loadTopics()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                val pos = data.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                if (pos < WikipediaApp.getInstance().language().appLanguageCodes.size) {
                    funnel.logChangeLanguage()

                    val newNamespace = when {
                        pageTitle.namespace() == Namespace.USER -> {
                            UserAliasData.valueFor(WikipediaApp.getInstance().language().appLanguageCodes[pos])
                        }
                        pageTitle.namespace() == Namespace.USER_TALK -> {
                            UserTalkAliasData.valueFor(WikipediaApp.getInstance().language().appLanguageCodes[pos])
                        }
                        else -> pageTitle.namespace
                    }

                    pageTitle = PageTitle(newNamespace, StringUtil.removeNamespace(pageTitle.prefixedText),
                            WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCodes[pos]))
                    loadTopics()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_talk, menu)
        menu!!.findItem(R.id.menu_change_language).isVisible = pageTitle.namespace() == Namespace.USER_TALK
        menu.findItem(R.id.menu_view_user_page).isVisible = pageTitle.namespace() == Namespace.USER_TALK
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_change_language) {
            startActivityForResult(WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.TALK_ACTIVITY),
                    Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
            return true
        } else if (item.itemId == R.id.menu_view_in_browser) {
            UriUtil.visitInExternalBrowser(this, Uri.parse(pageTitle.uri))
            return true
        } else if (item.itemId == R.id.menu_view_user_page) {
            val entry = HistoryEntry(PageTitle(UserAliasData.valueFor(pageTitle.wikiSite.languageCode()) + ":" + pageTitle.text, pageTitle.wikiSite), HistoryEntry.SOURCE_TALK_TOPIC)
            startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadTopics() {
        invalidateOptionsMenu()
        L10nUtil.setConditionalLayoutDirection(talkRefreshView, pageTitle.wikiSite.languageCode())
        talkUsernameView.text = StringUtil.fromHtml(pageTitle.displayText)

        disposables.clear()
        talkProgressBar.visibility = View.VISIBLE
        talkErrorView.visibility = View.GONE
        talkEmptyContainer.visibility = View.GONE

        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    talkProgressBar.visibility = View.GONE
                    talkRefreshView.isRefreshing = false
                }
                .subscribe({ response ->
                    topics.clear()
                    for (topic in response.topics!!) {
                        if (topic.id == 0 && topic.html!!.trim().isEmpty()) {
                            continue
                        }
                        topics.add(topic)
                    }
                    updateOnSuccess()
                }, { t ->
                    L.e(t)
                    updateOnError(t)
                }))
    }

    private fun updateOnSuccess() {
        if (topics.isEmpty()) {
            updateOnEmpty()
        } else {
            talkErrorView.visibility = View.GONE
            talkNewTopicButton.show()
            talkRecyclerView.visibility = View.VISIBLE
            talkRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateOnError(t: Throwable) {
        topics.clear()
        talkRecyclerView.adapter?.notifyDataSetChanged()
        talkRecyclerView.visibility = View.GONE

        // In the case of 404, it just means that the talk page hasn't been created yet.
        if (t is HttpStatusException && t.code() == 404) {
            updateOnEmpty()
        } else {
            talkNewTopicButton.hide()
            talkErrorView.visibility = View.VISIBLE
            talkErrorView.setError(t)
        }
    }

    private fun updateOnEmpty() {
        talkRecyclerView.visibility = View.GONE
        talkEmptyContainer.visibility = View.VISIBLE
        // Allow them to create a new topic anyway
        talkNewTopicButton.show()
    }

    internal inner class TalkTopicHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val title: TextView = view.findViewById(R.id.topicTitleText)
        private val subtitle: TextView = view.findViewById(R.id.topicSubtitleText)
        private val readDot: View = view.findViewById(R.id.topicReadDot)
        private var id: Int = 0

        fun bindItem(topic: TalkPage.Topic) {
            id = topic.id
            val seen = TalkPageSeenDatabaseTable.isTalkTopicSeen(topic)
            val titleStr = StringUtil.fromHtml(topic.html).toString().trim()
            title.text = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
            title.visibility = View.VISIBLE
            subtitle.visibility = View.GONE
            readDot.visibility = if (seen) View.GONE else View.VISIBLE
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, id, invokeSource))
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
        private const val EXTRA_PAGE_TITLE = "pageTitle"

        @JvmStatic
        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
