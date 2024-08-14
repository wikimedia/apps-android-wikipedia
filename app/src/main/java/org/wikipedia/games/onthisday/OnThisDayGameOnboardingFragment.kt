package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayGameOnboardingBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import java.time.LocalDate

class OnThisDayGameOnboardingFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameOnboardingBinding? = null
    private val binding get() = _binding!!

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
        }

        val today = DateUtil.getShortDateString(LocalDate.now())
        binding.messageText.text = getString(R.string.on_this_day_game_splash_subtitle, today, OnThisDayGameViewModel.gameForToday)
        binding.messageText2.text = getString(R.string.on_this_day_game_splash_message_2, Prefs.otdGameQuestionsPerDay)
        binding.footerMessage.text = getString(R.string.on_this_day_game_splash_footer_message, OnThisDayGameViewModel.daysLeft)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }


    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameOnboardingFragment {
            return OnThisDayGameOnboardingFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
