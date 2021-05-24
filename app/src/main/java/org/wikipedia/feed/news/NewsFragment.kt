package org.wikipedia.feed.news

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.databinding.FragmentNewsBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.*
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.TabUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentNewsBinding.inflate(inflater, container, false)

        appCompatActivity.setSupportActionBar(binding.toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar?.title = ""

        val item = GsonUnmarshaller.unmarshal(NewsItem::class.java,
            requireActivity().intent.getStringExtra(NewsActivity.EXTRA_NEWS_ITEM))
        val wiki = GsonUnmarshaller.unmarshal(WikiSite::class.java,
            requireActivity().intent.getStringExtra(NewsActivity.EXTRA_WIKI))

        L10nUtil.setConditionalLayoutDirection(binding.root, wiki.languageCode())

        binding.gradientView.background = GradientUtil.getPowerGradient(R.color.black54, Gravity.TOP)
        val imageUri = item.thumb()
        if (imageUri == null) {
            binding.appBarLayout.setExpanded(false, false)
        }
        binding.headerImageView.loadImage(imageUri)

        DeviceUtil.updateStatusBarTheme(requireActivity(), binding.toolbar, true)
        binding.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { layout, offset ->
            DeviceUtil.updateStatusBarTheme(requireActivity(), binding.toolbar,
                layout.totalScrollRange + offset > layout.totalScrollRange / 2)
            (requireActivity() as NewsActivity).updateNavigationBarColor()
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.toolbarContainer.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }

        binding.storyTextView.text = RichTextUtil.stripHtml(item.story())
        binding.linksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.linksRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(),
            R.attr.list_separator_drawable))
        binding.linksRecyclerView.isNestedScrollingEnabled = false
        binding.linksRecyclerView.adapter = RecyclerAdapter(item.linkCards(wiki), Callback())
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val appCompatActivity get() = requireActivity() as AppCompatActivity

    private class RecyclerAdapter constructor(items: List<NewsLinkCard>, private val callback: Callback) :
        DefaultRecyclerAdapter<NewsLinkCard, ListCardItemView>(items) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<ListCardItemView> {
            return DefaultViewHolder(ListCardItemView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<ListCardItemView>, position: Int) {
            val card = item(position)
            holder.view.setCard(card)
                .setHistoryEntry(HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_NEWS))
                .setCallback(callback)
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
                ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, InvokeSource.NEWS_ACTIVITY) { readingListId -> onMovePageToList(readingListId, entry) }
            } else {
                bottomSheetPresenter.show(childFragmentManager, AddToReadingListDialog.newInstance(entry.title, InvokeSource.NEWS_ACTIVITY))
            }
        }

        override fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
            bottomSheetPresenter.show(childFragmentManager, MoveToReadingListDialog.newInstance(sourceReadingListId, entry.title, InvokeSource.NEWS_ACTIVITY))
        }
    }

    companion object {
        fun newInstance(item: NewsItem, wiki: WikiSite): NewsFragment {
            return NewsFragment().apply {
                arguments = bundleOf(NewsActivity.EXTRA_NEWS_ITEM to GsonMarshaller.marshal(item),
                    NewsActivity.EXTRA_WIKI to GsonMarshaller.marshal(wiki))
            }
        }
    }
}
