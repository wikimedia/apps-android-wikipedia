package org.wikipedia.feed.topread

import android.app.ActivityOptions
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.databinding.FragmentMostReadBinding
import org.wikipedia.extensions.setLayoutDirectionByLang
import org.wikipedia.feed.view.ListItemView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.TabUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration

class TopReadFragment : Fragment() {

    private var _binding: FragmentMostReadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TopReadViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMostReadBinding.inflate(inflater, container, false)

        val card = viewModel.card

        (requireActivity() as AppCompatActivity).run {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.top_read_activity_title, DateUtil.getShortDateString(card.articles.localDate))
        }

        binding.root.setLayoutDirectionByLang(card.site.languageCode)

        binding.mostReadRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.mostReadRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider))
        binding.mostReadRecyclerView.isNestedScrollingEnabled = false
        binding.mostReadRecyclerView.adapter = RecyclerAdapter(card.articles.articles.map { it.getPageTitle(card.site) }, Callback())

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private class RecyclerAdapter(items: List<PageTitle>, private val callback: Callback) :
        DefaultRecyclerAdapter<PageTitle, ListItemView>(items) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<ListItemView> {
            return DefaultViewHolder(ListItemView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<ListItemView>, position: Int) {
            val title = item(position)
            holder.view.setPageTitle(title).setHistoryEntry(HistoryEntry(title,
                HistoryEntry.SOURCE_FEED_MOST_READ_ACTIVITY)).setCallback(callback)
        }
    }

    private inner class Callback : ListItemView.Callback {
        override fun onSelectPage(title: PageTitle, entry: HistoryEntry, openInNewBackgroundTab: Boolean) {
            if (openInNewBackgroundTab) {
                TabUtil.openInNewBackgroundTab(entry)
                FeedbackUtil.showMessage(requireActivity(), R.string.article_opened_in_background_tab)
            } else {
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
            }
        }

        override fun onSelectPage(title: PageTitle, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>) {
            val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), *sharedElements)
            val intent = PageActivity.newIntentForNewTab(requireContext(), entry, entry.title)
            if (sharedElements.isNotEmpty()) {
                intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true)
            }
            startActivity(intent, if (DimenUtil.isLandscape(requireContext()) || sharedElements.isEmpty()) null else options.toBundle())
        }

        override fun onAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, addToDefault, InvokeSource.MOST_READ_ACTIVITY)
        }

        override fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
            ReadingListBehaviorsUtil.moveToList(requireActivity(), sourceReadingListId, entry.title, InvokeSource.MOST_READ_ACTIVITY)
        }
    }

    companion object {
        fun newInstance(card: TopReadCard): TopReadFragment {
            return TopReadFragment().apply {
                arguments = bundleOf(TopReadArticlesActivity.TOP_READ_CARD to card)
            }
        }
    }
}
