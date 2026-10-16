package org.wikipedia.search.semantic

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import org.wikimedia.testkitchen.instrument.Funnel
import org.wikipedia.page.PageTitle

class SemanticSearchArticleFeedbackViewModel : ViewModel() {

    var semanticSearchFeedback: SemanticSearchFeedback? = null
        private set

    fun setup(title: PageTitle, searchId: String?, funnel: Funnel?) {
        semanticSearchFeedback = SemanticSearchFeedback(title, searchId, funnel)
    }

    fun startCountdown(): Long {
        val feedback = semanticSearchFeedback ?: return 0
        val now = SystemClock.elapsedRealtime()
        val dueAtMillis = feedback.dueAtMillis ?: (now + SemanticSearchFeedbackDialog.ARTICLE_DISPLAY_DELAY_MILLIS).also {
            semanticSearchFeedback = feedback.copy(dueAtMillis = it)
        }
        return (dueAtMillis - now).coerceAtLeast(0)
    }

    fun clear() {
        semanticSearchFeedback = null
    }

    data class SemanticSearchFeedback(
        val title: PageTitle,
        val searchId: String?,
        val funnel: Funnel?,
        val dueAtMillis: Long? = null
    )
}
