package org.wikipedia.games.onthisday

import androidx.fragment.app.Fragment
import java.time.LocalDate

abstract class OnThisDayGameBaseFragment : Fragment() {
    private var archiveCalendarHelper: ArchiveCalendarHelper? = null

    protected fun prepareAndOpenArchiveCalendar(viewModel: OnThisDayGameViewModel) {
        archiveCalendarHelper?.unRegister()
        archiveCalendarHelper = ArchiveCalendarHelper(
            fragment = this,
            wikiSite = viewModel.wikiSite,
            onDateSelected = { date -> onArchiveDateSelected(date) }
        ).also {
            it.register()
            it.show()
        }
    }

    override fun onDestroyView() {
        archiveCalendarHelper?.unRegister()
        super.onDestroyView()
    }

    abstract fun onArchiveDateSelected(date: LocalDate)
}
