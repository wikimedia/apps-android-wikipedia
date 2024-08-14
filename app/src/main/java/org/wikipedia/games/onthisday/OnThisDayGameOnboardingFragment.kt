package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playGameButton.setOnClickListener {
            requireActivity().finish()
        }

        val today = DateUtil.getShortDateString(LocalDate.now())
        binding.messageText.text = getString(R.string.on_this_day_game_splash_subtitle, today, OnThisDayGameViewModel.gameForToday)
        binding.messageText2.text = getString(R.string.on_this_day_game_splash_message_2, Prefs.otdGameQuestionsPerDay)
        binding.footerMessage.text = getString(R.string.on_this_day_game_splash_footer_message, OnThisDayGameViewModel.daysLeft)
    }


    companion object {
        fun newInstance(): OnThisDayGameOnboardingFragment {
            return OnThisDayGameOnboardingFragment()
        }
    }
}
