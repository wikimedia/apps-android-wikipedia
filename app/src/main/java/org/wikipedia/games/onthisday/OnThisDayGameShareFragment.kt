package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentOnThisDayGameFinalBinding

class OnThisDayGameShareFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameFinalBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private lateinit var timeUpdateRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameFinalBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        binding.root.removeCallbacks(timeUpdateRunnable)
        super.onDestroyView()
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameShareFragment {
            return OnThisDayGameShareFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
