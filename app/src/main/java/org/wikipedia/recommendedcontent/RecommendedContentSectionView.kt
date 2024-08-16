package org.wikipedia.recommendedcontent

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemRecommendedContentSectionTextBinding
import org.wikipedia.databinding.ViewRecommendedContentSectionBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.util.StringUtil

class RecommendedContentSectionView(context: Context) : FrameLayout(context) {

    private val binding = ViewRecommendedContentSectionBinding.inflate(LayoutInflater.from(context), this, true)

    fun buildContent(section: RecommendedContentSection, pageSummaries: List<PageSummary>) {
        binding.sectionHeader.text = context.getString(section.titleResId)
        binding.sectionMoreButton.setOnClickListener {
            // TODO: implement
        }
        binding.sectionList.layoutManager = LinearLayoutManager(context)
        binding.sectionList.adapter = RecyclerViewAdapter(pageSummaries)
    }

    private inner class RecyclerViewAdapter(val list: List<PageSummary>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSectionTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(list[position])
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemRecommendedContentSectionTextBinding) :
        RecyclerView.ViewHolder(binding.root), OnClickListener {

        private lateinit var pageSummary: PageSummary

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem(pageSummary: PageSummary) {
            this.pageSummary = pageSummary
            binding.listItem.text = StringUtil.fromHtml(pageSummary.displayTitle)
        }

        override fun onClick(v: View) {
            val entry = HistoryEntry(pageSummary.getPageTitle(WikipediaApp.instance.wikiSite), HistoryEntry.SOURCE_RECOMMENDED_CONTENT)
            context.startActivity(PageActivity.newIntentForNewTab(context, entry, entry.title))
        }
    }
}
