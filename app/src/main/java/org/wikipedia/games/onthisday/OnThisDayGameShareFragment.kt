package org.wikipedia.games.onthisday

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayGameShareBinding
import org.wikipedia.databinding.ItemOnThisDayGameTopicBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import java.time.LocalDate

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

        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.shareContainer.post {
            binding.shareContainer.drawToBitmap(Bitmap.Config.RGB_565).run {
                ShareUtil.shareImage(lifecycleScope, requireContext(), this, "on_this_day_game_" + LocalDate.now(),
                    binding.resultText.text.toString(), binding.resultText.text.toString())
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildSharableContent(gameState: OnThisDayGameViewModel.GameState) {
        binding.shareContainer.visibility = View.VISIBLE
        val totalCorrect = gameState.answerState.count { it }
        binding.resultText.text = getString(R.string.on_this_day_game_share_title, totalCorrect, gameState.totalQuestions)
        createDots(gameState)
        binding.shareArticlesList.layoutManager = LinearLayoutManager(requireContext())
        binding.shareArticlesList.isNestedScrollingEnabled = false
        binding.shareArticlesList.adapter = RecyclerViewAdapter(gameState.articles.filterIndexed { index, _ -> index % 2 != 0 })
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
                if (gameState.answerState.getOrNull(i) == true) {
                    dotView.setImageResource(R.drawable.ic_check_black_24dp)
                    ResourceUtil.getThemedColorStateList(requireContext(), R.attr.success_color)
                } else {
                    dotView.setImageResource(R.drawable.ic_close_black_24dp)
                    ResourceUtil.getThemedColorStateList(requireContext(), R.attr.destructive_color)
                }
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
        RecyclerView.ViewHolder(binding.root) {

        fun bindItem(page: PageSummary) {
            binding.listItemTitle.text = StringUtil.fromHtml(page.displayTitle)
            binding.listItemDescription.text = StringUtil.fromHtml(page.description)
            binding.listItemDescription.isVisible = !page.description.isNullOrEmpty()
            page.thumbnailUrl?.let {
                ViewUtil.loadImage(binding.listItemThumbnail, it, roundedCorners = true)
            }
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
