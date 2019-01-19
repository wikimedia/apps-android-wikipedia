package org.wikipedia.editactionfeed

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_my_contributions.*
import kotlinx.android.synthetic.main.item_my_contributions_type_entry.view.*
import kotlinx.android.synthetic.main.item_my_contributions_type_header.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.ViewUtil

class MyContributionsFragment : Fragment() {

    private var myContributionsData: MyContributions = MyContributions()
    private var list = mutableListOf<Any>()
    private val adapter = WikipediaLanguageItemAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_contributions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: using the endpoint to update the information
        prepareMockData()
        setupList()
        myContributionsProgressView.update(myContributionsData.level!!, myContributionsData.editCount!!)
        setupRecyclerView()
    }

    // TODO: remove this once the endpoint is completed.
    private fun prepareMockData() {
        val typeList: MutableList<MyContributions.ContributionType> = mutableListOf()
        val mockList: MutableList<MyContributions.EditCount> = mutableListOf()

        var tempVar: MyContributions.EditCount = MyContributions.EditCount()
        tempVar.editCount = 3
        tempVar.languageCode = "en"
        mockList.add(tempVar)
        tempVar = MyContributions.EditCount()
        tempVar.editCount = 5
        tempVar.languageCode = "zh-hant"
        mockList.add(tempVar)
        tempVar = MyContributions.EditCount()
        tempVar.editCount = 2
        tempVar.languageCode = "ja"
        mockList.add(tempVar)

        val tempVar2: MyContributions.ContributionType = MyContributions.ContributionType()
        tempVar2.list = mockList
        tempVar2.typeCode = 1
        tempVar2.typeTitle = getString(R.string.editactionfeed_my_contributions_category_added_title_description)
        typeList.add(tempVar2)
        typeList.add(tempVar2)
        typeList.add(tempVar2)
        typeList.add(tempVar2)

        myContributionsData.level = 1
        myContributionsData.editCount = 5
        myContributionsData.list = typeList
    }

    private fun setupList() {
        for (type in myContributionsData.list!!) {
            list.add(type)
            for (entry in type.list!!) {
                list.add(entry)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
    }

    private inner class WikipediaLanguageItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        override fun getItemViewType(position: Int): Int {
            return if (list[position] is MyContributions.ContributionType) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_ITEM
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            val context = parent.context
            val inflater = LayoutInflater.from(context)

            return if (viewType == VIEW_TYPE_HEADER) {
                TypeHeaderViewHolder(inflater.inflate(R.layout.item_my_contributions_type_header, parent, false))
            } else {
                TypeEntryViewHolder(inflater.inflate(R.layout.item_my_contributions_type_entry, parent, false))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            if (holder is TypeHeaderViewHolder) {
                holder.bindItem(list[pos] as MyContributions.ContributionType)
            } else if (holder is TypeEntryViewHolder) {
                holder.bindItem(list[pos] as MyContributions.EditCount)
            }
        }
    }

    private inner class TypeHeaderViewHolder internal constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        internal fun bindItem(item: MyContributions.ContributionType) {
            view.typeTitle.text = item.typeTitle
        }
    }

    private inner class TypeEntryViewHolder internal constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        internal fun bindItem(item: MyContributions.EditCount) {
            ViewUtil.formatLangButton(itemView.languageCode, item.languageCode!!, LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER)
            itemView.languageCode.text = item.languageCode
            itemView.languageTitle.text = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(item.languageCode)
            itemView.editCount.text = item.editCount.toString()
        }
    }

    companion object {

        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        fun newInstance(): MyContributionsFragment {
            return MyContributionsFragment()
        }
    }
}
