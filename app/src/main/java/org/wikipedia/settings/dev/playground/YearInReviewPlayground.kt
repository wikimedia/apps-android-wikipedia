package org.wikipedia.settings.dev.playground

import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewSnapshot

enum class YearInReviewPlaygroundData(val label: String, val readingStats: YearInReviewReadingStats?) {
    REAL("Real data", null),
    LOW_DATA("Low data", YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0)),
    DATA_RICH("Data rich", YearInReviewReadingStats(articlesReadCount = 120, visitedDaysCount = 45))
}

object YearInReviewPlayground {
    // Only replaces the returned snapshot, so the real reading history and cached stats are left untouched.
    fun applyTo(snapshot: YearInReviewSnapshot): YearInReviewSnapshot {
        if (!ReleaseUtil.isPreProdRelease) {
            return snapshot
        }
        val readingStats = Prefs.yearInReviewPlaygroundData.readingStats ?: return snapshot
        return snapshot.copy(readingStats = readingStats)
    }
}
