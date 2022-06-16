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
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import org.json.JSONException
import org.json.JSONObject
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ToCInteractionFunnel
import org.wikipedia.analytics.eventplatform.ArticleTocInteractionEvent
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.TalkTopicHolder
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.TalkTopicsViewModel
import org.wikipedia.util.*
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.FooterMarginItemDecoration
import org.wikipedia.views.ObservableWebView
import org.wikipedia.views.ObservableWebView.OnContentHeightChangedListener
import org.wikipedia.views.PageScrollerView
import org.wikipedia.views.SwipeableListView.OnSwipeOutListener

class SidePanelHandler internal constructor(private val fragment: PageFragment,
                                            private val bridge: CommunicationBridge) :
        ObservableWebView.OnClickListener, ObservableWebView.OnScrollChangeListener, OnContentHeightChangedListener {

    private val binding = (fragment.requireActivity() as PageActivity).binding
    private var talkViewModel: TalkTopicsViewModel? = null
    private val talkTopicsAdapter = TalkTopicItemAdapter()
    private val scrollerViewParams = FrameLayout.LayoutParams(DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE), DimenUtil.roundedDpToPx(SCROLLER_BUTTON_SIZE))
    private val webView = fragment.webView
    private val tocAdapter = ToCAdapter()
    private var rtl = false
    private var currentItemSelected = 0
    private var currentTalkSortMode = Prefs.talkTopicsSortMode
    private var funnel = ToCInteractionFunnel(WikipediaApp.instance, WikipediaApp.instance.wikiSite, 0, 0)
    private var articleTocInteractionEvent: ArticleTocInteractionEvent? = null

    private val sectionOffsetsCallback: ValueCallback<String> = ValueCallback { value ->
        if (!fragment.isAdded) {
            return@ValueCallback
        }
        try {
            val sections = JSONObject(value).getJSONArray("sections")
            for (i in 0 until sections.length()) {
                tocAdapter.setYOffset(sections.getJSONObject(i).getInt("id"),
                        sections.getJSONObject(i).getInt("yOffset"))
            }
            // artificially add height for bottom About section
            tocAdapter.setYOffset(ABOUT_SECTION_ID, webView.contentHeight)
        } catch (e: JSONException) {
            // ignore
        }
    }

    val isVisible get() = binding.navigationDrawer.isDrawerOpen(binding.sidePanelContainer)

    init {
        binding.tocList.adapter = tocAdapter
        binding.tocList.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val section = tocAdapter.getItem(position)
            scrollToSection(section)
            funnel.logClick()
            hide()
        }
        binding.tocList.listener = OnSwipeOutListener { hide() }
        webView.addOnClickListener(this)
        webView.addOnScrollChangeListener(this)
        webView.addOnContentHeightChangedListener(this)
        binding.pageScrollerButton.callback = ScrollerCallback()
        binding.navigationDrawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                super.onDrawerStateChanged(newState)
                if (!isVisible && newState == DrawerLayout.STATE_DRAGGING) {
                    onStartShow()
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                enableToCorTalkTopics(true)
            }
        })

        binding.talkTitleView.setOnClickListener { openTalkPage() }
        binding.talkFullscreenButton.setOnClickListener { openTalkPage() }
        binding.talkLastModified.setOnClickListener { _ ->
            talkViewModel?.let {
                fragment.startActivity(ArticleEditDetailsActivity.newIntent(fragment.requireContext(), it.pageTitle, it.lastRevision!!.revId))
            }
        }

        setScrollerPosition()
        enableToCorTalkTopics()
    }

    private fun setupTalkTopics(pageTitle: PageTitle) {
        binding.talkProgressBar.isVisible = true
        binding.talkErrorView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.GONE

        binding.talkRecyclerView.layoutManager = LinearLayoutManager(fragment.requireContext())
        binding.talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 120))
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(fragment.requireContext(), R.attr.side_panel_list_separator_drawable, drawStart = true, drawEnd = false))
        binding.talkRecyclerView.adapter = talkTopicsAdapter
        binding.talkErrorView.backClickListener = View.OnClickListener {
            hide()
        }

        if (talkViewModel == null) {
            talkViewModel = TalkTopicsViewModel(pageTitle.copy(), true)

            fragment.lifecycleScope.launchWhenCreated {
                talkViewModel?.uiState?.collect {
                    when (it) {
                        is TalkTopicsViewModel.UiState.LoadTopic -> updateOnSuccess(it.pageTitle)
                        is TalkTopicsViewModel.UiState.LoadError -> updateOnError(it.throwable)
                    }
                }
            }
        } else {
            talkViewModel?.updatePageTitle(pageTitle)
        }
    }

    private fun updateOnSuccess(pageTitle: PageTitle) {
        binding.talkTitleView.text = StringUtil.fromHtml(pageTitle.displayText)

        talkViewModel?.lastRevision?.let {
            binding.talkLastModified.text = StringUtil.fromHtml(fragment.getString(R.string.talk_last_modified,
                DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(it.timeStamp).time,
                    System.currentTimeMillis(), 0L), it.user))
            binding.talkLastModified.isVisible = true
        }

        binding.talkErrorView.visibility = View.GONE
        binding.talkProgressBar.visibility = View.GONE
        binding.talkRecyclerView.visibility = View.VISIBLE
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateOnError(throwable: Throwable) {
        binding.talkRecyclerView.visibility = View.GONE
        if (throwable is HttpStatusException && throwable.code == 404) {
            binding.talkEmptyContainer.visibility = View.VISIBLE
        } else {
            binding.talkLastModified.visibility = View.GONE
            binding.talkErrorView.visibility = View.VISIBLE
            binding.talkErrorView.setError(throwable)
        }
    }

    private fun openTalkPage() {
        talkViewModel?.let {
            fragment.startActivity(TalkTopicsActivity.newIntent(fragment.requireContext(), it.pageTitle, Constants.InvokeSource.PAGE_ACTIVITY))
        }
    }

    @SuppressLint("RtlHardcoded")
    fun setupForNewPage(page: Page) {
        tocAdapter.setPage(page)
        rtl = L10nUtil.isLangRTL(page.title.wikiSite.languageCode)
        binding.tocList.rtl = rtl
        L10nUtil.setConditionalLayoutDirection(binding.sidePanelContainer, page.title.wikiSite.languageCode)
        binding.sidePanelContainer.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = if (rtl) Gravity.LEFT else Gravity.RIGHT
        }
        log()
        funnel = ToCInteractionFunnel(WikipediaApp.instance, page.title.wikiSite, page.pageProperties.pageId, tocAdapter.count)
        articleTocInteractionEvent = ArticleTocInteractionEvent(page.pageProperties.pageId, page.title.wikiSite.dbName(), tocAdapter.count)
        articleTocInteractionEvent?.logClick()
        if (ReleaseUtil.isPreBetaRelease) {
            setupTalkTopics(page.title)
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
        articleTocInteractionEvent?.scrollStart()
    }

    private fun enableToCorTalkTopics(enableToC: Boolean = true) {
        binding.tocContainer.isVisible = enableToC
        binding.talkTopicsContainer.isVisible = !enableToC

        if (!enableToC && currentTalkSortMode != Prefs.talkTopicsSortMode) {
            currentTalkSortMode = Prefs.talkTopicsSortMode
            talkTopicsAdapter.notifyDataSetChanged()
        }
    }

    fun showToC() {
        enableToCorTalkTopics(true)
        binding.navigationDrawer.openDrawer(binding.sidePanelContainer)
        onStartShow()
    }

    fun showTalkTopics() {
        enableToCorTalkTopics(false)
        binding.navigationDrawer.openDrawer(binding.sidePanelContainer)
        onStartShow()
    }

    fun hide() {
        binding.navigationDrawer.closeDrawers()
        funnel.scrollStop()
        articleTocInteractionEvent?.scrollStop()
    }

    fun log() {
        funnel.log()
        articleTocInteractionEvent?.logEvent()
    }

    fun setEnabled(enabled: Boolean) {
        if (binding.talkTopicsContainer.isVisible) {
            binding.navigationDrawer.setSlidingEnabled(true)
            return
        }
        if (enabled) {
            setScrollerPosition()
            binding.navigationDrawer.setSlidingEnabled(true)
        } else {
            binding.navigationDrawer.closeDrawers()
            binding.navigationDrawer.setSlidingEnabled(false)
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
        binding.pageScrollerButton.layoutParams = scrollerViewParams
    }

    private fun scrollToListSectionByOffset(yOffset: Int) {
        var newYOffset = yOffset
        newYOffset = DimenUtil.roundedPxToDp(newYOffset.toFloat())
        var itemToSelect = 0
        for (i in 1 until tocAdapter.count) {
            val section = tocAdapter.getItem(i)
            itemToSelect = if (tocAdapter.getYOffset(section.id) < newYOffset) {
                i
            } else {
                break
            }
        }
        if (itemToSelect != currentItemSelected) {
            tocAdapter.setHighlightedSection(itemToSelect)
            currentItemSelected = itemToSelect
        }
        binding.tocList.smoothScrollToPositionFromTop(currentItemSelected,
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
        override fun getItemCount(): Int {
            return talkViewModel?.sortedThreadItems?.size ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): TalkTopicHolder {
            return TalkTopicHolder(ItemTalkTopicBinding.inflate(fragment.layoutInflater, parent, false),
                fragment.requireContext(), talkViewModel!!, Constants.InvokeSource.PAGE_ACTIVITY)
        }

        override fun onBindViewHolder(holder: TalkTopicHolder, pos: Int) {
             holder.bindItem(talkViewModel!!.sortedThreadItems[pos])
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
