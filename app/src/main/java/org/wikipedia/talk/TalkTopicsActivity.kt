package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.ActivityTalkTopicsBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.util.*

class TalkTopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityTalkTopicsBinding
    private lateinit var pageTitle: PageTitle
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var funnel: TalkFunnel
    private lateinit var notificationButtonView: NotificationButtonView
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()
    private val disposables = CompositeDisposable()
    private val topics = mutableListOf<TalkPage.Topic>()
    private val unreadTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private var revisionForLastEdit: MwQueryPage.Revision? = null
    private var resolveTitleRequired = false
    private var goToTopic = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goToTopic = intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)
        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 120))
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false, skipSearchBar = true))
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
        binding.talkLastModified.setOnClickListener {
            revisionForLastEdit?.let {
                startActivity(ArticleEditDetailsActivity.newIntent(this, pageTitle.displayText, it.revId, pageTitle.wikiSite.languageCode))
            }
        }
        notificationButtonView = NotificationButtonView(this)
        Prefs.hasAnonymousNotification = false

        // Determine whether we need to resolve the PageTitle, since the calling activity might
        // have given us a non-Talk page, and we need to prepend the correct namespace.
        if (pageTitle.namespace.isEmpty()) {
            pageTitle.namespace = TalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.isUserPage) {
            pageTitle.namespace = UserTalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.namespace() != Namespace.TALK && pageTitle.namespace() != Namespace.USER_TALK) {
            // defer resolution of Talk page title for an API call.
            resolveTitleRequired = true
        }
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
            val topic = data?.getIntExtra(TalkTopicActivity.EXTRA_TOPIC, 0) ?: -1
            val undoneSubject = data?.getStringExtra(TalkTopicActivity.EXTRA_SUBJECT) ?: ""
            val undoneText = data?.getStringExtra(TalkTopicActivity.EXTRA_BODY) ?: ""
            if (newRevisionId > 0) {
                FeedbackUtil.makeSnackbar(this, getString(R.string.talk_new_topic_submitted), FeedbackUtil.LENGTH_DEFAULT)
                    .setAnchorView(binding.talkNewTopicButton)
                    .setAction(R.string.talk_snackbar_undo) {
                        binding.talkNewTopicButton.isEnabled = false
                        binding.talkNewTopicButton.alpha = 0.5f
                        binding.talkProgressBar.visibility = View.VISIBLE
                        undoSave(newRevisionId, topic, undoneSubject, undoneText)
                    }
                    .show()
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_GO_TO_TOPIC_ACTIVITY && resultCode == TalkTopicActivity.RESULT_BACK_FROM_TOPIC) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!goToTopic) {
            menuInflater.inflate(R.menu.menu_talk, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (!goToTopic) {
            menu!!.findItem(R.id.menu_change_language).isVisible = pageTitle.namespace() == Namespace.USER_TALK
            menu.findItem(R.id.menu_view_user_page).isVisible = pageTitle.namespace() == Namespace.USER_TALK
            val notificationMenuItem = menu.findItem(R.id.menu_notifications)
            if (AccountUtil.isLoggedIn) {
                notificationMenuItem.isVisible = true
                notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
                notificationButtonView.setOnClickListener {
                    if (AccountUtil.isLoggedIn) {
                        startActivity(NotificationActivity.newIntent(this))
                    }
                }
                notificationButtonView.contentDescription =
                    getString(R.string.notifications_activity_title)
                notificationMenuItem.actionView = notificationButtonView
                notificationMenuItem.expandActionView()
                FeedbackUtil.setButtonLongPressToast(notificationButtonView)
            } else {
                notificationMenuItem.isVisible = false
            }
            updateNotificationDot(false)
        }
        return super.onPrepareOptionsMenu(menu)
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
                val entry = HistoryEntry(PageTitle(UserAliasData.valueFor(pageTitle.wikiSite.languageCode) + ":" + pageTitle.text, pageTitle.wikiSite), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                return true
            }
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_talk_page), pageTitle.uri)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationDot(true)
    }

    private fun loadTopics() {
        invalidateOptionsMenu()
        L10nUtil.setConditionalLayoutDirection(binding.talkRefreshView, pageTitle.wikiSite.languageCode)
        binding.talkUsernameView.text = StringUtil.fromHtml(pageTitle.displayText)
        binding.talkUsernameView.isVisible = !goToTopic

        disposables.clear()
        binding.talkProgressBar.isVisible = true
        binding.talkErrorView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.GONE

        disposables.add(if (resolveTitleRequired) { ServiceFactory.get(pageTitle.wikiSite).getPageNamespaceWithSiteInfo(pageTitle.prefixedText) } else { Observable.just(MwQueryResponse()) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    resolveTitleRequired = false
                    response.query?.namespaces?.let { namespaces ->
                        response.query?.firstPage()?.let { page ->
                            // In MediaWiki, namespaces that are even-numbered are "regular" pages,
                            // and namespaces that are odd-numbered are the "Talk" versions of the
                            // corresponding even-numbered namespace. For example, "User"=2, "User talk"=3.
                            // So then, if the namespace of our pageTitle is even (i.e. not a Talk page),
                            // then increment the namespace by 1, and update the pageTitle with it.
                            val newNs = namespaces.values.find { it.id == page.namespace().code() + 1 }
                            if (page.namespace().code() % 2 == 0 && newNs != null) {
                                pageTitle.namespace = newNs.name
                            }
                        }
                    }
                    binding.talkUsernameView.text = StringUtil.fromHtml(pageTitle.displayText)
                    ServiceFactory.get(pageTitle.wikiSite).getLastModified(pageTitle.prefixedText)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    it.query?.firstPage()?.revisions?.getOrNull(0)?.let { revision ->
                        revisionForLastEdit = revision
                        binding.talkLastModified.text = StringUtil.fromHtml(getString(R.string.talk_last_modified,
                            DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(revision.timeStamp).time,
                                System.currentTimeMillis(), 0L), revision.user))
                    }
                    ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    invalidateOptionsMenu()
                    binding.talkUsernameView.isVisible = !goToTopic
                    binding.talkProgressBar.isVisible = !goToTopic
                    binding.talkProgressBar.visibility = View.GONE
                    binding.talkRefreshView.isRefreshing = false
                }
                .subscribe({
                    topics.clear()
                    topics.addAll(it.topics!!)
                    updateOnSuccess()
                }, {
                    L.e(it)
                    updateOnError(it)
                }))
    }

    private fun updateOnSuccess() {
        if (intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)) {
            intent.putExtra(EXTRA_GO_TO_TOPIC, false)
            var topic: TalkPage.Topic? = null
            if (!pageTitle.fragment.isNullOrEmpty()) {
                val targetTopic = UriUtil.parseTalkTopicFromFragment(pageTitle.fragment.orEmpty())
                topic = topics.find {
                    StringUtil.addUnderscores(targetTopic) == StringUtil.addUnderscores(it.html)
                }
            }
            if (topic != null) {
                startActivityForResult(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, topic.id, invokeSource),
                        Constants.ACTIVITY_REQUEST_GO_TO_TOPIC_ACTIVITY)
                overridePendingTransition(0, 0)
                return
            }
        }
        goToTopic = false
        if (topics.isEmpty()) {
            updateOnEmpty()
        } else {
            binding.talkErrorView.visibility = View.GONE
            binding.talkNewTopicButton.show()
            binding.talkNewTopicButton.isEnabled = true
            binding.talkNewTopicButton.alpha = 1.0f
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
            invalidateOptionsMenu()
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

    private fun undoSave(newRevisionId: Long, topicId: Int, undoneSubject: String, undoneBody: String) {
        disposables.add(CsrfTokenClient(pageTitle.wikiSite).token
            .subscribeOn(Schedulers.io())
            .flatMap { token -> ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(pageTitle.prefixedText, newRevisionId, token) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, topicId, invokeSource, undoneSubject, undoneBody))
            }, {
                updateOnError(it)
            }))
    }

    fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    internal inner class TalkTopicHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val title: TextView = view.findViewById(R.id.topicTitleText)
        private val subtitle: TextView = view.findViewById(R.id.topicSubtitleText)
        private var id: Int = 0

        fun bindItem(topic: TalkPage.Topic) {
            id = topic.id
            val seen = AppDatabase.getAppDatabase().talkPageSeenDao().getTalkPageSeen(topic.getIndicatorSha()) != null
            var titleStr = RichTextUtil.stripHtml(topic.html).trim()
            if (titleStr.isEmpty()) {
                // build up a title based on the contents, massaging the html into plain text that
                // flows over a few lines...
                topic.replies?.firstOrNull()?.let {
                    titleStr = RichTextUtil.stripHtml(it.html).replace("\n", " ")
                    if (titleStr.length > MAX_CHARS_NO_SUBJECT) {
                        titleStr = titleStr.substring(0, MAX_CHARS_NO_SUBJECT) + "…"
                    }
                }
            }

            title.text = titleStr.ifEmpty { getString(R.string.talk_no_subject) }
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

        private val listPlaceholder get() = if (actionMode == null) 1 else 0

        override fun getItemCount(): Int {
            return topics.size + listPlaceholder
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 && listPlaceholder == 1) ITEM_SEARCH_BAR else ITEM_TOPIC
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            if (type == ITEM_SEARCH_BAR) {
                return TalkTopicSearcherHolder(layoutInflater.inflate(R.layout.view_talk_topic_search_bar, parent, false))
            }
            return TalkTopicHolder(layoutInflater.inflate(R.layout.item_talk_topic, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is TalkTopicHolder) {
                holder.bindItem(topics[pos - listPlaceholder])
            }
        }
    }

    private inner class TalkTopicSearcherHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val talkSortButton: AppCompatImageView = itemView.findViewById(R.id.talk_sort_button)

        init {
            (itemView as WikiCardView).setCardBackgroundColor(ResourceUtil.getThemedColor(this@TalkTopicsActivity, R.attr.color_group_22))

            itemView.setOnClickListener {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                }
            }

            talkSortButton.setOnClickListener {
                // TODO: show overflow menu
            }

            FeedbackUtil.setButtonLongPressToast(talkSortButton)
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchActionProvider: SearchActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchActionProvider = SearchActionProvider(this@TalkTopicsActivity, searchHintString,
                object : SearchActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchActionProvider)

            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            // TODO: update list
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            binding.talkRecyclerView.adapter?.notifyItemRangeInserted(0, 1)
        }

        override fun getSearchHintString(): String {
            return getString(R.string.talk_search_or_sort_hint)
        }

        override fun getParentContext(): Context {
            return this@TalkTopicsActivity
        }
    }

    companion object {
        private const val ITEM_SEARCH_BAR = 0
        private const val ITEM_TOPIC = 1
        private const val EXTRA_PAGE_TITLE = "pageTitle"
        private const val EXTRA_GO_TO_TOPIC = "goToTopic"
        private const val MAX_CHARS_NO_SUBJECT = 100
        const val NEW_TOPIC_ID = -2

        @JvmStatic
        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                .putExtra(EXTRA_GO_TO_TOPIC, !pageTitle.fragment.isNullOrEmpty())
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
