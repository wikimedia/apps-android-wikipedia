package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.ActivityTalkTopicsBinding
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
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.FooterMarginItemDecoration
import java.util.*
import kotlin.collections.ArrayList

class TalkTopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityTalkTopicsBinding
    private lateinit var pageTitle: PageTitle
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var funnel: TalkFunnel
    private val disposables = CompositeDisposable()
    private val topics = ArrayList<TalkPage.Topic>()
    private val unreadTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 120))
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        binding.talkRecyclerView.adapter = TalkTopicItemAdapter()

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            loadTopics()
        }

        binding.talkNewTopicButton.setOnClickListener {
            funnel.logNewTopicClick()
            startActivityForResult(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, NEW_TOPIC_ID, invokeSource),
                Constants.ACTIVITY_REQUEST_NEW_TOPIC_ACTIVITY)
        }

        binding.talkRefreshView.setOnRefreshListener {
            funnel.logRefresh()
            loadTopics()
        }
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent))

        invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
        funnel = TalkFunnel(pageTitle, invokeSource)
        funnel.logOpenTalk()

        binding.talkNewTopicButton.visibility = View.GONE
        binding.talkLastModified.visibility = View.GONE
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
        } else if (requestCode == Constants.ACTIVITY_REQUEST_NEW_TOPIC_ACTIVITY && resultCode == TalkTopicActivity.RESULT_EDIT_SUCCESS) {
            val newRevisionId = data?.getLongExtra(TalkTopicActivity.RESULT_NEW_REVISION_ID, 0) ?: 0
            if (newRevisionId > 0) {
                FeedbackUtil.makeSnackbar(this, getString(R.string.talk_new_topic_submitted), FeedbackUtil.LENGTH_DEFAULT)
                    .setAnchorView(binding.talkNewTopicButton)
                    .setAction(R.string.talk_snackbar_undo) {
                        binding.talkNewTopicButton.hide()
                        binding.talkProgressBar.visibility = View.VISIBLE
                        undoSave(newRevisionId)
                    }
                    .show()
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
        when (item.itemId) {
            R.id.menu_change_language -> {
                startActivityForResult(WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.TALK_ACTIVITY),
                    Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
                return true
            }
            R.id.menu_view_in_browser -> {
                UriUtil.visitInExternalBrowser(this, Uri.parse(pageTitle.uri))
                return true
            }
            R.id.menu_view_user_page -> {
                val entry = HistoryEntry(PageTitle(UserAliasData.valueFor(pageTitle.wikiSite.languageCode()) + ":" + pageTitle.text, pageTitle.wikiSite), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun loadTopics(newRevision: Long = 0) {
        invalidateOptionsMenu()
        L10nUtil.setConditionalLayoutDirection(binding.talkRefreshView, pageTitle.wikiSite.languageCode())
        binding.talkUsernameView.text = StringUtil.fromHtml(pageTitle.displayText)

        disposables.clear()
        binding.talkProgressBar.visibility = View.VISIBLE
        binding.talkErrorView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.GONE

        disposables.add(ServiceFactory.get(pageTitle.wikiSite).getLastModified(pageTitle.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    it.query?.firstPage()?.revisions()?.getOrNull(0)?.let { revision ->
                        binding.talkLastModified.text = StringUtil.fromHtml(getString(R.string.talk_last_modified,
                            DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(revision.timeStamp()).time,
                                System.currentTimeMillis(), 0L), revision.user))
                    }
                    ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    binding.talkProgressBar.visibility = View.GONE
                    binding.talkRefreshView.isRefreshing = false
                }
                .map { response ->
                    if (newRevision != 0L && response.revision < newRevision) {
                        throw IllegalStateException()
                    }
                    response
                }
                .retry(20) { t ->
                    (t is IllegalStateException) || (t is HttpStatusException && t.code == 404)
                }
                .subscribe({ response ->
                    topics.clear()
                    for (topic in response.topics!!) {
                        if (topic.id == 0 && topic.html!!.trim().isEmpty()) {
                            continue
                        }
                        L.d("loadTopics add  " + topic.html)
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
            binding.talkErrorView.visibility = View.GONE
            binding.talkNewTopicButton.show()
            binding.talkLastModified.visibility = View.VISIBLE
            binding.talkRecyclerView.visibility = View.VISIBLE
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateOnError(t: Throwable) {
        topics.clear()
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        binding.talkRecyclerView.visibility = View.GONE

        // In the case of 404, it just means that the talk page hasn't been created yet.
        if (t is HttpStatusException && t.code == 404) {
            updateOnEmpty()
        } else {
            binding.talkNewTopicButton.hide()
            binding.talkLastModified.visibility = View.GONE
            binding.talkErrorView.visibility = View.VISIBLE
            binding.talkErrorView.setError(t)
        }
    }

    private fun updateOnEmpty() {
        binding.talkRecyclerView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.VISIBLE
        binding.talkLastModified.visibility = View.GONE
        // Allow them to create a new topic anyway
        binding.talkNewTopicButton.show()
    }

    private fun undoSave(newRevisionId: Long) {
        disposables.add(CsrfTokenClient(pageTitle.wikiSite).token
            .subscribeOn(Schedulers.io())
            .flatMap { token -> ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(pageTitle.prefixedText, newRevisionId, token) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loadTopics(it.edit!!.newRevId)
            }, {
                updateOnError(it)
            }))
    }

    internal inner class TalkTopicHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val title: TextView = view.findViewById(R.id.topicTitleText)
        private val subtitle: TextView = view.findViewById(R.id.topicSubtitleText)
        private var id: Int = 0

        fun bindItem(topic: TalkPage.Topic) {
            id = topic.id
            val seen = AppDatabase.getAppDatabase().talkPageSeenDao().getTalkPageSeen(topic.getIndicatorSha()) != null
            val titleStr = StringUtil.fromHtml(topic.html).toString().trim()
            title.text = if (titleStr.isNotEmpty()) titleStr else getString(R.string.talk_no_subject)
            title.visibility = View.VISIBLE
            subtitle.visibility = View.GONE
            title.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
            title.setTextColor(ResourceUtil.getThemedColor(this@TalkTopicsActivity,
                    if (seen) android.R.attr.textColorTertiary else R.attr.material_theme_primary_color))
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
        const val NEW_TOPIC_ID = -2

        @JvmStatic
        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
