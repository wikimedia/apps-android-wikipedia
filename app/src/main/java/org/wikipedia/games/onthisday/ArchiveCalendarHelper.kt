package org.wikipedia.games.onthisday

import android.annotation.SuppressLint
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
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.games.WikiGames
import org.wikipedia.games.onthisday.OnThisDayGameViewModel.Companion.LANG_CODES_SUPPORTED
import org.wikipedia.games.onthisday.OnThisDayGameViewModel.Companion.dateReleasedForLang
import org.wikipedia.util.log.L
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class ArchiveCalendarHelper(
    private val fragment: Fragment,
    private val wikiSite: WikiSite,
    private val onDateSelected: (LocalDate) -> Unit
) {
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

    fun show() {
        fragment.lifecycleScope.launch {
            val startDateBasedOnLanguage = LANG_CODES_SUPPORTED.associateWith { dateReleasedForLang(it) }
            val localDate = startDateBasedOnLanguage[wikiSite.languageCode]
            val startDate = Date.from(localDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant())
            scoreData = getDataForArchiveCalendar(language = wikiSite.languageCode)
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

    fun register() {
        fragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
    }

    fun unRegister() {
        fragment.childFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
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
            .setTitleText(fragment.getString(R.string.on_this_day_game_archive_calendar_title))
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

        datePicker.show(fragment.childFragmentManager, "datePicker")
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
        onDateSelected(LocalDate.of(year, month, day))
    }

    private fun maybeShowToastForDate(selectedDateInMillis: Long, scoreData: Map<Long, Int>) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))
        calendar.timeInMillis = selectedDateInMillis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val scoreDataKey = DateDecorator.getDateKey(year, month + 1, day)
        if (scoreData[scoreDataKey] != null) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.getString(
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

    private suspend fun getDataForArchiveCalendar(gameName: Int = WikiGames.WHICH_CAME_FIRST.ordinal, language: String): Map<Long, Int> {
        val history = AppDatabase.instance.dailyGameHistoryDao().getGameHistory(gameName, language)
        val map = history.associate {
            val scoreKey = DateDecorator.getDateKey(it.year, it.month, it.day)
            scoreKey to it.score
        }
        return map
    }
}
