package org.wikipedia.games.onthisday

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialCalendar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.OnSelectionChangedListener
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.games.onthisday.OnThisDayGameViewModel.Companion.LANG_CODES_SUPPORTED
import org.wikipedia.games.onthisday.OnThisDayGameViewModel.Companion.dateReleasedForLang
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

abstract class OnThisDayGameBaseFragment : Fragment() {
    private var scoreData = emptyMap<LocalDate, Int>()

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        @SuppressLint("RestrictedApi")
        override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
            if (fragment is MaterialDatePicker<*>) {
                val calendar = getPrivateCalendarFragment(fragment)
                @Suppress("UNCHECKED_CAST")
                (calendar as MaterialCalendar<Long>?)?.addOnSelectionChangedListener(object :
                    OnSelectionChangedListener<Long>() {
                    override fun onSelectionChanged(selection: Long) {
                        maybeShowToastForDate(selection)
                    }
                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
    }

    override fun onDestroyView() {
        childFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroyView()
    }

    protected fun prepareAndOpenArchiveCalendar(viewModel: OnThisDayGameViewModel) {
        lifecycleScope.launch {
            val startDateBasedOnLanguage = LANG_CODES_SUPPORTED.associateWith { dateReleasedForLang(it) }
            val startDate = startDateBasedOnLanguage[viewModel.wikiSite.languageCode]!!
            scoreData = viewModel.getDataForArchiveCalendar(language = viewModel.wikiSite.languageCode)
            showArchiveCalendar(startDate, onDateSelected = ::handleDateSelection)
        }
    }

    private fun showArchiveCalendar(startDate: LocalDate, onDateSelected: (Long) -> Unit) {
        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val startTimeInMillis = startInstant.toEpochMilli()
        val endInstant = Instant.now()
        val endTimeInMillis = endInstant.toEpochMilli()
        val oneDayBeforeStart = startInstant.minus(1, ChronoUnit.DAYS).toEpochMilli()
        val calendarConstraints = CalendarConstraints.Builder()
            .setStart(startTimeInMillis)
            .setEnd(endTimeInMillis)
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(oneDayBeforeStart),
                        DateValidatorPointBackward.before(endTimeInMillis)
                    )
                )
            )
            .build()

        val endDate = LocalDate.ofInstant(endInstant, ZoneId.systemDefault())
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.on_this_day_game_archive_calendar_title))
            .setTheme(R.style.MaterialDatePickerStyle)
            .setDayViewDecorator(DateDecorator(startDate, endDate, scoreData))
            .setCalendarConstraints(calendarConstraints)
            .setSelection(endTimeInMillis)
            .build()
            .apply {
                addOnPositiveButtonClickListener(onDateSelected)
            }

        datePicker.show(childFragmentManager, "datePicker")
    }

    private fun handleDateSelection(selectedDateInMillis: Long) {
        val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(selectedDateInMillis),
            ZoneId.systemDefault())
        if (scoreData[localDate] != null) {
            return
        }
        WikiGamesEvent.submit("date_select", "game_play", slideName = "archive_calendar")
        onArchiveDateSelected(localDate)
    }

    abstract fun onArchiveDateSelected(date: LocalDate)

    private fun maybeShowToastForDate(selectedDateInMillis: Long) {
        val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(selectedDateInMillis),
            ZoneId.systemDefault())
        val score = scoreData[localDate]
        if (score != null) {
            val message = getString(R.string.on_this_day_game_score_toast_message, score,
                OnThisDayGameViewModel.MAX_QUESTIONS)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
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
