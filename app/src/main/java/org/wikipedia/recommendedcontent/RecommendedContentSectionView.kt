package org.wikipedia.recommendedcontent

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.metricsplatform.ExperimentalLinkPreviewInteraction
import org.wikipedia.analytics.metricsplatform.RecommendedContentAnalyticsHelper
import org.wikipedia.databinding.ItemRecommendedContentSectionTextBinding
import org.wikipedia.databinding.ViewRecommendedContentSectionBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.util.StringUtil

class RecommendedContentSectionView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewRecommendedContentSectionBinding.inflate(LayoutInflater.from(context), this, true)
    private var analyticsEvent: ExperimentalLinkPreviewInteraction? = null

    fun buildContent(wikiSite: WikiSite, pageSummaries: List<PageSummary>, analyticsEvent: ExperimentalLinkPreviewInteraction?) {
        this.analyticsEvent = analyticsEvent
        binding.sectionHeader.text = context.getString(R.string.recommended_content_section_you_might_like)
        binding.sectionList.layoutManager = LinearLayoutManager(context)
        binding.sectionList.adapter = RecyclerViewAdapter(pageSummaries, wikiSite)
    }

    private inner class RecyclerViewAdapter(val list: List<PageSummary>, val wikiSite: WikiSite) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSectionTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(list[position], wikiSite)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemRecommendedContentSectionTextBinding) :
        RecyclerView.ViewHolder(binding.root), OnClickListener {

        private lateinit var pageSummary: PageSummary
        private lateinit var wikiSite: WikiSite

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem(pageSummary: PageSummary, wikiSite: WikiSite) {
            this.pageSummary = pageSummary
            this.wikiSite = wikiSite
            binding.listItem.text = StringUtil.fromHtml(pageSummary.displayTitle)
        }

        override fun onClick(v: View) {
            val source = if (RecommendedContentAnalyticsHelper.abcTest.group == ABTest.GROUP_2) {
                HistoryEntry.SOURCE_RECOMMENDED_CONTENT_GENERALIZED
            } else {
                HistoryEntry.SOURCE_RECOMMENDED_CONTENT_PERSONALIZED
            }
            val entry = HistoryEntry(pageSummary.getPageTitle(wikiSite), source)
            context.startActivity(PageActivity.newIntentForNewTab(context, entry, entry.title))
            analyticsEvent?.let {
                it.source = source
                it.logNavigate()
            }
        }
    }
}
