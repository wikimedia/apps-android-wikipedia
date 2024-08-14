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
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayGameShareBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil

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
        viewModel.gameState.observe(requireActivity()) {
            when (it) {
                is Resource.Success -> onSuccess(it.data)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onSuccess(gameState: OnThisDayGameViewModel.GameState) {
        binding.shareContainer.visibility = View.VISIBLE
        createDots(gameState)
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

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameShareFragment {
            return OnThisDayGameShareFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
