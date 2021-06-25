package org.wikipedia.feed.configure

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedConfigureFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentFeedConfigureBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import java.util.*

class ConfigureFragment : Fragment(), ConfigureItemView.Callback {

    private var _binding: FragmentFeedConfigureBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var funnel: FeedConfigureFunnel
    private val orderedContentTypes = mutableListOf<FeedContentType>()
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedConfigureBinding.inflate(inflater, container, false)

        setupRecyclerView()
        funnel = FeedConfigureFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().wikiSite,
            requireActivity().intent.getIntExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, -1))

        disposables.add(ServiceFactory.getRest(WikiSite("wikimedia.org")).feedAvailability
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { prepareContentTypeList() }
            .subscribe({ result ->
                // apply the new availability rules to our content types
                FeedContentType.NEWS.langCodesSupported.clear()
                if (isLimitedToDomains(result.news!!)) {
                    addDomainNamesAsLangCodes(FeedContentType.NEWS.langCodesSupported, result.news!!)
                }
                FeedContentType.ON_THIS_DAY.langCodesSupported.clear()
                if (isLimitedToDomains(result.onThisDay!!)) {
                    addDomainNamesAsLangCodes(FeedContentType.ON_THIS_DAY.langCodesSupported, result.onThisDay!!)
                }
                FeedContentType.TOP_READ_ARTICLES.langCodesSupported.clear()
                if (isLimitedToDomains(result.mostRead!!)) {
                    addDomainNamesAsLangCodes(FeedContentType.TOP_READ_ARTICLES.langCodesSupported, result.mostRead!!)
                }
                FeedContentType.FEATURED_ARTICLE.langCodesSupported.clear()
                if (isLimitedToDomains(result.featuredArticle!!)) {
                    addDomainNamesAsLangCodes(FeedContentType.FEATURED_ARTICLE.langCodesSupported, result.featuredArticle!!)
                }
                FeedContentType.FEATURED_IMAGE.langCodesSupported.clear()
                if (isLimitedToDomains(result.featuredPicture!!)) {
                    addDomainNamesAsLangCodes(FeedContentType.FEATURED_IMAGE.langCodesSupported, result.featuredPicture!!)
                }
                FeedContentType.saveState()
            }) { caught -> L.e(caught) })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPause() {
        super.onPause()
        FeedContentType.saveState()
    }

    override fun onDestroyView() {
        disposables.clear()
        if (orderedContentTypes.isNotEmpty()) {
            funnel.done(orderedContentTypes)
        }
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_feed_configure, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_configure_select_all -> {
                FeedContentType.values().map { it.isEnabled = true }
                touch()
                binding.contentTypesRecycler.adapter?.notifyDataSetChanged()
                true
            }
            R.id.menu_feed_configure_deselect_all -> {
                FeedContentType.values().map { it.isEnabled = false }
                touch()
                binding.contentTypesRecycler.adapter?.notifyDataSetChanged()
                true
            }
            R.id.menu_feed_configure_reset -> {
                Prefs.resetFeedCustomizations()
                FeedContentType.restoreState()
                prepareContentTypeList()
                touch()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun prepareContentTypeList() {
        orderedContentTypes.clear()
        orderedContentTypes.addAll(FeedContentType.values())
        orderedContentTypes.sortWith { a, b -> a.order.compareTo(b.order) }
        // Remove items for which there are no available languages
        val i = orderedContentTypes.iterator()
        while (i.hasNext()) {
            val feedContentType = i.next()
            if (!feedContentType.showInConfig) {
                i.remove()
                continue
            }
            if (!AccountUtil.isLoggedIn && feedContentType === FeedContentType.SUGGESTED_EDITS) {
                i.remove()
                continue
            }
            val supportedLanguages = feedContentType.langCodesSupported
            if (supportedLanguages.isEmpty()) {
                continue
            }
            val atLeastOneSupported = WikipediaApp.getInstance().language().appLanguageCodes.any { supportedLanguages.contains(it) }
            if (!atLeastOneSupported) {
                i.remove()
            }
        }
        binding.contentTypesRecycler.adapter?.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        binding.contentTypesRecycler.setHasFixedSize(true)
        val adapter = ConfigureItemAdapter()
        binding.contentTypesRecycler.adapter = adapter
        binding.contentTypesRecycler.layoutManager = LinearLayoutManager(activity)
        binding.contentTypesRecycler.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable))
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.contentTypesRecycler)
    }

    override fun onCheckedChanged(contentType: FeedContentType, checked: Boolean) {
        touch()
        contentType.isEnabled = checked
    }

    override fun onLanguagesChanged(contentType: FeedContentType) {
        touch()
    }

    private fun updateItemOrder() {
        touch()
        for (i in orderedContentTypes.indices) {
            orderedContentTypes[i].order = i
        }
    }

    private fun touch() {
        requireActivity().setResult(SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED)
    }

    private inner class ConfigureItemHolder constructor(itemView: ConfigureItemView) : DefaultViewHolder<ConfigureItemView>(itemView) {
        fun bindItem(contentType: FeedContentType) {
            view.setContents(contentType)
        }
    }

    private inner class ConfigureItemAdapter : RecyclerView.Adapter<ConfigureItemHolder>() {

        override fun getItemCount(): Int {
            return orderedContentTypes.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ConfigureItemHolder {
            return ConfigureItemHolder(ConfigureItemView(requireContext()))
        }

        override fun onBindViewHolder(holder: ConfigureItemHolder, pos: Int) {
            holder.bindItem(orderedContentTypes[pos])
        }

        override fun onViewAttachedToWindow(holder: ConfigureItemHolder) {
            super.onViewAttachedToWindow(holder)
            holder.view.setDragHandleTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> itemTouchHelper.startDrag(holder)
                    MotionEvent.ACTION_UP -> v.performClick()
                }
                false
            }
            holder.view.callback = this@ConfigureFragment
        }

        override fun onViewDetachedFromWindow(holder: ConfigureItemHolder) {
            holder.view.callback = null
            holder.view.setDragHandleTouchListener(null)
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            Collections.swap(orderedContentTypes, oldPosition, newPosition)
            updateItemOrder()
            notifyItemMoved(oldPosition, newPosition)
        }
    }

    private inner class RearrangeableItemTouchHelperCallback constructor(private val adapter: ConfigureItemAdapter) : ItemTouchHelper.Callback() {

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.onMoveItem(source.absoluteAdapterPosition, target.absoluteAdapterPosition)
            return true
        }
    }

    companion object {
        private fun isLimitedToDomains(domainNames: List<String>): Boolean {
            return domainNames.isNotEmpty() && !domainNames[0].contains("*")
        }

        private fun addDomainNamesAsLangCodes(outList: MutableList<String>, domainNames: List<String>) {
            outList.addAll(domainNames.map { WikiSite(it).languageCode() })
        }

        fun newInstance(): ConfigureFragment {
            return ConfigureFragment()
        }
    }
}
