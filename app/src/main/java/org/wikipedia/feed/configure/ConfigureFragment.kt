package org.wikipedia.feed.configure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentFeedConfigureBinding
import org.wikipedia.feed.FeedContentType
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import java.util.Collections

class ConfigureFragment : Fragment(), MenuProvider, ConfigureItemView.Callback {

    private var _binding: FragmentFeedConfigureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConfigureViewModel by viewModels()

    private lateinit var itemTouchHelper: ItemTouchHelper
    private val orderedContentTypes = mutableListOf<FeedContentType>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedConfigureBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupRecyclerView()

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        FeedContentType.saveState()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> prepareContentTypeList()
                            is Resource.Error -> onError(it.throwable)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_feed_configure, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_configure_select_all -> {
                FeedContentType.entries.map { it.isEnabled = true }
                touch()
                binding.contentTypesRecycler.adapter?.notifyItemRangeChanged(0, orderedContentTypes.size)
                true
            }
            R.id.menu_feed_configure_deselect_all -> {
                FeedContentType.entries.map { it.isEnabled = false }
                touch()
                binding.contentTypesRecycler.adapter?.notifyItemRangeChanged(0, orderedContentTypes.size)
                true
            }
            R.id.menu_feed_configure_reset -> {
                Prefs.resetFeedCustomizations()
                FeedContentType.restoreState()
                prepareContentTypeList()
                touch()
                true
            }
            else -> false
        }
    }

    private fun prepareContentTypeList() {
        binding.progressBar.isVisible = false
        binding.contentTypesRecycler.isVisible = true
        orderedContentTypes.clear()
        orderedContentTypes.addAll(FeedContentType.entries)
        orderedContentTypes.sortBy { it.order }
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
            val atLeastOneSupported = WikipediaApp.instance.languageState.appLanguageCodes.any { supportedLanguages.contains(it) }
            if (!atLeastOneSupported) {
                i.remove()
            }
        }
        binding.contentTypesRecycler.adapter?.notifyItemRangeChanged(0, orderedContentTypes.size)
    }

    private fun setupRecyclerView() {
        binding.contentTypesRecycler.setHasFixedSize(true)
        val adapter = ConfigureItemAdapter()
        binding.contentTypesRecycler.adapter = adapter
        binding.contentTypesRecycler.layoutManager = LinearLayoutManager(activity)
        binding.contentTypesRecycler.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider))
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.contentTypesRecycler)
    }

    private fun onLoading() {
        binding.progressBar.isVisible = true
        binding.contentTypesRecycler.isVisible = false
    }

    private fun onError(throwable: Throwable) {
        L.e(throwable)
        prepareContentTypeList()
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
            view.contentDescription = getString(contentType.titleId) + ", " + getString(contentType.subtitleId)
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
        fun newInstance(): ConfigureFragment {
            return ConfigureFragment()
        }
    }
}
