package org.wikipedia.games.onthisday

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.FragmentOnThisDayGameOnboardingBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import java.util.Calendar

class OnThisDayGameOnboardingFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameOnboardingBinding.inflate(inflater, container, false)

        WikiGamesEvent.submit("impression", "game_play", slideName = "game_start")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playGameButton.setOnClickListener {
            WikiGamesEvent.submit("play_click", "game_play", slideName = "game_start")
            requireActivity().supportFragmentManager.popBackStack()
            (requireActivity() as? OnThisDayGameActivity)?.animateQuestionsIn()
        }

        binding.dateText.text = DateUtil.getShortDateString(viewModel.currentDate)
    }

    companion object {
        private const val SHOW_ON_EXPLORE_FEED_COUNT = 2

        fun newInstance(invokeSource: InvokeSource): OnThisDayGameOnboardingFragment {
            return OnThisDayGameOnboardingFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun maybeShowOnThisDayGameDialog(activity: Activity, invokeSource: InvokeSource, articleWikiSite: WikiSite = WikipediaApp.instance.wikiSite) {
            val wikiSite = WikipediaApp.instance.wikiSite
            // Both of the primary language and the article language should be in the supported languages list.
            if (!Prefs.otdEntryDialogShown &&
                OnThisDayGameViewModel.LANG_CODES_SUPPORTED.contains(wikiSite.languageCode) &&
                OnThisDayGameViewModel.LANG_CODES_SUPPORTED.contains(articleWikiSite.languageCode) &&
                (invokeSource != InvokeSource.FEED || Prefs.exploreFeedVisitCount >= SHOW_ON_EXPLORE_FEED_COUNT)) {
                Prefs.otdEntryDialogShown = true
                WikiGamesEvent.submit("impression", "game_modal")
                val dialogView = activity.layoutInflater.inflate(R.layout.dialog_on_this_day_game, null)
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setView(dialogView)
                    .setCancelable(false)
                    .show()
                dialogView.findViewById<Button>(R.id.playGameButton).setOnClickListener {
                    WikiGamesEvent.submit("enter_click", "game_modal")
                    activity.startActivityForResult(OnThisDayGameActivity.newIntent(activity, invokeSource, wikiSite), 0)
                    dialog.dismiss()
                }
                dialogView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
                    FeedbackUtil.showMessage(activity, R.string.on_this_day_game_entry_dialog_snackbar_message)
                    dialog.dismiss()
                }
            }
        }
    }

    // helper function to create a standard key for calendar views
    private fun getScoreDatKey(year: Int, month: Int, day: Int): String {
        return "$year-$month-$day"
    }

    fun maybeShowArchiveCalendar(startDate: Calendar, endDate: Calendar, scoreData: Map<String, Int>) {
        val endTimeInMillis = System.currentTimeMillis()
        val calendarConstraints = CalendarConstraints.Builder()
            .setEnd(endTimeInMillis)
            .setValidator(DateValidatorPointBackward.before(endTimeInMillis))
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Choose a game by date to play.") // @TODO: replace this with string resource later
            .setTheme(R.style.MaterialDatePickerStyle)
            .setDayViewDecorator(DateDecorator(
                startDate,
                endDate,
                scoreData))
            .setCalendarConstraints(calendarConstraints)
            .setSelection(endTimeInMillis)
            .build()
            .apply {
                addOnPositiveButtonClickListener {}
            }
            .show(requireActivity().supportFragmentManager, "datePicker")
    }
}
