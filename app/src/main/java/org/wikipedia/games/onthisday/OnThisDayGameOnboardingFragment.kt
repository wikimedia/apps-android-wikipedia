package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentOnThisDayGameOnboardingBinding
import org.wikipedia.util.DateUtil

class OnThisDayGameOnboardingFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameOnboardingBinding.inflate(inflater, container, false)

        // TODO: add analytics for InvokeSource

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // If we really want it full-screen:
        // requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.playGameButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            (requireActivity() as? OnThisDayGameActivity)?.animateQuestions()
        }

        binding.dateText.text = DateUtil.getShortDateString(viewModel.currentDate)
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameOnboardingFragment {
            return OnThisDayGameOnboardingFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
