package org.wikipedia.games.onthisday

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import org.wikipedia.R
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object ArchiveCalendarManager {
    fun showArchiveCalendar(
        fragment: Fragment,
        startDate: Date,
        endDate: Date,
        scoreData: Map<Long, Int>,
        onDateSelected: (Long) -> Unit
    ) {
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
            .setTitleText(fragment.getString(R.string.on_this_day_game_archive_calendar_title))
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
                   onDateSelected(selectedDateInMillis)
                }
            }

        datePicker.show(fragment.childFragmentManager, "datePicker")
    }

    fun maybeShowToastForDate(
        fragment: Fragment,
        selectedDateInMillis: Long,
        scoreData: Map<Long, Int>,) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = selectedDateInMillis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val scoreDataKey = DateDecorator.getDateKey(year, month + 1, day)
        if (scoreData[scoreDataKey] != null) {
            Toast.makeText(fragment.requireContext(),
                fragment.getString(R.string.on_this_day_game_score_toast_message,
                    scoreData[scoreDataKey],
                    OnThisDayGameViewModel.MAX_QUESTIONS), Toast.LENGTH_SHORT).show()
        }
    }
}
