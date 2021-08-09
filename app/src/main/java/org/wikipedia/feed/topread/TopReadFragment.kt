package org.wikipedia.feed.topread

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.databinding.FragmentMostReadBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.MoshiUtil
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.TabUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration

class TopReadFragment : Fragment() {

    private var _binding: FragmentMostReadBinding? = null
    private val binding get() = _binding!!
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMostReadBinding.inflate(inflater, container, false)

        val adapter = MoshiUtil.getDefaultMoshi().adapter(TopReadListCard::class.java)
        val card = adapter.fromJson(requireActivity().intent
            .getStringExtra(TopReadArticlesActivity.MOST_READ_CARD) ?: "null")!!

        appCompatActivity.setSupportActionBar(binding.toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar?.title = ""
        binding.toolbarTitle.text = getString(R.string.top_read_activity_title, card.subtitle())

        L10nUtil.setConditionalLayoutDirection(binding.root, card.wikiSite().languageCode())

        binding.mostReadRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.mostReadRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable))
        binding.mostReadRecyclerView.isNestedScrollingEnabled = false
        binding.mostReadRecyclerView.adapter = RecyclerAdapter(card.items(), Callback())

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val appCompatActivity get() = requireActivity() as AppCompatActivity

    private class RecyclerAdapter constructor(items: List<TopReadItemCard>, private val callback: Callback) :
        DefaultRecyclerAdapter<TopReadItemCard, ListCardItemView>(items) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<ListCardItemView> {
            return DefaultViewHolder(ListCardItemView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<ListCardItemView>, position: Int) {
            val card = item(position)
            holder.view.setCard(card).setHistoryEntry(HistoryEntry(card.pageTitle,
                HistoryEntry.SOURCE_FEED_MOST_READ_ACTIVITY)).setCallback(callback)
        }
    }

    private inner class Callback : ListCardItemView.Callback {
        override fun onSelectPage(card: Card, entry: HistoryEntry, openInNewBackgroundTab: Boolean) {
            if (openInNewBackgroundTab) {
                TabUtil.openInNewBackgroundTab(entry)
                FeedbackUtil.showMessage(requireActivity(), R.string.article_opened_in_background_tab)
            } else {
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
            }
        }

        override fun onSelectPage(card: Card, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>) {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), *sharedElements)
            val intent = PageActivity.newIntentForNewTab(requireContext(), entry, entry.title)
            if (sharedElements.isNotEmpty()) {
                intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true)
            }
            startActivity(intent, if (DimenUtil.isLandscape(requireContext()) || sharedElements.isEmpty()) null else options.toBundle())
        }

        override fun onAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
            if (addToDefault) {
                ReadingListBehaviorsUtil.addToDefaultList(requireActivity(),
                    entry.title, InvokeSource.MOST_READ_ACTIVITY) { readingListId -> onMovePageToList(readingListId, entry) }
            } else {
                bottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(entry.title, InvokeSource.MOST_READ_ACTIVITY))
            }
        }

        override fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
            bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(sourceReadingListId, entry.title, InvokeSource.MOST_READ_ACTIVITY))
        }
    }

    companion object {
        fun newInstance(card: TopReadItemCard): TopReadFragment {
            val adapter = MoshiUtil.getDefaultMoshi().adapter(TopReadItemCard::class.java)
            return TopReadFragment().apply {
                arguments = bundleOf(TopReadArticlesActivity.MOST_READ_CARD to adapter.toJson(card))
            }
        }
    }
}
