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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchRecentBinding
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.log.L
import org.wikipedia.views.SwipeableItemTouchHelperCallback

class RecentSearchesFragment : Fragment() {
    interface Callback {
        fun switchToSearch(text: String)
        fun onAddLanguageClicked()
    }

    private var _binding: FragmentSearchRecentBinding? = null
    private val binding get() = _binding!!
    var callback: Callback? = null
    var recentSearchList = mutableListOf<RecentSearch>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchRecentBinding.inflate(inflater, container, false)

        binding.recentSearchesDeleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.clear_recent_searches_confirm))
                    .setPositiveButton(getString(R.string.clear_recent_searches_confirm_yes)) { _, _ ->
                        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable -> L.e(throwable) }) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recentSearchesRecycler.adapter = RecentSearchAdapter()
        lifecycleScope.launch { updateList() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateSearchEmptyView(searchesEmpty: Boolean) {
        if (searchesEmpty) {
            binding.searchEmptyContainer.visibility = View.VISIBLE
            if (WikipediaApp.getInstance().language().appLanguageCodes.size == 1) {
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

    suspend fun updateList() {
        try {
            val searches = withContext(Dispatchers.IO) {
                AppDatabase.instance.recentSearchDao().getRecentSearches()
            }
            recentSearchList.clear()
            recentSearchList.addAll(searches)
            binding.recentSearchesRecycler.adapter?.notifyDataSetChanged()

            val searchesEmpty = recentSearchList.isEmpty()
            binding.searchEmptyContainer.isInvisible = !searchesEmpty
            updateSearchEmptyView(searchesEmpty)
            binding.recentSearches.isInvisible = searchesEmpty
        } catch (t: Throwable) {
            L.e(t)
        }
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
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.instance.recentSearchDao().delete(recentSearch)
                }
                updateList()
            }
        }
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
}
