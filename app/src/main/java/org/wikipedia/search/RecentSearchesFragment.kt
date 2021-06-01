package org.wikipedia.search

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchRecentBinding
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.log.L

class RecentSearchesFragment : Fragment() {
    interface Callback {
        fun switchToSearch(text: String)
        fun onAddLanguageClicked()
    }

    private var _binding: FragmentSearchRecentBinding? = null
    private val binding get() = _binding!!
    var callback: Callback? = null
    //private lateinit var adapter: RecentSearchesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchRecentBinding.inflate(inflater, container, false)

        binding.recentSearchesDeleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.clear_recent_searches_confirm))
                    .setPositiveButton(getString(R.string.clear_recent_searches_confirm_yes)) { _, _ ->
                        Completable.fromAction {
                            WikipediaApp.getInstance().getDatabaseClient(RecentSearch::class.java).deleteAll()
                        }
                                .subscribeOn(Schedulers.io()).subscribe()
                    }
                    .setNegativeButton(getString(R.string.clear_recent_searches_confirm_no), null)
                    .create().show()
        }
        binding.addLanguagesButton.setOnClickListener { onAddLangButtonClick() }
        setButtonLongPressToast(binding.recentSearchesDeleteButton)
        return binding.root
    }

    fun show() {
        binding.recentSearchesContainer.visibility = View.VISIBLE
    }

    fun hide() {
        binding.recentSearchesContainer.visibility = View.GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //adapter = RecentSearchesAdapter(requireContext(), null, true)
        //binding.recentSearchesList.adapter = adapter
        binding.recentSearchesList.onItemClickListener = OnItemClickListener { _, view: View, _, _ ->
            val entry = view.tag as RecentSearch
            callback?.switchToSearch(entry.text!!)
        }




        Observable.fromCallable {
            AppDatabase.getAppDatabase().recentSearchDao().getRecentSearches()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { searches ->
                searches.forEach {
                    L.d(it.text.orEmpty())
                }
            }
    }

    override fun onDestroyView() {
        LoaderManager.getInstance(this).destroyLoader(Constants.RECENT_SEARCHES_FRAGMENT_LOADER_ID)
        _binding = null
        super.onDestroyView()
    }

    /*
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        //val uri = SearchHistoryContract.Query.URI
        //val order = SearchHistoryContract.Query.ORDER_MRU
        return CursorLoader(requireContext(), uri, null, null, null, order)
    }

    override fun onLoadFinished(cursorLoaderLoader: Loader<Cursor>, cursorLoader: Cursor) {
        if (!isAdded) {
            return
        }
        //adapter.swapCursor(cursorLoader)
        val searchesEmpty = binding.recentSearchesList.count == 0
        binding.searchEmptyContainer.visibility = if (searchesEmpty) View.VISIBLE else View.INVISIBLE
        updateSearchEmptyView(searchesEmpty)
        binding.recentSearches.visibility = if (!searchesEmpty) View.VISIBLE else View.INVISIBLE
    }
     */

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

    fun onAddLangButtonClick() {
        callback?.onAddLanguageClicked()
    }

    fun updateList() {
        //adapter.notifyDataSetChanged()
    }

    /*
    private inner class RecentSearchesAdapter(context: Context, c: Cursor?, autoRequery: Boolean) : CursorAdapter(context, c, autoRequery) {
        override fun newView(context: Context, cursor: Cursor, viewGroup: ViewGroup): View {
            return LayoutInflater.from(activity).inflate(R.layout.item_search_recent, viewGroup, false)
        }

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val textView = view as TextView
            val entry = getEntry(cursor)
            textView.text = entry.text
            view.setTag(entry)
        }

        override fun convertToString(cursor: Cursor): CharSequence {
            return getEntry(cursor).text!!
        }

        fun getEntry(cursor: Cursor): RecentSearch {
            return RecentSearch.DATABASE_TABLE.fromCursor(cursor)
        }
    }
     */
}
