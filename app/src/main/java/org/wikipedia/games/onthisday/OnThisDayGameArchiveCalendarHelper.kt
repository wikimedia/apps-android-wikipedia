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
import org.wikipedia.games.WikiGames
import org.wikipedia.games.onthisday.OnThisDayGameViewModel.Companion.dateReleasedForLang
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class OnThisDayGameArchiveCalendarHelper(
    private val fragment: Fragment,
    private var languageCode: String,
    private val onDateSelected: (LocalDate) -> Unit
) {
    private var scoreData: Map<LocalDate, Int> = emptyMap()

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
            val startDateBasedOnLanguage = WikiGames.WHICH_CAME_FIRST.supportLanguages.associateWith { dateReleasedForLang(it) }
            val localDate = startDateBasedOnLanguage[languageCode]!!
            scoreData = getDataForArchiveCalendar(language = languageCode)
            showArchiveCalendar(
                localDate,
                LocalDate.now(),
                scoreData,
                onDateSelected = ::handleDateSelection
            )
        }
    }

    fun updateLanguageCode(languageCode: String) {
        this.languageCode = languageCode
    }

    fun register() {
        fragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
    }

    fun unRegister() {
        fragment.childFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }

    private fun showArchiveCalendar(startDate: LocalDate, endDate: LocalDate, scoreData: Map<LocalDate, Int>, onDateSelected: (Long) -> Unit) {
        val zoneId = ZoneId.systemDefault()
        val startInstant = startDate.atStartOfDay(zoneId).toInstant()
        val endTime = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val calendarConstraints = CalendarConstraints.Builder()
            .setStart(startInstant.toEpochMilli())
            .setEnd(endTime)
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(
                            startInstant.minus(1, ChronoUnit.DAYS).toEpochMilli()
                        ),
                        DateValidatorPointBackward.before(endTime)
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
            .setSelection(endTime)
            .build()
            .apply {
                addOnPositiveButtonClickListener(onDateSelected)
            }

        datePicker.show(fragment.childFragmentManager, "datePicker")
    }

    private fun handleDateSelection(selectedDateInMillis: Long) {
        val selectedDate = LocalDate.ofInstant(Instant.ofEpochMilli(selectedDateInMillis), ZoneId.systemDefault())
        if (scoreData[selectedDate] != null) {
            return
        }
        WikiGamesEvent.submit("date_select", "game_play", slideName = "archive_calendar")
        onDateSelected(selectedDate)
    }

    private fun maybeShowToastForDate(selectedDateInMillis: Long, scoreData: Map<LocalDate, Int>) {
        val selectedDate = LocalDate.ofInstant(Instant.ofEpochMilli(selectedDateInMillis), ZoneId.systemDefault())
        if (scoreData[selectedDate] != null) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.getString(
                    R.string.on_this_day_game_score_toast_message,
                    scoreData[selectedDate],
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

    private suspend fun getDataForArchiveCalendar(gameName: Int = WikiGames.WHICH_CAME_FIRST.ordinal, language: String): Map<LocalDate, Int> {
        val history = AppDatabase.instance.dailyGameHistoryDao().getGameHistory(gameName, language)
        return history.associate { it.date to it.score }
    }
}
