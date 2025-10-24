package org.wikipedia.usercontrib

import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.settings.Prefs
import java.util.*
import kotlin.math.ceil

object UserContribStats {
    private const val REVERT_SEVERITY_PAUSE_THRESHOLD = 5
    private const val REVERT_SEVERITY_DISABLE_THRESHOLD = 7
    private const val PAUSE_DURATION_DAYS = 7

    private var totalEdits: Int = 0
    var totalReverts: Int = 0

    fun verifyEditCountsAndPauseState(totalContributionsList: List<UserContribution>) {
        totalEdits = totalContributionsList.size
        totalReverts = totalContributionsList.count { it.ns == 0 && it.tags.contains("mw-reverted") }
        maybePauseAndGetEndDate()
    }

    fun getRevertSeverity(): Int {
        return if (totalEdits <= 100) totalReverts else ceil(totalReverts.toFloat() / totalEdits.toFloat() * 100f).toInt()
    }

    fun isDisabled(): Boolean {
        return getRevertSeverity() > REVERT_SEVERITY_DISABLE_THRESHOLD
    }

    fun maybePauseAndGetEndDate(): Date? {
        val pauseDate = Prefs.suggestedEditsPauseDate
        var pauseEndDate: Date? = null

        // Are we currently in a pause period?
        if (pauseDate.time != 0L) {
            val cal = Calendar.getInstance()
            cal.time = pauseDate
            cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
            pauseEndDate = cal.time

            if (Date().after((pauseEndDate))) {
                // We've exceeded the pause period, so remove it.
                Prefs.suggestedEditsPauseDate = Date(0)
                pauseEndDate = null
            }
        }

        if (getRevertSeverity() > REVERT_SEVERITY_PAUSE_THRESHOLD) {
            // Do we need to impose a new pause?
            if (totalReverts > Prefs.suggestedEditsPauseReverts) {
                val cal = Calendar.getInstance()
                cal.time = Date()
                Prefs.suggestedEditsPauseDate = cal.time
                Prefs.suggestedEditsPauseReverts = totalReverts

                cal.add(Calendar.DAY_OF_YEAR, PAUSE_DURATION_DAYS)
                pauseEndDate = cal.time
            }
        }
        return pauseEndDate
    }
}
