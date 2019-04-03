package org.wikipedia.editactionfeed

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_my_contributions.*
import kotlinx.android.synthetic.main.item_my_contributions.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.EditorTaskCounts
import org.wikipedia.notifications.NotificationEditorTasksHandler
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER
import org.wikipedia.search.SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.ViewUtil

class MyContributionsFragment : Fragment() {

    private val adapter = MyContributionsItemAdapter()
    private val disposables = CompositeDisposable()
    private var languageList = listOf<String>()
    private lateinit var editorTaskCounts: EditorTaskCounts

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_contributions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener { this.updateUI() }
        contributionsRecyclerView.setHasFixedSize(true)
        contributionsRecyclerView.adapter = adapter
        contributionsRecyclerView.layoutManager = LinearLayoutManager(activity)
        username.text = AccountUtil.getUserName()
        fetchUserContributions()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.menu_suggested_edits_tasks, menu)
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

    private fun updateUI() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun fetchUserContributions() {
        contributionsText.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    progressBar.visibility = View.GONE
                    contributionsText.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }
                .subscribe({ response ->
                    editorTaskCounts = response.query()!!.editorTaskCounts()!!
                    NotificationEditorTasksHandler.dispatchEditorTaskResults(requireContext(), editorTaskCounts)
                    val totalEdits = editorTaskCounts.descriptionEditsPerLanguage!!.values.sum()
                    languageList = editorTaskCounts.descriptionEditsPerLanguage!!.keys.toList()
                    contributionsText.text = resources.getQuantityString(R.plurals.edit_action_contribution_count, totalEdits, totalEdits)
                    adapter.notifyDataSetChanged()
                }, { throwable ->
                    L.e(throwable)
                    FeedbackUtil.showError(requireActivity(), throwable)
                }))
    }

    private inner class MyContributionsItemAdapter : RecyclerView.Adapter<ItemViewHolder>() {

        override fun getItemCount(): Int {
            return languageList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_my_contributions, parent, false))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, pos: Int) {
            holder.bindItem(languageList[pos])
        }
    }

    private inner class ItemViewHolder internal constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        internal fun bindItem(langCode: String) {
            ViewUtil.formatLangButton(itemView.languageCode, langCode, LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER)
            itemView.languageCode.text = langCode
            itemView.languageTitle.text = WikipediaApp.getInstance().language().getAppLanguageCanonicalName(langCode)
            itemView.editCount.text = editorTaskCounts.descriptionEditsPerLanguage!![langCode].toString()
        }
    }

    companion object {
        fun newInstance(): MyContributionsFragment {
            return MyContributionsFragment()
        }
    }
}
