package org.wikipedia.games.onthisday

import androidx.fragment.app.Fragment
import java.time.LocalDate

abstract class OnThisDayGameBaseFragment : Fragment() {
    private var onThisDayGameArchiveCalendarHelper: OnThisDayGameArchiveCalendarHelper? = null

    protected fun prepareAndOpenArchiveCalendar(languageCode: String) {
        onThisDayGameArchiveCalendarHelper?.unRegister()
        onThisDayGameArchiveCalendarHelper = OnThisDayGameArchiveCalendarHelper(
            fragment = this,
            languageCode = languageCode,
            onDateSelected = { date -> onArchiveDateSelected(date) }
        ).also {
            it.register()
            it.show()
        }
    }

    override fun onDestroyView() {
        onThisDayGameArchiveCalendarHelper?.unRegister()
        super.onDestroyView()
    }

    abstract fun onArchiveDateSelected(date: LocalDate)
}
