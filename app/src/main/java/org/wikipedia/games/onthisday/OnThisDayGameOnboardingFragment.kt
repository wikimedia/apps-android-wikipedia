package org.wikipedia.games.onthisday

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialCalendar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.OnSelectionChangedListener
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
import org.wikipedia.util.log.L
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class OnThisDayGameOnboardingFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        @SuppressLint("RestrictedApi")
        override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
            if (fragment is MaterialDatePicker<*>) {
                val calendar = getPrivateCalendarFragment(fragment)
                L.d("calendar: $calendar")
                @Suppress("UNCHECKED_CAST")
                (calendar as MaterialCalendar<Long>?)?.addOnSelectionChangedListener(object : OnSelectionChangedListener<Long>() {
                    override fun onSelectionChanged(selection: Long) {
                        // TODO: handle selection!
                        L.d("selected: $selection")
                    }
                })
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameOnboardingBinding.inflate(inflater, container, false)
        childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        WikiGamesEvent.submit("impression", "game_play", slideName = "game_start")
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        childFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
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

    fun maybeShowArchiveCalendar(startDate: Date, endDate: Date, scoreData: Map<Long, Int>) {
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
                addOnPositiveButtonClickListener { selectedDateInMillis ->
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = selectedDateInMillis
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val scoreDataKey = DateDecorator.getDateKey(year, month, day)
                    val score = scoreData[scoreDataKey]
                    val total = OnThisDayGameViewModel.MAX_QUESTIONS
                    if (scoreData[scoreDataKey] != null) {
                        val formattedDate = DateUtil.getMMMMdYYYY(calendar.time)
                        // @TODO: replace this with string resource later
                        Toast.makeText(requireContext(), "You score $score/$total on $formattedDate", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show(childFragmentManager, "datePicker")
    }

    private fun getPrivateCalendarFragment(picker: MaterialDatePicker<*>): Any? {
        try {
            val field = picker.javaClass.getDeclaredField("calendar")
            field.isAccessible = true
            return field.get(picker)
        } catch (e: Exception) {
            L.e(e)
        }
        return null
    }
}
