package org.wikipedia.games.onthisday

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentOnThisDayGameShareBinding
import org.wikipedia.databinding.ItemOnThisDayGameTopicBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class OnThisDayGameShareFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameShareBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private val dotViews = mutableListOf<ImageView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildSharableContent(viewModel.getCurrentGameState())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildSharableContent(gameState: OnThisDayGameViewModel.GameState) {
        binding.shareContainer.visibility = View.VISIBLE
        createDots(gameState)
        binding.shareArticlesList.layoutManager = LinearLayoutManager(requireContext())
        binding.shareArticlesList.adapter = RecyclerViewAdapter(gameState.articles)
    }

    private fun createDots(gameState: OnThisDayGameViewModel.GameState) {
        val dotSize = DimenUtil.roundedDpToPx(20f)
        for (i in 0 until gameState.totalQuestions) {
            val viewId = View.generateViewId()
            val dotView = ImageView(requireContext())
            dotViews.add(dotView)
            dotView.layoutParams = ViewGroup.LayoutParams(dotSize, dotSize)
            dotView.setPadding(DimenUtil.roundedDpToPx(1f))
            dotView.setBackgroundResource(R.drawable.shape_circle)
            dotView.backgroundTintList =
                ResourceUtil.getThemedColorStateList(requireContext(), R.attr.inactive_color)
            dotView.imageTintList = ColorStateList.valueOf(Color.WHITE)
            dotView.id = viewId
            dotView.isVisible = true
            binding.shareContainer.addView(dotView)
        }
        binding.questionDotsFlow.referencedIds = dotViews.map { it.id }.toIntArray()
    }

    private inner class RecyclerViewAdapter(val pages: List<PageSummary>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return pages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemOnThisDayGameTopicBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(pages[position])
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemOnThisDayGameTopicBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private lateinit var page: PageSummary

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem(page: PageSummary) {
            this.page = page
            binding.listItemTitle.text = StringUtil.fromHtml(page.displayTitle)
            binding.listItemDescription.text = StringUtil.fromHtml(page.description)
            binding.listItemDescription.isVisible = !page.description.isNullOrEmpty()
            page.thumbnailUrl?.let {
                ViewUtil.loadImage(binding.listItemThumbnail, it, roundedCorners = true)
            }
        }

        override fun onClick(v: View) {
            val entry = HistoryEntry(page.getPageTitle(WikipediaApp.instance.wikiSite), HistoryEntry.SOURCE_PLACES)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameShareFragment {
            return OnThisDayGameShareFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
