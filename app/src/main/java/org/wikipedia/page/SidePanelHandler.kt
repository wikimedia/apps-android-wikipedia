package org.wikipedia.page

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.format.DateUtils
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.FixedDrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import org.json.JSONException
import org.json.JSONObject
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ToCInteractionFunnel
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.TalkTopicHolder
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.TalkTopicsProvider
import org.wikipedia.util.*
import org.wikipedia.views.*
import org.wikipedia.views.ObservableWebView.OnContentHeightChangedListener
import org.wikipedia.views.SwipeableListView.OnSwipeOutListener

class SidePanelHandler internal constructor(private val fragment: PageFragment,
                                            private val drawerLayout: FixedDrawerLayout,
                                            private val scrollerView: PageScrollerView,
                                            private val bridge: CommunicationBridge) :
        ObservableWebView.OnClickListener, ObservableWebView.OnScrollChangeListener, OnContentHeightChangedListener {

    private val containerView = drawerLayout.findViewById<FrameLayout>(R.id.side_panel_container)
    private val tocContainerView = drawerLayout.findViewById<FrameLayout>(R.id.toc_container)
    private val talkTopicsContainerView = drawerLayout.findViewById<FrameLayout>(R.id.talk_topic_container)
    private val tocList = drawerLayout.findViewById<SwipeableListView>(R.id.toc_list)
    private val talkTitleView = drawerLayout.findViewById<TextView>(R.id.talk_title_view)
    private val talkFullscreenButton = drawerLayout.findViewById<ImageView>(R.id.tall_fullscreen_button)
    private val talkRecyclerView = drawerLayout.findViewById<RecyclerView>(R.id.talk_recycler_view)
    private val talkEmptyContainer = drawerLayout.findViewById<LinearLayout>(R.id.talk_empty_container)
    private val talkErrorView = drawerLayout.findViewById<WikiErrorView>(R.id.talk_error_view)
    private val talkProgressBar = drawerLayout.findViewById<ProgressBar>(R.id.talk_progress_bar)
    private val talkLastModified = drawerLayout.findViewById<MaterialTextView>(R.id.talk_last_modified)
    private val talkTopicsAdapter = TalkTopicItemAdapter()
    private val scrollerViewParams = FrameLayout.LayoutParams(DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE), DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE))
    private val webView = fragment.webView
    private val adapter = ToCAdapter()
    private var rtl = false
    private var currentItemSelected = 0
    private var currentTalkSortMode = Prefs.talkTopicsSortMode
    private var funnel = ToCInteractionFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().wikiSite, 0, 0)

    private val sectionOffsetsCallback: ValueCallback<String> = ValueCallback { value ->
        if (!fragment.isAdded) {
            return@ValueCallback
        }
        try {
            val sections = JSONObject(value).getJSONArray("sections")
            for (i in 0 until sections.length()) {
                adapter.setYOffset(sections.getJSONObject(i).getInt("id"),
                        sections.getJSONObject(i).getInt("yOffset"))
            }
            // artificially add height for bottom About section
            adapter.setYOffset(ABOUT_SECTION_ID, webView.contentHeight)
        } catch (e: JSONException) {
            // ignore
        }
    }

    val isVisible get() = drawerLayout.isDrawerOpen(containerView)

    init {
        tocList.adapter = adapter
        tocList.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val section = adapter.getItem(position)
            scrollToSection(section)
            funnel.logClick()
            hide()
        }
        tocList.listener = OnSwipeOutListener { hide() }
        webView.addOnClickListener(this)
        webView.addOnScrollChangeListener(this)
        webView.addOnContentHeightChangedListener(this)
        scrollerView.callback = ScrollerCallback()
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                super.onDrawerStateChanged(newState)
                if (!isVisible && newState == DrawerLayout.STATE_DRAGGING) {
                    onStartShow()
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                enableToCContainer(true)
            }
        })
        setScrollerPosition()
        enableToCContainer()
    }

    fun setupTalkTopics(pageTitle: PageTitle) {
        val newPageTitle = pageTitle.copy()

        talkProgressBar.isVisible = true
        talkErrorView.visibility = View.GONE
        talkEmptyContainer.visibility = View.GONE

        talkRecyclerView.layoutManager = LinearLayoutManager(fragment.requireContext())
        talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 120))
        talkRecyclerView.addItemDecoration(DrawableItemDecoration(fragment.requireContext(), R.attr.side_panel_list_separator_drawable, drawStart = true, drawEnd = false))
        talkRecyclerView.adapter = talkTopicsAdapter
        talkErrorView.backClickListener = View.OnClickListener {
            hide()
        }

        TalkTopicsProvider(newPageTitle).load(object : TalkTopicsProvider.Callback {
            override fun onUpdatePageTitle(title: PageTitle) {
                talkTitleView.text = StringUtil.fromHtml(title.displayText)
                talkTitleView.setOnClickListener(openTalkPageOnClickListener(title))
                talkFullscreenButton.setOnClickListener(openTalkPageOnClickListener(title))
            }

            override fun onReceivedRevision(revision: MwQueryPage.Revision?) {
                revision?.let {
                    talkLastModified.text = StringUtil.fromHtml(fragment.getString(R.string.talk_last_modified,
                        DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(revision.timeStamp).time,
                            System.currentTimeMillis(), 0L), revision.user))
                    talkLastModified.isVisible = true
                    talkLastModified.setOnClickListener { _ ->
                        fragment.startActivity(ArticleEditDetailsActivity.newIntent(fragment.requireContext(), pageTitle.displayText, it.revId, pageTitle.wikiSite.languageCode))
                    }
                }
            }

            override fun onSuccess(talkPage: TalkPage) {
                talkTopicsAdapter.pageTitle = newPageTitle
                talkTopicsAdapter.topics.clear()
                talkTopicsAdapter.topics.addAll(talkPage.topics!!)
                talkErrorView.visibility = View.GONE
                talkRecyclerView.visibility = View.VISIBLE
                talkRecyclerView.adapter?.notifyDataSetChanged()
            }

            override fun onError(throwable: Throwable) {
                talkRecyclerView.visibility = View.GONE
                if (throwable is HttpStatusException && throwable.code == 404) {
                    talkEmptyContainer.visibility = View.VISIBLE
                } else {
                    talkLastModified.visibility = View.GONE
                    talkErrorView.visibility = View.VISIBLE
                    talkErrorView.setError(throwable)
                }
            }

            override fun onFinished() {
                talkProgressBar.visibility = View.GONE
            }

            private fun openTalkPageOnClickListener(title: PageTitle): View.OnClickListener {
                return View.OnClickListener {
                    fragment.startActivity(TalkTopicsActivity.newIntent(fragment.requireContext(), title, Constants.InvokeSource.PAGE_ACTIVITY))
                }
            }
        })
    }

    @SuppressLint("RtlHardcoded")
    fun setupToC(page: Page?, wiki: WikiSite) {
        page?.let {
            adapter.setPage(it)
            rtl = L10nUtil.isLangRTL(wiki.languageCode)
            tocList.rtl = rtl
            L10nUtil.setConditionalLayoutDirection(containerView, wiki.languageCode)
            containerView.updateLayoutParams<DrawerLayout.LayoutParams> {
                gravity = if (rtl) Gravity.LEFT else Gravity.RIGHT
            }
            log()
            funnel = ToCInteractionFunnel(WikipediaApp.getInstance(), wiki, it.pageProperties.pageId, adapter.count)
        }
    }

    private fun scrollToSection(section: Section?) {
        section?.let {
            when {
                it.isLead -> webView.scrollY = 0
                it.id == ABOUT_SECTION_ID -> bridge.execute(JavaScriptActionHandler.scrollToFooter(webView.context))
                else -> bridge.execute(JavaScriptActionHandler.prepareToScrollTo(it.anchor, false))
            }
        }
    }

    private fun onStartShow() {
        currentItemSelected = -1
        onScrollerMoved(0f, false)
        funnel.scrollStart()
    }

    private fun enableToCContainer(isToC: Boolean = true) {
        tocContainerView.isVisible = isToC
        talkTopicsContainerView.isVisible = !isToC

        if (!isToC && currentTalkSortMode != Prefs.talkTopicsSortMode) {
            currentTalkSortMode = Prefs.talkTopicsSortMode
            talkTopicsAdapter.notifyDataSetChanged()
        }
    }

    fun show(isToC: Boolean = true) {
        enableToCContainer(isToC)
        drawerLayout.openDrawer(containerView)
        onStartShow()
    }

    fun hide() {
        drawerLayout.closeDrawers()
        funnel.scrollStop()
    }

    fun log() {
        funnel.log()
    }

    fun setEnabled(enabled: Boolean) {
        if (talkTopicsContainerView.isVisible) {
            drawerLayout.setSlidingEnabled(true)
            return
        }
        if (enabled) {
            setScrollerPosition()
            drawerLayout.setSlidingEnabled(true)
        } else {
            drawerLayout.closeDrawers()
            drawerLayout.setSlidingEnabled(false)
        }
    }

    override fun onClick(x: Float, y: Float): Boolean {
        if (isVisible) {
            hide()
        }
        return false
    }

    override fun onScrollChanged(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean) {
        setScrollerPosition()
    }

    override fun onContentHeightChanged(contentHeight: Int) {
        if (fragment.isLoading) {
            return
        }
        bridge.evaluate(JavaScriptActionHandler.getOffsets(), sectionOffsetsCallback)
    }

    inner class ToCAdapter : BaseAdapter() {
        private val sections = ArrayList<Section>()
        private val sectionYOffsets = SparseIntArray()
        private var highlightedSection = 0
        fun setPage(page: Page) {
            sections.clear()
            sectionYOffsets.clear()
            sections.addAll(page.sections.filter { it.level < MAX_LEVELS })
            // add a fake section at the end to represent the "about this article" contents at the bottom:
            sections.add(Section(ABOUT_SECTION_ID, 1,
                    L10nUtil.getStringForArticleLanguage(page.title, R.string.about_article_section),
                    L10nUtil.getStringForArticleLanguage(page.title, R.string.about_article_section), ""))
            highlightedSection = 0
            notifyDataSetChanged()
        }

        fun setHighlightedSection(id: Int) {
            highlightedSection = id
            notifyDataSetChanged()
        }

        fun getYOffset(id: Int): Int {
            return sectionYOffsets[id, 0]
        }

        fun setYOffset(id: Int, yOffset: Int) {
            sectionYOffsets.put(id, yOffset)
        }

        override fun getCount(): Int {
            return sections.size
        }

        override fun getItem(position: Int): Section {
            return sections[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var newConvertView = convertView
            if (newConvertView == null) {
                newConvertView = LayoutInflater.from(parent.context).inflate(R.layout.item_toc_entry, parent, false)
            }
            val section = getItem(position)
            val sectionHeading = newConvertView!!.findViewById<TextView>(R.id.page_toc_item_text)
            val sectionBullet = newConvertView.findViewById<View>(R.id.page_toc_item_bullet)
            sectionHeading.text = StringUtil.fromHtml(section.title)
            var textSize = TOC_SUBSECTION_TEXT_SIZE
            when {
                section.isLead -> {
                    textSize = TOC_LEAD_TEXT_SIZE
                    sectionHeading.typeface = Typeface.SERIF
                }
                section.level == 1 -> {
                    textSize = TOC_SECTION_TEXT_SIZE
                    sectionHeading.typeface = Typeface.SERIF
                }
                else -> sectionHeading.typeface = Typeface.SANS_SERIF
            }
            sectionHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            sectionBullet.updateLayoutParams<LinearLayout.LayoutParams> {
                topMargin = DimenUtil.roundedDpToPx(textSize / 2)
            }
            if (highlightedSection == position) {
                sectionHeading.setTextColor(ResourceUtil.getThemedColor(fragment.requireContext(), R.attr.colorAccent))
            } else {
                if (section.level > 1) {
                    sectionHeading.setTextColor(
                            ResourceUtil.getThemedColor(fragment.requireContext(), R.attr.primary_text_color))
                } else {
                    sectionHeading.setTextColor(
                            ResourceUtil.getThemedColor(fragment.requireContext(), R.attr.toc_h1_h2_color))
                }
            }
            return newConvertView
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun setScrollerPosition() {
        scrollerViewParams.gravity = if (rtl) Gravity.LEFT else Gravity.RIGHT
        scrollerViewParams.leftMargin = if (rtl) DimenUtil.roundedDpToPx(SCROLLER_BUTTON_REVEAL_MARGIN) else 0
        scrollerViewParams.rightMargin = if (rtl) 0 else DimenUtil.roundedDpToPx(SCROLLER_BUTTON_REVEAL_MARGIN)
        val toolbarHeight = DimenUtil.getToolbarHeightPx(fragment.requireContext())
        scrollerViewParams.topMargin = (toolbarHeight + (webView.height - 2 * toolbarHeight) *
                (webView.scrollY.toFloat() / webView.contentHeight.toFloat() / DimenUtil.densityScalar)).toInt()
        if (scrollerViewParams.topMargin < toolbarHeight) {
            scrollerViewParams.topMargin = toolbarHeight
        }
        scrollerView.layoutParams = scrollerViewParams
    }

    private fun scrollToListSectionByOffset(yOffset: Int) {
        var newYOffset = yOffset
        newYOffset = DimenUtil.roundedPxToDp(newYOffset.toFloat())
        var itemToSelect = 0
        for (i in 1 until adapter.count) {
            val section = adapter.getItem(i)
            itemToSelect = if (adapter.getYOffset(section.id) < newYOffset) {
                i
            } else {
                break
            }
        }
        if (itemToSelect != currentItemSelected) {
            adapter.setHighlightedSection(itemToSelect)
            currentItemSelected = itemToSelect
        }
        tocList.smoothScrollToPositionFromTop(currentItemSelected,
                scrollerViewParams.topMargin - DimenUtil.roundedDpToPx(TOC_SECTION_TOP_OFFSET_ADJUST), 0)
    }

    private fun onScrollerMoved(dy: Float, scrollWebView: Boolean) {
        val webViewScrollY = webView.scrollY
        val webViewHeight = webView.height
        val webViewContentHeight = webView.contentHeight * DimenUtil.densityScalar
        var scrollY = webViewScrollY.toFloat()
        scrollY += dy * webViewContentHeight / (webViewHeight - 2 * DimenUtil.getToolbarHeightPx(fragment.requireContext())).toFloat()
        if (scrollY < 0) {
            scrollY = 0f
        } else if (scrollY > webViewContentHeight - webViewHeight) {
            scrollY = webViewContentHeight - webViewHeight
        }
        if (scrollWebView) {
            webView.scrollTo(0, scrollY.toInt())
        }
        scrollToListSectionByOffset(scrollY.toInt() + webViewHeight / 2)
    }

    private inner class ScrollerCallback : PageScrollerView.Callback {
        override fun onClick() {}
        override fun onScrollStart() {}
        override fun onScrollStop() {
            hide()
        }

        override fun onVerticalScroll(dy: Float) {
            onScrollerMoved(dy, true)
        }
    }

    inner class TalkTopicItemAdapter : RecyclerView.Adapter<TalkTopicHolder>() {

        lateinit var pageTitle: PageTitle
        var topics = mutableListOf<TalkPage.Topic>()

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): TalkTopicHolder {
            return TalkTopicHolder(ItemTalkTopicBinding.inflate(fragment.layoutInflater, parent, false),
                fragment.requireContext(), pageTitle, Constants.InvokeSource.PAGE_ACTIVITY)
        }

        override fun onBindViewHolder(holder: TalkTopicHolder, pos: Int) {
            holder.bindItem(list[pos])
        }

        private val list get(): List<TalkPage.Topic> {
            when (Prefs.talkTopicsSortMode) {
                TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_DESCENDING -> {
                    topics.sortByDescending { it.id }
                }
                TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_ASCENDING -> {
                    topics.sortBy { it.id }
                }
                TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_DESCENDING -> {
                    topics.sortByDescending { RichTextUtil.stripHtml(it.html) }
                }
                TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_ASCENDING -> {
                    topics.sortBy { RichTextUtil.stripHtml(it.html) }
                }
            }
            return topics
        }
    }

    companion object {
        private const val SCROLLER_BUTTON_SIZE = 44f
        private const val SCROLLER_BUTTON_REVEAL_MARGIN = 12f
        private const val TOC_LEAD_TEXT_SIZE = 24f
        private const val TOC_SECTION_TEXT_SIZE = 18f
        private const val TOC_SUBSECTION_TEXT_SIZE = 14f
        private const val TOC_SECTION_TOP_OFFSET_ADJUST = 70f
        private const val MAX_LEVELS = 3
        private const val ABOUT_SECTION_ID = -1
    }
}
