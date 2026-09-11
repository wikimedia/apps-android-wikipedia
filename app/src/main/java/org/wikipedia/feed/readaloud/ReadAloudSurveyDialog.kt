package org.wikipedia.feed.readaloud

import android.content.Context
import org.wikipedia.settings.Prefs
import java.time.LocalDate

object ReadAloudSurveyDialog {
    fun maybeShow(context: Context, byDate: Boolean) {
        if (Prefs.readAloudLeadSectionSurveyShown) {
            return
        }
        val lastPlayedDate = if (Prefs.readAloudLeadSectionLastPlayedDate.isEmpty()) null else runCatching { LocalDate.parse(Prefs.readAloudLeadSectionLastPlayedDate) }.getOrNull()
        if (byDate) {
            if (lastPlayedDate == null || lastPlayedDate.isAfter(LocalDate.now().minusDays(7))) {
                return
            }
        }

        // TODO: show the dialog.

        Prefs.readAloudLeadSectionSurveyShown = true
    }
}
