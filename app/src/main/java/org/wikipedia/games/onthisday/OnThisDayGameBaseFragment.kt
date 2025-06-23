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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

abstract class OnThisDayGameBaseFragment : Fragment() {
    private var scoreData: Map<Long, Int> = emptyMap()

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        @SuppressLint("RestrictedApi")
        override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
            if (fragment is MaterialDatePicker<*>) {
                val calendar = getPrivateCalendarFragment(fragment)
                @Suppress("UNCHECKED_CAST")
                (calendar as MaterialCalendar<Long>?)?.addOnSelectionChangedListener(object :
                    OnSelectionChangedListener<Long>() {
                    override fun onSelectionChanged(selection: Long) {
                        maybeShowToastForDate(selection, scoreData)
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
            val localDate = startDateBasedOnLanguage[viewModel.wikiSite.languageCode]
            val startDate = Date.from(localDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant())
            scoreData = viewModel.getDataForArchiveCalendar(language = viewModel.wikiSite.languageCode)
            showArchiveCalendar(
                startDate,
                Date(),
                scoreData,
                onDateSelected = { selectedDateInMillis ->
                    handleDateSelection(selectedDateInMillis)
                }
            )
        }
    }

    private fun showArchiveCalendar(startDate: Date, endDate: Date, scoreData: Map<Long, Int>, onDateSelected: (Long) -> Unit) {
        val startTimeInMillis = startDate.time
        val endTimeInMillis = endDate.time
        val calendarConstraints = CalendarConstraints.Builder()
            .setStart(startDate.time)
            .setEnd(endTimeInMillis)
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(startTimeInMillis - (24 * 60 * 60 * 1000)),
                        DateValidatorPointBackward.before(endTimeInMillis)
                    )
                )
            )
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.on_this_day_game_archive_calendar_title))
            .setTheme(R.style.MaterialDatePickerStyle)
            .setDayViewDecorator(
                DateDecorator(
                    startDate,
                    endDate,
                    scoreData
                )
            )
            .setCalendarConstraints(calendarConstraints)
            .setSelection(endTimeInMillis)
            .build()
            .apply {
                addOnPositiveButtonClickListener { selectedDateInMillis ->
                    onDateSelected(selectedDateInMillis)
                }
            }

        datePicker.show(childFragmentManager, "datePicker")
    }

    private fun handleDateSelection(selectedDateInMillis: Long) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))
        calendar.timeInMillis = selectedDateInMillis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val scoreDataKey = DateDecorator.getDateKey(year, month, day)
        if (scoreData[scoreDataKey] != null) {
            return
        }
        WikiGamesEvent.submit("date_select", "game_play", slideName = "archive_calendar")
        onArchiveDateSelected(LocalDate.of(year, month, day))
    }

    abstract fun onArchiveDateSelected(date: LocalDate)

    private fun maybeShowToastForDate(selectedDateInMillis: Long, scoreData: Map<Long, Int>) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))
        calendar.timeInMillis = selectedDateInMillis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val scoreDataKey = DateDecorator.getDateKey(year, month + 1, day)
        if (scoreData[scoreDataKey] != null) {
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.on_this_day_game_score_toast_message,
                    scoreData[scoreDataKey],
                    OnThisDayGameViewModel.MAX_QUESTIONS
                ), Toast.LENGTH_SHORT
            ).show()
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
