package org.wikipedia.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.fragment_watchlist.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration

class WatchlistFragment : Fragment() {
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar!!.title = "Foo."

        watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        watchlistRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable))
        //watchlistRecyclerView.adapter = (RecyclerAdapter(card.items()));

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }




    internal inner class RecyclerAdapter(tasks: List<SuggestedEditsTask>) : DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView>(tasks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<SuggestedEditsTaskView> {
            return DefaultViewHolder(SuggestedEditsTaskView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<SuggestedEditsTaskView>, i: Int) {
            holder.view.setUpViews(items()[i], callback)
        }
    }





    private class RecyclerAdapter(items: List<*>) : DefaultRecyclerAdapter<Any?, Any?>(items) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<Any?> {
            return DefaultViewHolder<ListCardItemView>(ListCardItemView(parent.context))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
    }

    private inner class Callback : ListCardItemView.Callback {
        override fun onSelectPage(card: Card, entry: HistoryEntry) {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
        }

        override fun onAddPageToList(entry: HistoryEntry) {
            bottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(entry.title, InvokeSource.MOST_READ_ACTIVITY))
        }

        override fun onRemovePageFromList(entry: HistoryEntry) {
            FeedbackUtil.showMessage(requireActivity(),
                    getString(R.string.reading_list_item_deleted, entry.title.displayText))
        }

        override fun onSharePage(entry: HistoryEntry) {
            ShareUtil.shareText(activity, entry.title)
        }
    }

    companion object {
        fun newInstance(): WatchlistFragment {
            return WatchlistFragment()
        }
    }
}