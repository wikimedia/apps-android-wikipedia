package org.wikipedia.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchRecentBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.page.Namespace
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import java.util.concurrent.ConcurrentHashMap

class RecentSearchesFragment : Fragment() {
    interface Callback {
        fun switchToSearch(text: String)
        fun onAddLanguageClicked()
        fun getLangCode(): String
    }

    private var _binding: FragmentSearchRecentBinding? = null
    private val binding get() = _binding!!
    private val namespaceHints = listOf(Namespace.USER, Namespace.PORTAL, Namespace.HELP)
    private val namespaceMap = ConcurrentHashMap<String, Map<Namespace, String>>()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable -> L.e(throwable) }
    var callback: Callback? = null
    val recentSearchList = mutableListOf<RecentSearch>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchRecentBinding.inflate(inflater, container, false)

        binding.recentSearchesDeleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.clear_recent_searches_confirm))
                    .setPositiveButton(getString(R.string.clear_recent_searches_confirm_yes)) { _, _ ->
                        lifecycleScope.launch(coroutineExceptionHandler) {
                            withContext(Dispatchers.IO) {
                                AppDatabase.instance.recentSearchDao().deleteAll()
                            }
                            updateList()
                        }
                    }
                    .setNegativeButton(getString(R.string.clear_recent_searches_confirm_no), null)
                    .create().show()
        }
        binding.addLanguagesButton.setOnClickListener { onAddLangButtonClick() }
        binding.recentSearchesRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.namespacesRecycler.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        binding.recentSearchesRecycler.adapter = RecentSearchAdapter()
        val touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.recentSearchesRecycler)
        setButtonLongPressToast(binding.recentSearchesDeleteButton)
        return binding.root
    }

    fun show() {
        binding.recentSearchesContainer.visibility = View.VISIBLE
    }

    fun hide() {
        binding.recentSearchesContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateSearchEmptyView(searchesEmpty: Boolean) {
        if (searchesEmpty) {
            binding.searchEmptyContainer.visibility = View.VISIBLE
            if (WikipediaApp.instance.languageState.appLanguageCodes.size == 1) {
                binding.addLanguagesButton.visibility = View.VISIBLE
                binding.searchEmptyMessage.text = getString(R.string.search_empty_message_multilingual_upgrade)
            } else {
                binding.addLanguagesButton.visibility = View.GONE
                binding.searchEmptyMessage.text = getString(R.string.search_empty_message)
            }
        } else {
            binding.searchEmptyContainer.visibility = View.INVISIBLE
        }
    }

    private fun onAddLangButtonClick() {
        callback?.onAddLanguageClicked()
    }

    fun onLangCodeChanged() {
        lifecycleScope.launch(coroutineExceptionHandler) {
            updateList()
        }
    }

    suspend fun updateList() {
        val searches: List<RecentSearch>
        val nsMap: Map<String, MwQueryResult.Namespace>
        val langCode = callback?.getLangCode().orEmpty()

        if (!namespaceMap.containsKey(langCode)) {
            val map = mutableMapOf<Namespace, String>()
            namespaceMap[langCode] = map
            withContext(Dispatchers.IO) {
                nsMap = ServiceFactory.get(WikiSite.forLanguageCode(langCode)).getPageNamespaceWithSiteInfo(null).query?.namespaces.orEmpty()
                namespaceHints.forEach {
                    map[it] = nsMap[it.code().toString()]?.name.orEmpty()
                }
            }
        }

        withContext(Dispatchers.IO) {
            searches = AppDatabase.instance.recentSearchDao().getRecentSearches()
        }

        recentSearchList.clear()
        recentSearchList.addAll(searches)

        binding.namespacesRecycler.adapter = NamespaceAdapter()
        binding.recentSearchesRecycler.adapter?.notifyDataSetChanged()

        val searchesEmpty = recentSearchList.isEmpty()
        binding.searchEmptyContainer.isInvisible = !searchesEmpty
        updateSearchEmptyView(searchesEmpty)
        binding.recentSearches.isInvisible = searchesEmpty
    }

    private inner class RecentSearchItemViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, SwipeableItemTouchHelperCallback.Callback {
        private lateinit var recentSearch: RecentSearch

        fun bindItem(position: Int) {
            recentSearch = recentSearchList[position]
            itemView.setOnClickListener(this)
            (itemView as TextView).text = recentSearch.text
        }

        override fun onClick(v: View) {
            callback?.switchToSearch((v as TextView).text.toString())
        }

        override fun onSwipe() {
            lifecycleScope.launch(coroutineExceptionHandler) {
                withContext(Dispatchers.IO) {
                    AppDatabase.instance.recentSearchDao().delete(recentSearch)
                }
                updateList()
            }
        }

        override fun isSwipeable(): Boolean { return true }
    }

    private inner class RecentSearchAdapter : RecyclerView.Adapter<RecentSearchItemViewHolder>() {
        override fun getItemCount(): Int {
            return recentSearchList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchItemViewHolder {
            return RecentSearchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_recent, parent, false))
        }

        override fun onBindViewHolder(holder: RecentSearchItemViewHolder, pos: Int) {
            holder.bindItem(pos)
        }
    }

    private inner class NamespaceItemViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun bindItem(ns: Namespace?) {
            val isHeader = ns == null
            itemView.setOnClickListener(this)
            (itemView as TextView).text = if (isHeader) getString(R.string.search_namespaces) else namespaceMap[callback?.getLangCode()]?.get(ns).orEmpty() + ":"
            itemView.isEnabled = !isHeader
            itemView.setTextColor(ResourceUtil.getThemedColor(requireContext(), if (isHeader) R.attr.material_theme_primary_color else R.attr.colorAccent))
        }

        override fun onClick(v: View) {
            callback?.switchToSearch((v as TextView).text.toString())
        }
    }

    private inner class NamespaceAdapter : RecyclerView.Adapter<NamespaceItemViewHolder>() {
        override fun getItemCount(): Int {
            return namespaceHints.size + 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NamespaceItemViewHolder {
            return NamespaceItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_namespace, parent, false))
        }

        override fun onBindViewHolder(holder: NamespaceItemViewHolder, pos: Int) {
            holder.bindItem(namespaceHints.getOrNull(pos - 1))
        }
    }
}
