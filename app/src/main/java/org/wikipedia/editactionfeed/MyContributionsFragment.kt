package org.wikipedia.editactionfeed

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import kotlinx.android.synthetic.main.fragment_my_contributions.*
import kotlinx.android.synthetic.main.item_my_contributions.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.ViewUtil

class MyContributionsFragment : Fragment() {

    private var myContributionsData: MyContributions = MyContributions()
    private var list = mutableListOf<Any>()
    private val adapter = MyContributionsItemAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_contributions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: using the endpoint to update the information
        prepareMockData()
        setupList()
        setupProgressData()
        setupRecyclerView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        // TODO: use exclamation mark icon
        inflater!!.inflate(R.menu.menu_my_contributions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // TODO: remove this once the endpoint is completed.
    private fun prepareMockData() {
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

        myContributionsData.list = mockList
    }

    private fun setupProgressData() {
        username.text = AccountUtil.getUserName()
        contributionsText.text = resources.getQuantityString(R.plurals.edit_action_contribution_count,
                Prefs.getTotalUserDescriptionsEdited(), Prefs.getTotalUserDescriptionsEdited())
    }

    private fun setupList() {
        for (counts in myContributionsData.list!!) {
            list.add(counts)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
    }

    private inner class MyContributionsItemAdapter : RecyclerView.Adapter<ItemViewHolder>() {

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_my_contributions, parent, false))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, pos: Int) {
            holder.bindItem(list[pos] as MyContributions.EditCount)
        }
    }

    private inner class ItemViewHolder internal constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        internal fun bindItem(item: MyContributions.EditCount) {
            ViewUtil.formatLangButton(itemView.languageCode, item.languageCode!!, LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER)
            itemView.languageCode.text = item.languageCode
            itemView.languageTitle.text = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(item.languageCode)
            itemView.editCount.text = item.editCount.toString()
        }
    }

    companion object {
        fun newInstance(): MyContributionsFragment {
            return MyContributionsFragment()
        }
    }
}
